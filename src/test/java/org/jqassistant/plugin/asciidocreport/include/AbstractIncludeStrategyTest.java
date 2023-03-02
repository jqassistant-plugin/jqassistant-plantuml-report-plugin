package org.jqassistant.plugin.asciidocreport.include;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class AbstractIncludeStrategyTest {

    @Test
    public void escape() {
        AbstractIncludeStrategy includeStrategy = new AbstractIncludeStrategy(emptyMap(), emptyMap()) {

            @Override
            public String getName() {
                return "Test";
            }

            @Override
            public void process(Map<String, Object> attributes, StringBuilder builder) {

            }
        };
        assertThat(includeStrategy.escape("line1\nline2")).isEqualTo("line1 line2");
        assertThat(includeStrategy.escape(null)).isNull();
    }

}
