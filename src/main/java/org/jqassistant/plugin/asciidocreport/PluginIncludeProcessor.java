package org.jqassistant.plugin.asciidocreport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import com.buschmais.jqassistant.core.rule.api.source.ClasspathRuleSource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

/**
 * {@link IncludeProcessor} translating include targets to classpath resources
 * relative to {@link ClasspathRuleSource#RULE_RESOURCE_PATH}.
 */
@Slf4j
public class PluginIncludeProcessor extends IncludeProcessor {

    private final ClassLoader classLoader;

    private final String rootPath;

    public PluginIncludeProcessor(ClassLoader classLoader, String path) {
        this.classLoader = classLoader;
        StringBuilder builder = new StringBuilder("/");
        int lastFileSeparatorIndex = path.lastIndexOf('/');
        if (lastFileSeparatorIndex >= 0) {
            builder.append(path, 0, lastFileSeparatorIndex);
        }
        builder.append('/');
        this.rootPath = builder.toString();
    }

    @Override
    public boolean handles(String target) {
        return getClasspathResource(target).isPresent();
    }

    @Override
    public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
        getClasspathResource(target).ifPresent(url -> {
            try (InputStream inputStream = url.openConnection().getInputStream()) {
                reader.pushInclude(IOUtils.toString(inputStream, UTF_8), target, url.toExternalForm(), 1, attributes);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot open input stream for class path resource " + url);
            }
        });
    }

    /**
     * Determines the {@link URL} of a classpath resource according to the given
     * include target.
     *
     * @param target
     *            The include target.
     * @return The optional {@link URL}.
     */
    private Optional<URL> getClasspathResource(String target) {
        StringBuilder resource = new StringBuilder(ClasspathRuleSource.RULE_RESOURCE_PATH);
        if (!target.startsWith("/")) {
            resource.append(rootPath);
        }
        resource.append(target);
        log.debug("Mapped include target '{}' to class path resource '{}'.", target, resource);
        return ofNullable(classLoader.getResource(resource.toString()));
    }

}
