package org.jqassistant.plugin.asciidocreport.include;

import java.util.Map;
import java.util.Set;

import com.buschmais.jqassistant.core.rule.api.model.*;
import org.jqassistant.plugin.asciidocreport.RuleResult;

import org.asciidoctor.ast.StructuralNode;

public abstract class AbstractIncludeRulesStrategy extends AbstractIncludeStrategy {

    protected final Map<String, StructuralNode> ruleBlocks;

    private final Set<ExecutableRule<?>> includedRules;

    protected AbstractIncludeRulesStrategy(Map<String, RuleResult> conceptResults, Map<String, RuleResult> constraintResults,
            Map<String, StructuralNode> ruleBlocks, Set<ExecutableRule<?>> includedRules) {
        super(conceptResults, constraintResults);
        this.ruleBlocks = ruleBlocks;
        this.includedRules = includedRules;
    }

    protected void includeRuleResult(RuleResult ruleResult, StringBuilder builder) {
        ExecutableRule<?> rule = ruleResult.getRule();
        // Include a rule only if it is not declared in the rendered document or already
        // included before
        if (!ruleBlocks.containsKey(rule.getId()) && includedRules.add(rule)) {
            builder.append("[[").append(rule.getId()).append("]]").append('\n');
            String language = null;
            String source = null;
            Executable<?> executable = rule.getExecutable();
            if (executable instanceof CypherExecutable) {
                language = "cypher";
                source = ((CypherExecutable) executable).getSource();
            } else if (executable instanceof ScriptExecutable) {
                ScriptExecutable scriptExecutable = (ScriptExecutable) executable;
                language = executable.getLanguage();
                source = scriptExecutable.getSource();
            }
            String ruleType;
            if (rule instanceof Concept) {
                ruleType = "concept";
            } else if (rule instanceof Constraint) {
                ruleType = "constraint";
            } else {
                throw new IllegalArgumentException("Cannot determine type of rule " + executable);
            }
            builder.append("[source,").append(language).append(",role=").append(ruleType).append(",indent=0").append("]").append('\n');
            builder.append('.').append(escape(rule.getDescription())).append('\n');
            builder.append("----").append('\n');
            if (source != null) {
                builder.append(source).append('\n');
            }
            builder.append("----").append('\n');
            builder.append('\n');
        }
    }
}
