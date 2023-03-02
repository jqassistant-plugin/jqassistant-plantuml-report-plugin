package org.jqassistant.plugin.asciidocreport;

import java.util.*;

import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import com.buschmais.jqassistant.core.shared.asciidoc.DocumentParser;

import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.PreprocessorReader;
import org.jqassistant.plugin.asciidocreport.include.*;

import static java.util.stream.Collectors.toMap;

public class IncludeProcessor extends org.asciidoctor.extension.IncludeProcessor {

    public static final String PREFIX = "jQA:";

    private Map<String, IncludeStrategy> strategies;

    public IncludeProcessor(DocumentParser documentParser, Document document, Map<String, RuleResult> conceptResults,
            Map<String, RuleResult> constraintResults) {
        DocumentParser.Result result = documentParser.parse(document);
        Map<String, StructuralNode> ruleBlocks = new HashMap<>();
        ruleBlocks.putAll(result.getConcepts());
        ruleBlocks.putAll(result.getConstraints());
        Set<ExecutableRule<?>> includedRules = new HashSet<>();
        SummaryFilter summaryFilter = new SummaryFilter(conceptResults, constraintResults, ruleBlocks);
        strategies = Arrays
                .<IncludeStrategy> asList(new SummaryIncludeStrategy(conceptResults, constraintResults, summaryFilter),
                        new RulesIncludeStrategy(conceptResults, constraintResults, ruleBlocks, includedRules),
                        new ImportedRulesIncludeStrategy(conceptResults, constraintResults, ruleBlocks, includedRules))
                .stream().collect(toMap(strategy -> strategy.getName(), strategy -> strategy));
    }

    @Override
    public boolean handles(String target) {
        return target.startsWith(PREFIX);
    }

    @Override
    public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
        String include = target.substring(PREFIX.length());
        IncludeStrategy strategy = strategies.get(include);
        StringBuilder builder = new StringBuilder();
        if (strategy != null) {
            strategy.process(attributes, builder);
        } else {
            throw new IllegalArgumentException("jQA include not supported: " + target);
        }
        reader.push_include(builder.toString(), target, include, 1, attributes);
    }
}
