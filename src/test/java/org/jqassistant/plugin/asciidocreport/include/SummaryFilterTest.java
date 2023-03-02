package org.jqassistant.plugin.asciidocreport.include;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jqassistant.plugin.asciidocreport.RuleResult;

import org.asciidoctor.ast.StructuralNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SummaryFilterTest {

    private Map<String, RuleResult> conceptResults = new HashMap<>();
    private Map<String, RuleResult> constraintResults = new HashMap<>();
    private Map<String, StructuralNode> ruleBlocks = new HashMap<>();

    @Mock
    private RuleResult conceptResult;
    @Mock
    private RuleResult importedConceptResult;
    @Mock
    private RuleResult constraintResult;
    @Mock
    private RuleResult importedConstraintResult;

    private SummaryFilter summaryFilter;

    @BeforeEach
    void setUp() {
        conceptResults.put("concept", conceptResult);
        conceptResults.put("importedConcept", importedConceptResult);
        constraintResults.put("constraint", constraintResult);
        constraintResults.put("importedConstraint", importedConstraintResult);
        ruleBlocks.put("concept", mock(StructuralNode.class));
        ruleBlocks.put("constraint", mock(StructuralNode.class));
        summaryFilter = new SummaryFilter(conceptResults, constraintResults, ruleBlocks);
    }

    @Test
    void emtpyFilter() {
        SummaryFilter.Result result = summaryFilter.apply(Collections.emptyMap());

        assertThat(result.getConcepts()).containsOnly(conceptResult, importedConceptResult);
        assertThat(result.getConstraints()).containsOnly(constraintResult, importedConstraintResult);
    }

    @Test
    void concepts() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("concepts", "concept");

        SummaryFilter.Result result = summaryFilter.apply(attributes);

        assertThat(result.getConcepts()).containsOnly(conceptResult);
        assertThat(result.getConstraints()).isEmpty();
    }

    @Test
    void importedConcepts() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("importedConcepts", "importedConcept");

        SummaryFilter.Result result = summaryFilter.apply(attributes);

        assertThat(result.getConcepts()).containsOnly(importedConceptResult);
        assertThat(result.getConstraints()).isEmpty();
    }

    @Test
    void conceptsAndimportedConcepts() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("concepts", "concept");
        attributes.put("importedConcepts", "importedConcept");

        SummaryFilter.Result result = summaryFilter.apply(attributes);

        assertThat(result.getConcepts()).containsOnly(conceptResult, importedConceptResult);
        assertThat(result.getConstraints()).isEmpty();
    }

    @Test
    void constraints() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("constraints", "constraint");

        SummaryFilter.Result result = summaryFilter.apply(attributes);

        assertThat(result.getConcepts()).isEmpty();
        ;
        assertThat(result.getConstraints()).containsOnly(constraintResult);
    }

    @Test
    void importedConstraints() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("importedConstraints", "importedConstraint");

        SummaryFilter.Result result = summaryFilter.apply(attributes);

        assertThat(result.getConcepts()).isEmpty();
        ;
        assertThat(result.getConstraints()).containsOnly(importedConstraintResult);
    }

    @Test
    void constraintsAndimportedConstraints() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("constraints", "constraint");
        attributes.put("importedConstraints", "importedConstraint");

        SummaryFilter.Result result = summaryFilter.apply(attributes);

        assertThat(result.getConcepts()).isEmpty();
        assertThat(result.getConstraints()).containsOnly(constraintResult, importedConstraintResult);
    }

    @Test
    void conceptsAndConstraints() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("concepts", "*");
        attributes.put("constraints", "*");

        SummaryFilter.Result result = summaryFilter.apply(attributes);

        assertThat(result.getConcepts()).containsOnly(conceptResult);
        assertThat(result.getConstraints()).containsOnly(constraintResult);
    }

    @Test
    void importedConceptsAndimportedConstraints() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("importedConcepts", "*");
        attributes.put("importedConstraints", "*");

        SummaryFilter.Result result = summaryFilter.apply(attributes);

        assertThat(result.getConcepts()).containsOnly(importedConceptResult);
        assertThat(result.getConstraints()).containsOnly(importedConstraintResult);
    }
}
