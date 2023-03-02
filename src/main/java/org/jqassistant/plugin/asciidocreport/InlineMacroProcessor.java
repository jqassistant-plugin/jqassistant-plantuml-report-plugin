package org.jqassistant.plugin.asciidocreport;

import java.util.HashMap;
import java.util.Map;

import com.buschmais.jqassistant.core.shared.asciidoc.DocumentParser;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.StructuralNode;

public class InlineMacroProcessor extends org.asciidoctor.extension.InlineMacroProcessor {

    public static final String CONCEPT_REF = "conceptRef";
    public static final String CONSTRAINT_REF = "constraintRef";

    private final DocumentParser documentParser;

    public InlineMacroProcessor(DocumentParser documentParser) {
        super("jQA");
        this.documentParser = documentParser;
    }

    @Override
    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        if (CONCEPT_REF.equals(target)) {
            DocumentParser.Result result = documentParser.parse(parent.getDocument());
            return processRef(parent, attributes, result.getConcepts());
        } else if (CONSTRAINT_REF.equals(target)) {
            DocumentParser.Result result = documentParser.parse(parent.getDocument());
            return processRef(parent, attributes, result.getConstraints());
        }
        throw new IllegalArgumentException("Unknown jQAssistant macro '" + target + "'");
    }

    private Object processRef(ContentNode parent, Map<String, Object> attributes, Map<String, StructuralNode> blocks) {
        Object rule = attributes.get("1");
        if (rule != null && blocks.containsKey(rule)) {
            Map<String, Object> options = new HashMap<>();
            options.put("type", ":link");
            options.put("target", "#" + rule);
            return createPhraseNode(parent, "anchor", rule.toString(), attributes, options);
        }
        return rule;
    }

}
