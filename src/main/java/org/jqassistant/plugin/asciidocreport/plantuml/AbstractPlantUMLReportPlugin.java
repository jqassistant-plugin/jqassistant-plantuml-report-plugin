package org.jqassistant.plugin.asciidocreport.plantuml;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractPlantUMLReportPlugin implements ReportPlugin {

    private static final String PROPERTY_FILE_FORMAT = "plantuml.report.format";
    private static final String PROPERTY_RENDER_MODE = "plantuml.report.rendermode";

    private ReportContext reportContext;

    private File directory;

    private String fileFormat;

    private String renderMode;

    @Override
    public void initialize() {
    }

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {
        this.reportContext = reportContext;
        directory = reportContext.getReportDirectory("plantuml");
        // avoiding PlantUML types (e.g. net.sourceforge.plantuml.FileFormat) here to avoid warnings on startup if e.g. dot.exe is not present
        this.fileFormat = (String) properties.getOrDefault(PROPERTY_FILE_FORMAT, "svg");
        this.renderMode = (String) properties.get(PROPERTY_RENDER_MODE);
    }

    @Override
    public void setResult(Result<? extends ExecutableRule> result) throws ReportException {
        String diagram = getRenderer(RenderMode.getRenderMode(renderMode)).renderDiagram(result);
        ImageRenderer imageRenderer = new ImageRenderer();
        File file = imageRenderer.renderDiagram(diagram, result.getRule(), directory, fileFormat);
        URL url;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new ReportException("Cannot convert file '" + file.getAbsolutePath() + "' to URL");
        }
        reportContext.addReport(getReportLabel(), result.getRule(), ReportContext.ReportType.IMAGE, url);
    }

    protected abstract AbstractDiagramRenderer getRenderer(RenderMode renderMode);

    protected abstract String getReportLabel();

}
