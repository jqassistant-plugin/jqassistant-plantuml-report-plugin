package org.jqassistant.plugin.asciidocreport;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.rule.api.source.FileRuleSource;
import com.buschmais.jqassistant.core.rule.api.source.RuleSource;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FilePatternMatcher;

import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;

@Slf4j
public class RuleSourceMatcher {

    private static final String DEFAULT_INCLUDE = "index.adoc,**/index.adoc";

    private final File ruleDirectory;

    private final FilePatternMatcher filePatternMatcher;

    RuleSourceMatcher(File ruleDirectory, String fileInclude, String fileExclude) {
        this.ruleDirectory = ruleDirectory;
        FilePatternMatcher.Builder builder = FilePatternMatcher.builder();
        if (fileInclude == null && fileExclude == null) {
            builder.include(DEFAULT_INCLUDE);
        } else {
            builder.include(fileInclude).exclude(fileExclude);
        }
        this.filePatternMatcher = builder.build();
    }

    /**
     * Determine the ADOC files.
     *
     * @param ruleSources
     *     The {@link RuleSource}s.
     * @return The {@link RuleSource}
     * @throws ReportException
     *     If execution fails.
     */
    public List<RuleSource> match(Set<RuleSource> ruleSources) throws ReportException {
        if (ruleDirectory != null) {
            try {
                return FileRuleSource.getRuleSources(ruleDirectory).stream().filter(ruleSource -> filePatternMatcher.accepts(ruleSource.getRelativePath()))
                    .collect(toList());
            } catch (IOException e) {
                throw new ReportException("Cannot read rules from directory " + ruleDirectory, e);
            }
        } else {
            return ruleSources.stream().filter(ruleSource -> filePatternMatcher.accepts(ruleSource.getRelativePath())).collect(toList());
        }
    }

}
