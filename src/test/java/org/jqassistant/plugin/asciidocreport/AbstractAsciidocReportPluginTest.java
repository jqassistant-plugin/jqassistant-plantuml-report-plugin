package org.jqassistant.plugin.asciidocreport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.report.api.model.Result.Status;
import com.buschmais.jqassistant.core.report.impl.ReportContextImpl;
import com.buschmais.jqassistant.core.rule.api.configuration.Rule;
import com.buschmais.jqassistant.core.rule.api.model.*;
import com.buschmais.jqassistant.core.rule.api.source.FileRuleSource;
import com.buschmais.jqassistant.core.rule.api.source.RuleSource;
import com.buschmais.jqassistant.core.rule.impl.reader.AsciidocRuleParserPlugin;
import com.buschmais.jqassistant.core.rule.impl.reader.RuleParser;
import com.buschmais.jqassistant.core.shared.io.ClasspathResource;
import org.jqassistant.plugin.asciidocreport.plantuml.component.ComponentDiagramReportPlugin;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
abstract class AbstractAsciidocReportPluginTest {

    @Mock
    private Rule rule;

    protected Map<String, ReportPlugin> reportPlugins;

    protected File ruleDirectory;

    protected File outputDirectory = new File("target/");

    protected RuleSet ruleSet;

    @BeforeEach
    final void setUp() throws RuleException {
        File classesDirectory = ClasspathResource.getFile(AbstractAsciidocReportPluginTest.class, "/");
        ruleDirectory = new File(classesDirectory, "working directory/jqassistant");
        List<String> asciidocFiles = getAsciidocFiles();
        ruleSet = getRuleSet(ruleDirectory, asciidocFiles);
        reportPlugins = new HashMap<>();
        reportPlugins.put("asciidoc", new AsciidocReportPlugin());
        reportPlugins.put("plantuml-component-diagram", new ComponentDiagramReportPlugin());
        for (ReportPlugin reportPlugin : reportPlugins.values()) {
            reportPlugin.initialize();
        }
    }

    /**
     * Return the list of Asciidoc file names to be used in the test.
     *
     * @return The Asciidoc file names.
     */
    protected abstract List<String> getAsciidocFiles();

    protected final ReportContext configureReportContext(Map<String, Object> properties) throws ReportException {
        ReportContext reportContext = new ReportContextImpl(AbstractAsciidocReportPluginTest.class.getClassLoader(), null, outputDirectory);
        for (ReportPlugin reportPlugin : reportPlugins.values()) {
            reportPlugin.configure(reportContext, properties);
        }
        return reportContext;
    }

    protected final void verifyRule(Document document, String id, String expectedDescription, Status expectedStatus, String expectedTitle) {
        Element rule = document.getElementById(id);
        Element title = rule.getElementsByClass("title").first();
        assertThat(title).isNotNull();
        assertThat(title.text()).isEqualTo(expectedDescription);

        Element status = rule.getElementsByClass("jqassistant-rule-status").first();
        assertThat(status).isNotNull();
        assertThat(status.hasClass("fa")).isEqualTo(true);
        switch (expectedStatus) {
        case SUCCESS:
            assertThat(status.hasClass("fa-check")).isEqualTo(true);
            assertThat(status.hasClass("jqassistant-status-success")).isEqualTo(true);
            break;
        case WARNING:
            assertThat(status.hasClass("fa-exclamation")).isEqualTo(true);
            assertThat(status.hasClass("jqassistant-status-warning")).isEqualTo(true);
            break;
        case FAILURE:
            assertThat(status.hasClass("fa-ban")).isEqualTo(true);
            assertThat(status.hasClass("jqassistant-status-failure")).isEqualTo(true);
            break;
        }
        assertThat(status.attr("title")).isEqualTo("Id: " + id + ", " + expectedTitle);

        Element ruleToggle = rule.getElementsByClass("jqassistant-rule-toggle").first();
        assertThat(ruleToggle).isNotNull();
        Element content = rule.getElementsByClass("content").first();
        assertThat(content).isNotNull();
    }

    protected final void verifyRuleResult(Document document, String id, String expectedColumnName, String... expectedValues) {
        Element ruleResult = document.getElementById("result(" + id + ")");
        Element thead = ruleResult.getElementsByTag("thead").first();
        assertThat(thead).isNotNull();
        Element th = thead.getElementsByTag("th").first();
        assertThat(th).isNotNull();
        assertThat(th.text()).isEqualTo(expectedColumnName);
        Element tbody = ruleResult.getElementsByTag("tbody").first();
        assertThat(tbody).isNotNull();
        Elements tds = tbody.getElementsByTag("td");
        List<String> values = tds.stream().map(td -> td.text()).collect(toList());
        assertThat(values).containsExactly(expectedValues);
    }

    private RuleSet getRuleSet(File ruleDirectory, Iterable<String> adocFiles) throws RuleException {
        AsciidocRuleParserPlugin ruleParserPlugin = new AsciidocRuleParserPlugin();
        ruleParserPlugin.initialize();
        ruleParserPlugin.configure(rule);
        RuleParser ruleParser = new RuleParser(asList(ruleParserPlugin));
        List<RuleSource> ruleSources = new ArrayList<>();
        for (String adocFile : adocFiles) {
            ruleSources.add(new FileRuleSource(ruleDirectory, adocFile));
        }
        return ruleParser.parse(ruleSources);
    }

    protected final void processConcept(ReportPlugin plugin, String id, Status status, Severity severity, List<String> columnNames,
            List<Map<String, Object>> rows) throws RuleException {
        Concept includedConcept = ruleSet.getConceptBucket().getById(id);
        processConcept(plugin, includedConcept, new Result<>(includedConcept, status, severity, columnNames, rows));
    }

    protected final void processConcept(ReportPlugin plugin, Concept rule, Result<Concept> result) throws ReportException {
        plugin.beginConcept(rule);
        plugin.setResult(result);
        plugin.endConcept();
    }

    protected final void processConstraint(ReportPlugin plugin, String id, Status status, Severity severity, List<String> columnNames,
            List<Map<String, Object>> rows) throws RuleException {
        Constraint constraint = ruleSet.getConstraintBucket().getById(id);
        processConstraint(plugin, constraint, new Result<>(constraint, status, severity, columnNames, rows));
    }

    protected final void processConstraint(ReportPlugin plugin, Constraint rule, Result<Constraint> result) throws ReportException {
        plugin.beginConstraint(rule);
        plugin.setResult(result);
        plugin.endConstraint();
    }
}
