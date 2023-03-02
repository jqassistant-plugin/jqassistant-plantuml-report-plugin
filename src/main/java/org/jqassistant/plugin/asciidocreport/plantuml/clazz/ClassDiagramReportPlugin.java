package org.jqassistant.plugin.asciidocreport.plantuml.clazz;

import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import org.jqassistant.plugin.asciidocreport.plantuml.AbstractDiagramRenderer;
import org.jqassistant.plugin.asciidocreport.plantuml.AbstractPlantUMLReportPlugin;
import org.jqassistant.plugin.asciidocreport.plantuml.RenderMode;

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
