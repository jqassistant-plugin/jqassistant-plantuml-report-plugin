package org.jqassistant.plugin.asciidocreport;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.core.rule.api.source.FileRuleSource;
import com.buschmais.jqassistant.core.rule.api.source.RuleSource;
import com.buschmais.jqassistant.core.shared.io.ClasspathResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class RuleSourceMatcherTest {

    private File ruleDirectory;

    @BeforeEach
    public void setUp() {
        File classesDirectory = ClasspathResource.getFile(RuleSourceMatcherTest.class, "/");
        ruleDirectory = new File(classesDirectory, "working directory/jqassistant");
    }

    /**
     * Verifies that a given rule directory is scanned for adoc files.
     */
    @Test
    void scanRuleDirectory() throws ReportException {
        RuleSourceMatcher ruleSourceMatcher = new RuleSourceMatcher(ruleDirectory, "index.adoc", null);

        List<RuleSource> filteredRuleSources = ruleSourceMatcher.match(emptySet());

        assertThat(filteredRuleSources).hasSize(1);
        RuleSource ruleSource = filteredRuleSources.get(0);
        assertThat(ruleSource.getRelativePath()).isEqualTo("index.adoc");
    }

    /**
     * Verifies that the provided set of {@link RuleSource}s is used to detect the
     * "index.adoc" file.
     */
    @Test
    void detectIndexFileFromRuleSources() throws RuleException {
        RuleSourceMatcher ruleSourceMatcher = new RuleSourceMatcher(null, null, null);
        Set<RuleSource> ruleSources = new HashSet<>();
        ruleSources.add(new FileRuleSource(ruleDirectory, "index.adoc"));
        ruleSources.add(new FileRuleSource(ruleDirectory, "additional-rules/importedRules.adoc"));

        List<RuleSource> filteredRuleSources = ruleSourceMatcher.match(ruleSources);

        assertThat(filteredRuleSources).hasSize(1);
        RuleSource ruleSource = filteredRuleSources.get(0);
        assertThat(ruleSource.getRelativePath()).isEqualTo("index.adoc");
    }

    /**
     * Verifies that the provided set of {@link RuleSource}s is used to detect the
     * "index.adoc" file.
     */
    @Test
    void detectIndexFileFromRuleSourcesWithFilter() throws RuleException {
        RuleSourceMatcher ruleSourceMatcher = new RuleSourceMatcher(null, "notmatching", null);
        Set<RuleSource> ruleSources = new HashSet<>();
        ruleSources.add(new FileRuleSource(ruleDirectory, "index.adoc"));

        List<RuleSource> filteredRuleSources = ruleSourceMatcher.match(ruleSources);

        assertThat(filteredRuleSources).isEmpty();
    }
}
