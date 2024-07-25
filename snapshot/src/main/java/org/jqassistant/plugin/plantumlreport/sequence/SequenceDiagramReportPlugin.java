package org.jqassistant.plugin.plantumlreport.sequence;

import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;

import org.jqassistant.plugin.plantumlreport.AbstractDiagramRenderer;
import org.jqassistant.plugin.plantumlreport.AbstractPlantUMLReportPlugin;

public class SequenceDiagramReportPlugin extends AbstractPlantUMLReportPlugin {

    @Override
    protected AbstractDiagramRenderer getRenderer() {
        return new SequenceDiagramRenderer(new SubGraphFactory());
    }

    @Override
    protected String getReportLabel() {
        return "Sequence Diagram";
    }
}
