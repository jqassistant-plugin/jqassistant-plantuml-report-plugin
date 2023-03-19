package org.jqassistant.plugin.plantumlreport.sequence;

import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import org.jqassistant.plugin.plantumlreport.AbstractDiagramRenderer;
import org.jqassistant.plugin.plantumlreport.AbstractPlantUMLReportPlugin;
import org.jqassistant.plugin.plantumlreport.RenderMode;

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
