package org.jqassistant.plugin.asciidocreport.include;

import java.util.Map;

/**
 * Defines the interface for an include strategy
 */
public interface IncludeStrategy {

    /**
     * Return the name, used to map jQA:_Name_ to the implementing strategy.
     *
     * @return The name.
     */
    String getName();

    /**
     * Process an include with the given attributes.
     *
     * @param attributes
     *            The attributes.
     * @param builder
     *            The builder to append Asciidoc markup.
     */
    void process(Map<String, Object> attributes, StringBuilder builder);
}
