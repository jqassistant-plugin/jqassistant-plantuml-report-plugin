package org.jqassistant.plugin.plantumlreport.clazz;

import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;

import org.jqassistant.plugin.plantumlreport.AbstractDiagramRenderer;
import org.jqassistant.plugin.plantumlreport.AbstractPlantUMLReportPlugin;

public class ClassDiagramReportPlugin extends AbstractPlantUMLReportPlugin {

    @Override
    protected AbstractDiagramRenderer getRenderer() {
        return new ClassDiagramRenderer(new ClassDiagramResultConverter(new SubGraphFactory()));
    }

    @Override
    protected String getReportLabel() {
        return "Java Class Diagram";
    }
}
