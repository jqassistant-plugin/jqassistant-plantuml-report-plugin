package org.jqassistant.plugin.plantumlreport.component;

import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import org.jqassistant.plugin.plantumlreport.AbstractDiagramRenderer;
import org.jqassistant.plugin.plantumlreport.AbstractPlantUMLReportPlugin;
import org.jqassistant.plugin.plantumlreport.RenderMode;

public class ComponentDiagramReportPlugin extends AbstractPlantUMLReportPlugin {

    @Override
    protected AbstractDiagramRenderer getRenderer(RenderMode renderMode) {
        return new ComponentDiagramRenderer(new SubGraphFactory(), renderMode);
    }

    @Override
    protected String getReportLabel() {
        return "Component Diagram";
    }
}
