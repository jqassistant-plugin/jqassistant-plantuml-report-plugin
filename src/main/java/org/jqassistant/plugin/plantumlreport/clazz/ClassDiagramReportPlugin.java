package org.jqassistant.plugin.plantumlreport.clazz;

import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import org.jqassistant.plugin.plantumlreport.AbstractDiagramRenderer;
import org.jqassistant.plugin.plantumlreport.AbstractPlantUMLReportPlugin;
import org.jqassistant.plugin.plantumlreport.RenderMode;

public class ClassDiagramReportPlugin extends AbstractPlantUMLReportPlugin {

    @Override
    protected AbstractDiagramRenderer getRenderer(RenderMode renderMode) {
        return new ClassDiagramRenderer(new ClassDiagramResultConverter(new SubGraphFactory()), renderMode);
    }

    @Override
    protected String getReportLabel() {
        return "Java Class Diagram";
    }
}
