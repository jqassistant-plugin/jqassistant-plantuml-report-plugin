package org.jqassistant.plugin.asciidocreport.plantuml.sequence;

import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import org.jqassistant.plugin.asciidocreport.plantuml.AbstractDiagramRenderer;
import org.jqassistant.plugin.asciidocreport.plantuml.AbstractPlantUMLReportPlugin;
import org.jqassistant.plugin.asciidocreport.plantuml.RenderMode;

public class SequenceDiagramReportPlugin extends AbstractPlantUMLReportPlugin {

    @Override
    protected AbstractDiagramRenderer getRenderer(RenderMode renderMode) {
        return new SequenceDiagramRenderer(new SubGraphFactory(), renderMode);
    }

    @Override
    protected String getReportLabel() {
        return "Sequence Diagram";
    }
}
