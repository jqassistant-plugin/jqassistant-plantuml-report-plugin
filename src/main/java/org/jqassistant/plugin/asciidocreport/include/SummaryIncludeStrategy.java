package org.jqassistant.plugin.asciidocreport.include;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import org.jqassistant.plugin.asciidocreport.RuleResult;
import org.jqassistant.plugin.asciidocreport.StatusHelper;

import lombok.extern.slf4j.Slf4j;
import org.jqassistant.plugin.asciidocreport.InlineMacroProcessor;

@Slf4j
public class SummaryIncludeStrategy extends AbstractIncludeStrategy {

    private final SummaryFilter summaryFilter;

    public SummaryIncludeStrategy(Map<String, RuleResult> conceptResults, Map<String, RuleResult> constraintResults, SummaryFilter summaryFilter) {
        super(conceptResults, constraintResults);
        this.summaryFilter = summaryFilter;
    }

    @Override
    public String getName() {
        return "Summary";
    }

    @Override
    public void process(Map<String, Object> attributes, StringBuilder builder) {
        SummaryFilter.Result result = summaryFilter.apply(attributes);
        includeSummaryTable("Constraints", InlineMacroProcessor.CONSTRAINT_REF, result.getConstraints(), builder);
        includeSummaryTable("Concepts", InlineMacroProcessor.CONCEPT_REF, result.getConcepts(), builder);
    }

    private void includeSummaryTable(String title, String referenceMacro, Collection<RuleResult> results, StringBuilder builder) {
        if (!results.isEmpty()) {
            builder.append('.').append(title).append('\n');
            builder.append("[options=header,role=summary]").append('\n');
            builder.append("|===").append('\n');
            builder.append("| Id | Description | Severity | Status").append('\n');
            Set<RuleResult> ruleResults = new TreeSet<>(StatusHelper.getRuleResultComparator());
            ruleResults.addAll(results);
            for (RuleResult result : ruleResults) {
                ExecutableRule rule = result.getRule();
                builder.append("| ").append("jQA:").append(referenceMacro).append('[').append(rule.getId()).append(']');
                builder.append("| ").append(escape(rule.getDescription()));
                builder.append("| ").append(result.getEffectiveSeverity().getInfo(rule.getSeverity()));
                Result.Status status = result.getStatus();
                String statusClass = StatusHelper.getStatusClass(status);
                builder.append("| ").append("[").append(statusClass).append("]#").append(status).append('#').append('\n');
            }
            builder.append("|===").append('\n');
        }
    }
}
