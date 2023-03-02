package org.jqassistant.plugin.asciidocreport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.*;

/**
 * {@link Postprocessor} that hides listing blocks of concepts and constraints
 * by default and adds a toggle to show them.
 */
public class RulePostProcessor extends Postprocessor {

    private final Map<String, RuleResult> conceptResults;
    private final Map<String, RuleResult> constraintResults;

    public RulePostProcessor(Map<String, RuleResult> conceptResults, Map<String, RuleResult> constraintResults) {
        this.conceptResults = conceptResults;
        this.constraintResults = constraintResults;
    }

    @Override
    public String process(Document document, String output) {
        if (document.basebackend("html")) {
            return HtmlRulePostProcessor.process(conceptResults, constraintResults, output);
        }
        return output;
    }

    public static class HtmlRulePostProcessor {

        static String process(Map<String, RuleResult> conceptResults, Map<String, RuleResult> constraintResults, String output) {
            org.jsoup.nodes.Document doc = Jsoup.parse(output, "UTF-8");
            List<String> contentIds = new ArrayList<>();
            processRuleBlocks(doc, "concept", conceptResults, contentIds);
            processRuleBlocks(doc, "constraint", constraintResults, contentIds);
            addStyles(doc, contentIds);
            return doc.html();
        }

        private static void processRuleBlocks(org.jsoup.nodes.Document doc, String ruleClass, Map<String, RuleResult> ruleResults, List<String> contentIds) {
            Elements ruleBlocks = doc.getElementsByClass(ruleClass);
            for (Element ruleBlock : ruleBlocks) {
                addStatus(ruleBlock, ruleResults);
                if (ruleBlock.hasClass("listingblock")) {
                    hideListing(ruleBlock, contentIds);
                }
            }
        }

        private static void addStatus(Element ruleBlock, Map<String, RuleResult> ruleResults) {
            Elements title = ruleBlock.getElementsByClass("title");
            Element titleElement = title.first();
            Element status;
            if (titleElement != null) {
                // insert status before title
                status = new Element("div");
                titleElement.before(status);
                titleElement.attr("style", "display:inline;");
            } else {
                //
                status = ruleBlock.prependElement("div");
            }
            status.addClass("jqassistant-rule-status");
            RuleResult ruleResult = ruleResults.get(ruleBlock.id());
            if (ruleResult != null) {
                ExecutableRule<?> rule = ruleResult.getRule();
                status.addClass(StatusHelper.getStatusClass(ruleResult.getStatus()));
                switch (ruleResult.getStatus()) {
                case SUCCESS:
                    status.addClass("fa").addClass("fa-check");
                    break;
                case WARNING:
                    status.addClass("fa").addClass("fa-exclamation");
                    break;
                case FAILURE:
                    status.addClass("fa").addClass("fa-ban");
                    break;
                }
                String hover = "Id: " + ruleResult.getRule()
                    .getId() + ", Status: " + ruleResult.getStatus() + ", Severity: " + ruleResult.getEffectiveSeverity()
                    .getInfo(rule.getSeverity());
                status.attr("title", hover);
            } else {
                status.addClass("fa").addClass("fa-question");
                status.attr("title", "Rule has not been executed.");
            }
        }

        private static void hideListing(Element ruleBlock, List<String> contentIds) {
            String contentId = "jqassistant-rule-listing" + contentIds.size();
            contentIds.add(contentId);
            Elements contents = ruleBlock.getElementsByClass("content");
            Element content = contents.first();
            content.before("<input type=\"checkbox\" class=\"jqassistant-rule-toggle\" title=\"Rule details\"/>");
            contents.attr("id", contentId);
        }

        private static void addStyles(org.jsoup.nodes.Document doc, List<String> contentIds) {
            StringBuilder styles = new StringBuilder();
            styles.append("<style>\n");
            for (String contentId : contentIds) {
                styles.append("#").append(contentId).append("{\n");
                styles.append("  display:none;\n"); // disable source content blocks by default
                styles.append("}\n");
                styles.append("input.jqassistant-rule-toggle:checked + #").append(contentId).append("{\n");
                styles.append("  display:block;\n"); // activate them if the checkbox element is checked
                styles.append("}\n");
            }
            styles.append("." + StatusHelper.getStatusClass(SUCCESS) + "{color: green}");
            styles.append("." + StatusHelper.getStatusClass(WARNING) + "{color: orange}");
            styles.append("." + StatusHelper.getStatusClass(FAILURE) + "{color: crimson}");
            styles.append("." + StatusHelper.getStatusClass(SKIPPED) + "{color: gray}");
            styles.append("</style>\n");
            doc.head().append(styles.toString());
        }
    }
}
