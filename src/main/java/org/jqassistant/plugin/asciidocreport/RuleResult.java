package org.jqassistant.plugin.asciidocreport;

import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import com.buschmais.jqassistant.core.rule.api.model.Severity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import static lombok.AccessLevel.PRIVATE;

@Builder
@Getter
@AllArgsConstructor(access = PRIVATE)
@ToString
public class RuleResult {

    private ExecutableRule rule;

    private Severity effectiveSeverity;

    private Result.Status status;

    private List<String> columnNames;

    @Singular
    private List<Map<String, List<String>>> rows;

    private SubGraph subGraph;

}
