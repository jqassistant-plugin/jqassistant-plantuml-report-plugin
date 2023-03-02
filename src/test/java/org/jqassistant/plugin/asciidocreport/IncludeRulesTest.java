package org.jqassistant.plugin.asciidocreport;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.impl.CompositeReportPlugin;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.core.rule.api.model.Severity;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.SUCCESS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class IncludeRulesTest extends AbstractAsciidocReportPluginTest {

    @Override
    protected List<String> getAsciidocFiles() {
        return asList("includeRules.adoc", "additional-rules/includedRules.adoc");
    }

    @Test
    void include() throws RuleException, IOException {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("asciidoc.report.rule.directory", ruleDirectory.getAbsolutePath());
        properties.put("asciidoc.report.file.include", "includeRules.adoc");
        configureReportContext(properties);

        ReportPlugin plugin = new CompositeReportPlugin(reportPlugins);
        plugin.begin();

        processConcept(plugin, "test:IncludedConcept", SUCCESS, Severity.MINOR, singletonList("Value"), emptyList());
        processConcept(plugin, "test:NonIncludedConcept", SUCCESS, Severity.MINOR, singletonList("Value"), emptyList());
        processConstraint(plugin, "test:IncludedConstraint", SUCCESS, Severity.MAJOR, singletonList("Value"), emptyList());
        processConstraint(plugin, "test:NonIncludedConstraint", SUCCESS, Severity.MAJOR, singletonList("Value"), emptyList());

        plugin.end();

        File indexHtml = new File(outputDirectory, "report/asciidoc/includeRules.html");
        assertThat(indexHtml.exists()).isTrue();
        String html = FileUtils.readFileToString(indexHtml, "UTF-8");
        Document document = Jsoup.parse(html);

        verifyRule(document, "test:IncludedConcept", "Included Concept", SUCCESS, "Status: SUCCESS, Severity: MINOR");
        assertThat(document.getElementById("test:NonIncludedConcept")).isNull();

        verifyRule(document, "test:IncludedConstraint", "Included Constraint", SUCCESS, "Status: SUCCESS, Severity: MAJOR");
        assertThat(document.getElementById("test:NonIncludedConstraint")).isNull();

    }

}
