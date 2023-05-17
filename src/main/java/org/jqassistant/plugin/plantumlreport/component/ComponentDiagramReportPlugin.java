package org.jqassistant.plugin.plantumlreport.component;

import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;

import org.jqassistant.plugin.plantumlreport.AbstractDiagramRenderer;
import org.jqassistant.plugin.plantumlreport.AbstractPlantUMLReportPlugin;

public class ComponentDiagramReportPlugin extends AbstractPlantUMLReportPlugin {

    @Override
    protected AbstractDiagramRenderer getRenderer() {
        return new ComponentDiagramRenderer(new SubGraphFactory());
    }

    @Override
    protected String getReportLabel() {
        return "Component Diagram";
    }
}
