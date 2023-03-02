package org.jqassistant.plugin.asciidocreport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.report.impl.CompositeReportPlugin;
import com.buschmais.jqassistant.core.rule.api.model.Concept;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.core.rule.api.model.Severity;
import org.jqassistant.plugin.asciidocreport.plantuml.RenderMode;
import com.buschmais.jqassistant.plugin.common.api.model.ArtifactFileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.DependsOnDescriptor;
import com.buschmais.xo.neo4j.api.model.Neo4jLabel;
import com.buschmais.xo.neo4j.api.model.Neo4jNode;
import com.buschmais.xo.neo4j.api.model.Neo4jRelationship;
import com.buschmais.xo.neo4j.api.model.Neo4jRelationshipType;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.*;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsciidocReportPluginTest extends AbstractAsciidocReportPluginTest {

    @Override
    protected List<String> getAsciidocFiles() {
        return asList("index.adoc", "additional-rules/importedRules.adoc");
    }

    @Test
    void defaultIndexDocument() throws RuleException, IOException {
        verify(emptyMap(), new File(outputDirectory, "report/asciidoc"));
    }

    @Test
    void defaultReportDirectory() throws RuleException, IOException {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("asciidoc.report.rule.directory", ruleDirectory.getAbsolutePath());
        properties.put("asciidoc.report.file.include", "index.adoc");
        verify(properties, new File(outputDirectory, "report/asciidoc"));
    }

    @Test
    void customReportDirectory() throws RuleException, IOException {
        File customReportDirectory = new File(outputDirectory, "report/custom-report");
        Map<String, Object> properties = new HashMap<>();
        properties.put("asciidoc.report.rule.directory", ruleDirectory.getAbsolutePath());
        properties.put("asciidoc.report.file.include", "index.adoc");
        properties.put("asciidoc.report.directory", customReportDirectory.getAbsolutePath());
        verify(properties, customReportDirectory);
    }

    @Test
    void smetanaPlantUmlRenderer() throws RuleException, IOException {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("asciidoc.report.rule.directory", ruleDirectory.getAbsolutePath());
        properties.put("asciidoc.report.file.include", "index.adoc");
        properties.put("plantuml.report.rendermode", "smetana");
        verify(properties, new File(outputDirectory, "report/asciidoc"));
        File plantUmlFile = new File(outputDirectory, "report/plantuml/test_ComponentDiagram.plantuml");
        String plantuml = FileUtils.readFileToString(plantUmlFile, "UTF-8");
        assertThat(plantuml).contains(RenderMode.SMETANA.getPragma());
    }

    @Test
    void elkPlantUmlRenderer() throws RuleException, IOException {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("asciidoc.report.rule.directory", ruleDirectory.getAbsolutePath());
        properties.put("asciidoc.report.file.include", "index.adoc");
        properties.put("plantuml.report.rendermode", "elk");
        verify(properties, new File(outputDirectory, "report/asciidoc"));
        File plantUmlFile = new File(outputDirectory, "report/plantuml/test_ComponentDiagram.plantuml");
        String plantuml = FileUtils.readFileToString(plantUmlFile, "UTF-8");
        assertThat(plantuml).contains(RenderMode.ELK.getPragma());
    }

    private String verify(Map<String, Object> properties, File expectedDirectory) throws RuleException, IOException {
        ReportContext reportContext = configureReportContext(properties);

        Concept componentDiagram = execute();

        String file = "index.html";
        File indexHtml = new File(expectedDirectory, file);
        assertThat(indexHtml.exists()).isTrue();

        String html = FileUtils.readFileToString(indexHtml, "UTF-8");

        Document document = Jsoup.parse(html);
        Elements summaryTables = document.getElementsByClass("summary");
        assertThat(summaryTables.size()).isEqualTo(2);
        verifyConstraintsSummary(summaryTables.get(0));
        verifyConceptsSummary(summaryTables.get(1));

        verifyRule(document, "test:Concept", "Concept Description", SUCCESS, "Status: SUCCESS, Severity: MAJOR (from MINOR)");
        verifyRuleResult(document, "test:Concept", "Value", "Foo Bar");

        verifyRule(document, "test:ImportedConcept", "Imported Concept", WARNING, "Status: WARNING, Severity: MINOR");
        verifyRuleResult(document, "test:ImportedConcept", "ImportedConceptValue", "FooBar");

        verifyToggle(html);

        verifyDiagram(componentDiagram, reportContext, html);

        return html;
    }

    private Concept execute() throws RuleException {
        ReportPlugin plugin = new CompositeReportPlugin(reportPlugins);
        plugin.initialize();
        plugin.begin();

        Concept concept = ruleSet.getConceptBucket().getById("test:Concept");
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("Value", asList("Foo", "Bar"));
        rows.add(row);
        processConcept(plugin, concept, new Result<>(concept, SUCCESS, Severity.MAJOR, singletonList("Value"), rows));

        Constraint constraint = ruleSet.getConstraintBucket().getById("test:Constraint");
        processConstraint(plugin, constraint, Result.<Constraint> builder().rule(constraint).status(FAILURE)
            .severity(Severity.MAJOR).columnNames(singletonList("Value")).rows(rows).build());

        Concept componentDiagram = ruleSet.getConceptBucket().getById("test:ComponentDiagram");
        List<Map<String, Object>> diagramRows = new ArrayList<>();
        Neo4jLabel packageLabel = mock(Neo4jLabel.class);
        when(packageLabel.getName()).thenReturn("Package");
        ArtifactFileDescriptor node1 = createNode(1l, "a");
        ArtifactFileDescriptor node2 = createNode(2l, "b");
        DependsOnDescriptor dependsOn = createRelationship(1l, node1, node2);
        Map<String, Object> diagramRow1 = new HashMap<>();
        diagramRow1.put("Node", node1);
        diagramRow1.put("DependsOn", dependsOn);
        diagramRows.add(diagramRow1);
        Map<String, Object> diagramRow2 = new HashMap<>();
        diagramRow2.put("Node", node2);
        diagramRow2.put("DependsOn", null);
        diagramRows.add(diagramRow2);
        processConcept(plugin, componentDiagram, Result.<Concept> builder().rule(componentDiagram).status(SUCCESS).severity(Severity.INFO)
                .columnNames(asList("Node", "DependsOn")).rows(diagramRows).build());

        Concept importedConcept = ruleSet.getConceptBucket().getById("test:ImportedConcept");
        List<Map<String, Object>> importedConceptRows = new ArrayList<>();
        Map<String, Object> importedConceptRow = new HashMap<>();
        importedConceptRow.put("ImportedConceptValue", asList("FooBar"));
        importedConceptRows.add(importedConceptRow);
        processConcept(plugin, importedConcept, Result.<Concept> builder().rule(importedConcept).status(WARNING).severity(Severity.MINOR)
                .columnNames(singletonList("ImportedConceptValue")).rows(importedConceptRows).build());

        Constraint importedConstraintWithoutDescription = ruleSet.getConstraintBucket().getById("test:ImportedConstraintWithoutDescription");
        processConstraint(plugin, importedConstraintWithoutDescription, Result.<Constraint> builder().rule(importedConstraintWithoutDescription).status(SUCCESS)
                .severity(Severity.MAJOR).columnNames(emptyList()).rows(emptyList()).build());

        plugin.end();
        plugin.destroy();
        return componentDiagram;
    }

    private void verifyToggle(String html) {
        // Toggle for rule content (i.e. Cypher source)
        assertThat(html).contains("<input type=\"checkbox\" class=\"jqassistant-rule-toggle\" title=\"Rule details\">");
        assertThat(html).contains("<div class=\"content\" id=\"jqassistant-rule-listing0\">");
        assertThat(html).contains("<style>" + lineSeparator() + //
                "#jqassistant-rule-listing0{" + lineSeparator() + //
                "  display:none;" + lineSeparator() + //
                "}" + lineSeparator() + //
                "input.jqassistant-rule-toggle:checked + #jqassistant-rule-listing0{" + lineSeparator() + //
                "  display:block;" + lineSeparator() + //
                "}");
    }

    private void verifyDiagram(Concept concept, ReportContext reportContext, String html) {
        // PlantUML diagram
        File plantumlReportDirectory = reportContext.getReportDirectory("plantuml");
        assertThat(new File(plantumlReportDirectory, "test_ComponentDiagram.svg").exists()).isTrue();
        assertThat(new File(plantumlReportDirectory, "test_ComponentDiagram.plantuml").exists()).isTrue();
        List<ReportContext.Report<?>> componentDiagrams = reportContext.getReports(concept);
        assertThat(componentDiagrams.size()).isEqualTo(1);
        String expectedDiagramUrl = "../plantuml/test_ComponentDiagram.svg";
        String expectedImageLink = "<a href=\"" + expectedDiagramUrl + "\"><img src=\"" + expectedDiagramUrl + "\"></a>";
        assertThat(html).contains(expectedImageLink);
    }

    private void verifyConstraintsSummary(Element constraintSummaryTable) {
        assertThat(constraintSummaryTable.getElementsByTag("caption").first().text()).contains("Constraints");
        Element constraintSummaryTableBody = constraintSummaryTable.getElementsByTag("tbody").first();
        Elements rows = constraintSummaryTableBody.getElementsByTag("tr");
        assertThat(rows.size()).isEqualTo(2);
        verifySummaryColumns(rows.get(0), "test:Constraint", "Constraint Description", "MAJOR", "FAILURE", "jqassistant-status-failure");
        verifySummaryColumns(rows.get(1), "test:ImportedConstraintWithoutDescription", "", "MAJOR", "SUCCESS", "jqassistant-status-success");
    }

    private void verifyConceptsSummary(Element conceptSummaryTable) {
        assertThat(conceptSummaryTable.getElementsByTag("caption").first().text()).contains("Concepts");
        Element conceptSummaryTableBody = conceptSummaryTable.getElementsByTag("tbody").first();
        assertThat(conceptSummaryTable).isNotNull();
        Elements rows = conceptSummaryTableBody.getElementsByTag("tr");
        assertThat(rows.size()).isEqualTo(3);
        verifySummaryColumns(rows.get(0), "test:ImportedConcept", "Imported Concept", "MINOR", "WARNING", "jqassistant-status-warning");
        verifySummaryColumns(rows.get(1), "test:Concept", "Concept Description", "MAJOR (from MINOR)", "SUCCESS", "jqassistant-status-success");
        verifySummaryColumns(rows.get(2), "test:ComponentDiagram", "Component Diagram Description", "INFO (from MINOR)", "SUCCESS",
                "jqassistant-status-success");
    }

    private void verifySummaryColumns(Element row, String expectedId, String expectedDescription, String expectedSeverity, String expectedStatus,
            String expectedStatusClass) {
        Elements columns = row.getElementsByTag("td");
        Element id = columns.get(0).getElementsByTag("a").first();
        assertThat(id).isNotNull();
        assertThat(id.text()).isEqualTo(expectedId);
        assertThat(id.attr("href")).isEqualTo("#" + expectedId);

        Elements description = columns.get(1).getElementsByTag("p");
        assertThat(description).isNotNull();
        assertThat(description.text()).isEqualTo(expectedDescription);

        Elements severity = columns.get(2).getElementsByTag("p");
        assertThat(severity).isNotNull();
        assertThat(severity.text()).isEqualTo(expectedSeverity);

        Elements status = columns.get(3).getElementsByTag("span");
        assertThat(status).isNotNull();
        assertThat(status.text()).isEqualTo(expectedStatus);
        assertThat(status.hasClass(expectedStatusClass)).isTrue();
    }

    private ArtifactFileDescriptor createNode(long id, String name) {
        Neo4jNode node = mock(Neo4jNode.class);
        when(node.getId()).thenReturn(id);
        Neo4jLabel artifactLabel = mock(Neo4jLabel.class);
        when(artifactLabel.getName()).thenReturn("Artifact");
        when(node.getLabels()).thenReturn(asList(artifactLabel));
        ArtifactFileDescriptor artifactFileDescriptor = mock(ArtifactFileDescriptor.class);
        when(artifactFileDescriptor.getFullQualifiedName()).thenReturn(name);
        when(artifactFileDescriptor.getDelegate()).thenReturn(node);
        return artifactFileDescriptor;
    }

    private DependsOnDescriptor createRelationship(long id, ArtifactFileDescriptor start, ArtifactFileDescriptor end) {
        Neo4jRelationshipType relationshipType = mock(Neo4jRelationshipType.class);
        when(relationshipType.getName()).thenReturn("DEPENDS_ON");
        Neo4jRelationship relationship = mock(Neo4jRelationship.class);
        when(relationship.getId()).thenReturn(id);
        when(relationship.getType()).thenReturn(relationshipType);
        Neo4jNode startNode = start.getDelegate();
        Neo4jNode endNode = end.getDelegate();
        when(relationship.getStartNode()).thenReturn(startNode);
        when(relationship.getEndNode()).thenReturn(endNode);
        DependsOnDescriptor dependsOnDescriptor = mock(DependsOnDescriptor.class);
        when(dependsOnDescriptor.getDependent()).thenReturn(start);
        when(dependsOnDescriptor.getDependency()).thenReturn(end);
        when(dependsOnDescriptor.getDelegate()).thenReturn(relationship);
        return dependsOnDescriptor;
    }
}
