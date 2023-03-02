package org.jqassistant.plugin.asciidocreport;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.Concept;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import com.buschmais.jqassistant.core.rule.api.model.Severity;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RulePostProcessorTest {

    private String html;

    @BeforeEach
    void readHtml() throws IOException {
        this.html = IOUtils.toString(RulePostProcessorTest.class.getResource("/rulepostprocessor.html"), UTF_8);
    }

    @Test
    void listingConceptWithTitle() {
        Concept concept = Concept.builder()
            .id("listingConceptWithTitle")
            .severity(Severity.MINOR)
            .build();
        verify(concept, createRuleResults(concept), emptyMap(), ruleToggle -> assertThat(ruleToggle).isNotNull());
    }

    @Test
    void listingConceptWithoutTitle() {
        // Given
        Concept concept = Concept.builder()
            .id("listingConceptWithoutTitle")
            .severity(Severity.MINOR)
            .build();
        verify(concept, createRuleResults(concept), emptyMap(), ruleToggle -> assertThat(ruleToggle).isNotNull());
    }

    @Test
    void nonListingConceptWithoutTitle() {
        Concept concept = Concept.builder()
            .id("nonListingConceptWithTitle")
            .severity(Severity.MINOR)
            .build();
        verify(concept, createRuleResults(concept), emptyMap(), ruleToggle -> assertThat(ruleToggle).isEmpty());
    }

    @Test
    void listingConstraintWithTitle() {
        Constraint constraint = Constraint.builder()
            .id("listingConstraintWithTitle")
            .severity(Severity.MINOR)
            .build();
        verify(constraint, emptyMap(), createRuleResults(constraint), ruleToggle -> assertThat(ruleToggle).isNotNull());
    }

    private void verify(ExecutableRule<?> rule, Map<String, RuleResult> conceptResults, Map<String, RuleResult> constraintResults,
        Consumer<Elements> ruleToggleConsumer) {

        // When
        String result = RulePostProcessor.HtmlRulePostProcessor.process(conceptResults, constraintResults, html);

        // Then
        org.jsoup.nodes.Document doc = Jsoup.parse(result, "UTF-8");
        Element ruleBlock = doc.getElementById(rule.getId());
        verifyStatus(ruleBlock, rule.getId());
        Elements ruleToggle = ruleBlock.getElementsByClass("jqassistant-rule-toggle");
        ruleToggleConsumer.accept(ruleToggle);
    }

    private Map<String, RuleResult> createRuleResults(ExecutableRule<?>... rules) {
        Map<String, RuleResult> ruleResults = new HashMap<>();
        for (ExecutableRule<?> rule : rules) {
            ruleResults.put(rule.getId(), RuleResult.builder()
                .rule(rule)
                .effectiveSeverity(Severity.MINOR)
                .status(Result.Status.SUCCESS)
                .build());
        }
        return ruleResults;
    }

    private void verifyStatus(Element ruleBlock, String expectedId) {
        Element status = ruleBlock.getElementsByClass("jqassistant-rule-status")
            .first();
        assertThat(status).isNotNull();
        assertThat(status.hasClass("jqassistant-status-success")).isTrue();
        assertThat(status.hasClass("fa")).isTrue();
        assertThat(status.hasClass("fa-check")).isTrue();
        assertThat(status.attr("title")).contains("Id: " + expectedId)
            .contains("Status: SUCCESS")
            .contains("Severity: MINOR");
    }

}
