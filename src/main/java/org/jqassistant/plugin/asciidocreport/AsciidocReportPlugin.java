package org.jqassistant.plugin.asciidocreport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportHelper;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.ReportPlugin.Default;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.*;
import com.buschmais.jqassistant.core.rule.api.source.RuleSource;
import com.buschmais.jqassistant.core.shared.asciidoc.AsciidoctorFactory;
import com.buschmais.jqassistant.core.shared.asciidoc.DocumentParser;

import org.apache.commons.io.IOUtils;
import org.asciidoctor.*;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.SKIPPED;
import static java.nio.charset.StandardCharsets.UTF_8;

@Default
public class AsciidocReportPlugin implements ReportPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsciidocReportPlugin.class);

    private static final String PROPERTY_DIRECTORY = "asciidoc.report.directory";
    private static final String PROPERTY_RULE_DIRECTORY = "asciidoc.report.rule.directory";
    private static final String PROPERTY_FILE_INCLUDE = "asciidoc.report.file.include";
    private static final String PROPERTY_FILE_EXCLUDE = "asciidoc.report.file.exclude";

    private static final String DEFAULT_REPORT_DIRECTORY = "asciidoc";

    private static final String BACKEND_HTML5 = "html5";
    private static final String CODERAY = "coderay";

    private final DocumentParser documentParser = new DocumentParser();

    private ReportContext reportContext;

    private File reportDirectory;

    private RuleSourceMatcher ruleSourceMatcher;

    private Set<RuleSource> ruleSources;

    private Map<String, RuleResult> conceptResults;
    private Map<String, RuleResult> constraintResults;

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {
        this.reportContext = reportContext;
        File defaultReportDirectory = reportContext.getReportDirectory(DEFAULT_REPORT_DIRECTORY);
        this.reportDirectory = getFile(PROPERTY_DIRECTORY, defaultReportDirectory, properties).getAbsoluteFile();
        if (this.reportDirectory.mkdirs()) {
            LOGGER.info("Created directory '" + this.reportDirectory.getAbsolutePath() + "'.");
        }
        File ruleDirectory = getFile(PROPERTY_RULE_DIRECTORY, null, properties);
        String fileInclude = (String) properties.get(PROPERTY_FILE_INCLUDE);
        String fileExclude = (String) properties.get(PROPERTY_FILE_EXCLUDE);
        this.ruleSourceMatcher = new RuleSourceMatcher(ruleDirectory, fileInclude, fileExclude);
    }

    private File getFile(String property, File defaultValue, Map<String, Object> properties) {
        String directoryName = (String) properties.get(property);
        return directoryName != null ? new File(directoryName) : defaultValue;
    }

    @Override
    public void begin() {
        ruleSources = new HashSet<>();
        conceptResults = new HashMap<>();
        constraintResults = new HashMap<>();
    }

    @Override
    public void end() throws ReportException {
        List<RuleSource> filteredRuleSources = ruleSourceMatcher.match(ruleSources);
        if (!filteredRuleSources.isEmpty()) {
            LOGGER.info("Calling for the Asciidoctor...");
            Asciidoctor asciidoctor = AsciidoctorFactory.getAsciidoctor();
            LOGGER.info("Writing to report directory " + reportDirectory.getAbsolutePath());
            for (RuleSource ruleSource : filteredRuleSources) {
                OptionsBuilder optionsBuilder = Options.builder().mkDirs(true).toDir(reportDirectory).backend(BACKEND_HTML5).safe(SafeMode.UNSAFE)
                        .attributes(Attributes.builder().experimental(true).sourceHighlighter(CODERAY).icons("font").build());
                ruleSource.getDirectory().ifPresent(baseDir -> optionsBuilder.baseDir(baseDir));
                String outputFileName = getOutputFileName(ruleSource);
                optionsBuilder.toFile(new File(outputFileName));
                Options options = optionsBuilder.build();
                LOGGER.info("-> {}", ruleSource);
                String content = readContent(ruleSource);
                Document document = asciidoctor.load(content, options);
                JavaExtensionRegistry extensionRegistry = asciidoctor.javaExtensionRegistry();
                IncludeProcessor includeProcessor = new IncludeProcessor(documentParser, document, conceptResults, constraintResults);
                extensionRegistry.includeProcessor(includeProcessor);
                extensionRegistry.includeProcessor(new PluginIncludeProcessor(reportContext.getClassLoader(), ruleSource.getRelativePath()));
                extensionRegistry.inlineMacro(new InlineMacroProcessor(documentParser));
                extensionRegistry.treeprocessor(new TreePreprocessor(documentParser, conceptResults, constraintResults,
                        new File(reportDirectory, outputFileName).getParentFile(), reportContext));
                extensionRegistry.postprocessor(new RulePostProcessor(conceptResults, constraintResults));
                asciidoctor.convert(content, options);
                asciidoctor.unregisterAllExtensions();
            }
            LOGGER.info("The Asciidoctor finished his work successfully.");
        }

    }

    private String readContent(RuleSource ruleSource) throws ReportException {
        String content;
        try (InputStream inputStream = ruleSource.getInputStream()) {
            content = IOUtils.toString(inputStream, UTF_8);
        } catch (IOException e) {
            throw new ReportException("Cannot read rule source " + ruleSource);
        }
        return content;
    }

    /**
     * Create the output file name by replacing the relative path of the given
     * {@link RuleSource} extension with ".html".
     *
     * @param ruleSource
     *            The {@link RuleSource}.
     * @return The output file name.
     */
    private String getOutputFileName(RuleSource ruleSource) {
        String relativePath = ruleSource.getRelativePath();
        return relativePath.substring(0, relativePath.lastIndexOf('.')) + ".html";
    }

    @Override
    public void beginGroup(Group group) {
        addRuleSource(group);
    }

    @Override
    public void beginConcept(Concept concept) {
        addRuleSource(concept);
    }

    @Override
    public void beginConstraint(Constraint constraint) {
        addRuleSource(constraint);
    }

    private void addRuleSource(Rule rule) {
        ruleSources.add(rule.getSource());
    }

    @Override
    public void setResult(Result<? extends ExecutableRule> result) {
        // Collect the results for executed concepts and constraints
        ExecutableRule rule = result.getRule();
        if (rule instanceof Concept) {
            this.conceptResults.put(rule.getId(), getRuleResult(result));
        } else if (rule instanceof Constraint) {
            this.constraintResults.put(rule.getId(), getRuleResult(result));
        }
    }

    private RuleResult getRuleResult(Result<? extends ExecutableRule> result) {
        RuleResult.RuleResultBuilder ruleResultBuilder = RuleResult.builder();
        List<String> columnNames = result.getColumnNames();
        ruleResultBuilder.rule(result.getRule()).effectiveSeverity(result.getSeverity()).status(result.getStatus()).columnNames(columnNames);
        if (!SKIPPED.equals(result.getStatus())) {
            addResultRows(result, ruleResultBuilder);
        }
        return ruleResultBuilder.build();
    }

    private void addResultRows(Result<? extends ExecutableRule> result, RuleResult.RuleResultBuilder ruleResultBuilder) {
        for (Map<String, Object> row : result.getRows()) {
            Map<String, List<String>> resultRow = new LinkedHashMap<>();
            for (Map.Entry<String, Object> rowEntry : row.entrySet()) {
                Object value = rowEntry.getValue();
                List<String> values = new ArrayList<>();
                if (value instanceof Iterable<?>) {
                    for (Object o : ((Iterable) value)) {
                        values.add(ReportHelper.getLabel(o));
                    }
                } else {
                    values.add(ReportHelper.getLabel(value));
                }
                resultRow.put(rowEntry.getKey(), values);
            }
            ruleResultBuilder.row(resultRow);
        }
    }
}
