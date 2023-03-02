package org.jqassistant.plugin.asciidocreport;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.PreprocessorReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PluginIncludeProcessorTest {

    @Mock
    private Document document;

    @Mock
    private PreprocessorReader preprocessorReader;

    private final PluginIncludeProcessor classpathIncludeProcessor = new PluginIncludeProcessor(PluginIncludeProcessorTest.class.getClassLoader(),
        "test/index.adoc");

    @Test
    void relativeInclude() {
        verifyInclude("include/include.adoc");
    }

    @Test
    void absoluteInclude() {
        verifyInclude("/test/include/include.adoc");
    }

    @Test
    void nonMatchingInclude() {
        classpathIncludeProcessor.process(document, preprocessorReader, "non-matching.adoc", emptyMap());

        verify(preprocessorReader, never()).pushInclude(anyString(), anyString(), anyString(), anyInt(), anyMap());
    }

    private void verifyInclude(String target) {
        classpathIncludeProcessor.process(document, preprocessorReader, target, emptyMap());

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(preprocessorReader).pushInclude(dataCaptor.capture(), eq(target), anyString(), eq(1), anyMap());

        assertThat(dataCaptor.getValue()).startsWith("= Test");
    }

}
