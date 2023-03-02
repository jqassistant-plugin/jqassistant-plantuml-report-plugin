package org.jqassistant.plugin.asciidocreport;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import com.buschmais.jqassistant.core.shared.asciidoc.DocumentParser;

import org.apache.commons.lang3.StringEscapeUtils;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.Treeprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.buschmais.jqassistant.core.report.api.ReportContext.Report;

public class TreePreprocessor extends Treeprocessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreePreprocessor.class);

    private final DocumentParser documentParser;
    private final Map<String, RuleResult> conceptResults;
    private final Map<String, RuleResult> constraintResults;
    private final File outputFileDirectory;
    private final ReportContext reportContext;

    public TreePreprocessor(DocumentParser documentParser, Map<String, RuleResult> conceptResults, Map<String, RuleResult> constraintResults,
            File outputFileDirectory, ReportContext reportContext) {
        this.documentParser = documentParser;
        this.conceptResults = conceptResults;
        this.constraintResults = constraintResults;
        this.outputFileDirectory = outputFileDirectory;
        this.reportContext = reportContext;
    }

    public Document process(Document document) {
        DocumentParser.Result result = documentParser.parse(document);
        enrichResults(result.getConcepts(), conceptResults);
        enrichResults(result.getConstraints(), constraintResults);
        return document;
    }

    private void enrichResults(Map<String, StructuralNode> blocks, Map<String, RuleResult> results) {
        for (Map.Entry<String, StructuralNode> blockEntry : blocks.entrySet()) {
            String id = blockEntry.getKey();
            StructuralNode block = blockEntry.getValue();
            RuleResult result = results.get(id);
            List<String> content = renderRuleResult(result);
            ContentNode parent = block.getParent();
            List<StructuralNode> siblings = ((StructuralNode) parent).getBlocks();
            int i = siblings.indexOf(block);
            siblings.add(i + 1, createBlock((StructuralNode) parent, "paragraph", content, new HashMap<>(), new HashMap<>()));
        }
    }

    /**
     * Renders a {@link RuleResult} to HTML.
     *
     * @param result
     *            The {@link RuleResult}.
     * @return The HTML to be embedded into the document.
     */
    private List<String> renderRuleResult(RuleResult result) {
        List<String> content = new ArrayList<>();
        if (result != null) {
            ExecutableRule<?> rule = result.getRule();
            List<ReportContext.Report<?>> reports = reportContext.getReports(rule);
            content.add("<div id=\"result(" + rule.getId() + ")\">");
            if (!reports.isEmpty()) {
                for (ReportContext.Report<?> report : reports) {
                    switch (report.getReportType()) {
                    case IMAGE:
                        content.add(renderImage(getReportUrl(report)));
                        break;
                    case LINK:
                        content.add(renderDownloadLink(getReportUrl(report), report.getLabel()));
                        break;
                    }
                }
            } else if (!result.getRows().isEmpty()) {
                content.add(renderResultTable(result));
            }
            content.add("</div>");
        }
        return content;
    }

    /**
     * Returns a String representation of the URL of a {@link Report}.
     * <p>
     * If the URL references a file the URL is relative.
     *
     * @param report
     *            the {@link Report}.
     * @return The URL as {@link String} representation.
     */
    private String getReportUrl(ReportContext.Report<?> report) {
        URL url = report.getUrl();
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            return getRelativeReportUrl(url);
        }
        return url.toExternalForm();
    }

    /**
     * Determines a URL relative to the {@link #outputFileDirectory}.
     *
     * @param url
     *            The {@link URL}.
     * @return The relative URL as {@link String} representation.
     */
    private String getRelativeReportUrl(URL url) {
        Path path;
        try {
            path = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            LOGGER.info("Cannot determine path from URL '" + url + "'.", e);
            return url.toExternalForm();
        }
        Path relativePath = outputFileDirectory.getAbsoluteFile().toPath().relativize(path);
        return relativePath.toString().replace('\\', '/');
    }

    private String renderDownloadLink(String url, String label) {
        StringBuilder a = new StringBuilder();
        a.append("<span class=\"fa fa-download\"/>");
        a.append("<a href=").append('"').append(url).append('"').append(" style=\"text-decoration:none; color:initial\">");
        a.append("<b>");
        a.append(label);
        a.append("</b>");
        a.append("</a>");
        return a.toString();
    }

    /**
     * Renders a {@link RuleResult} as table.
     *
     * @param result
     *            The {@link RuleResult}.
     * @return The rendered table.
     */
    private String renderResultTable(RuleResult result) {
        List<String> columnNames = result.getColumnNames();
        StringBuilder tableBuilder = new StringBuilder();
        tableBuilder.append("<table>").append('\n');
        tableBuilder.append("<thead>").append('\n');
        tableBuilder.append("<tr>").append('\n');
        for (String columnName : columnNames) {
            tableBuilder.append("<th>").append(columnName).append("</th>").append('\n');
        }
        tableBuilder.append("</tr>").append('\n');
        tableBuilder.append("</thead>").append('\n');
        tableBuilder.append("<tbody>").append('\n');
        for (Map<String, List<String>> row : result.getRows()) {
            tableBuilder.append("<tr>").append('\n');
            for (String columnName : columnNames) {
                tableBuilder.append("<td>").append('\n');
                for (String value : row.get(columnName)) {
                    tableBuilder.append(StringEscapeUtils.escapeHtml4(value)).append('\n');
                }
                tableBuilder.append("</td>").append('\n');
            }
            tableBuilder.append("</tr>").append('\n');
        }
        tableBuilder.append("</tbody>").append('\n');
        tableBuilder.append("</table>").append('\n');
        return tableBuilder.toString();
    }

    /**
     * Embed an image with the given file name.
     *
     * @param url
     *            The {@link URL} of the image to embed.
     * @return The HTML to be embedded in the document.
     */
    private String renderImage(String url) {
        StringBuilder content = new StringBuilder();
        content.append("<div>");
        content.append("<a href=\"" + url + "\">");
        content.append("<img src=\"" + url + "\"/>");
        content.append("</a>");
        content.append("</div>");
        return content.toString();
    }
}
