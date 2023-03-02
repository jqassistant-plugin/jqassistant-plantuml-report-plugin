package org.jqassistant.plugin.asciidocreport.include;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import com.buschmais.jqassistant.core.rule.api.filter.RuleFilter;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import org.jqassistant.plugin.asciidocreport.RuleResult;

import org.asciidoctor.ast.StructuralNode;

public class RulesIncludeStrategy extends AbstractIncludeRulesStrategy {

    public RulesIncludeStrategy(Map<String, RuleResult> conceptResults, Map<String, RuleResult> constraintResults, Map<String, StructuralNode> ruleBlocks,
        Set<ExecutableRule<?>> includedRules) {
        super(conceptResults, constraintResults, ruleBlocks, includedRules);
    }

    @Override
    public String getName() {
        return "Rules";
    }

    @Override
    public void process(Map<String, Object> attributes, StringBuilder builder) {
        includeRules(attributes, "concepts", conceptResults, builder);
        includeRules(attributes, "constraints", constraintResults, builder);

    }

    /**
     * Include the rules that are specified by an filter attribute.
     *
     * @param attributes
     *     The include attributes.
     * @param filterAttribute
     *     The name of the filter attribute.
     * @param results
     *     The {@link RuleResult}s.
     * @param builder
     *     The builder to use.s
     */
    private void includeRules(Map<String, Object> attributes, String filterAttribute, Map<String, RuleResult> results, StringBuilder builder) {
        String filterValue = (String) attributes.get(filterAttribute);
        SortedSet<String> matches = RuleFilter.match(results.keySet(), filterValue);
        for (String match : matches) {
            includeRuleResult(results.get(match), builder);
        }
    }
}
