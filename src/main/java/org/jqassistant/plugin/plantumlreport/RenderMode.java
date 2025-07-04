package org.jqassistant.plugin.plantumlreport;

import com.buschmais.jqassistant.core.report.api.ReportException;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.crash.ReportLog;
import net.sourceforge.plantuml.dot.GraphvizUtils;

import static java.util.Arrays.asList;

@Slf4j
public enum RenderMode {

    GRAPHVIZ(""),
    SMETANA("!pragma layout smetana\n"),
    ELK("!pragma layout elk");

    RenderMode(String pragma) {
        this.pragma = pragma;
    }

    private final String pragma;

    public String getPragma() {
        return pragma;
    }

    public static RenderMode getRenderMode(String value) throws ReportException {
        boolean graphvizAvailable = verifyGraphviz();
        if (value != null) {
            RenderMode renderMode = RenderMode.fromString(value);
            if (GRAPHVIZ == renderMode && !graphvizAvailable) {
                throw new ReportException("GraphViz is requested but installation could not be validated.");
            }
            return renderMode;
        }
        return graphvizAvailable ? GRAPHVIZ : SMETANA;
    }

    /**
     * Returns the {@link RenderMode} for the given string
     *
     * @param renderMode
     *     The {@link RenderMode} as string.
     * @return The matching {@link RenderMode}
     * @throws ReportException
     *     If renderMode is not valid.
     */
    private static RenderMode fromString(String renderMode) throws ReportException {
        for (RenderMode mode : RenderMode.values()) {
            if (mode.name()
                .equalsIgnoreCase(renderMode)) {
                return mode;
            }
        }
        throw new ReportException(renderMode + " is not a valid, supported modes are " + asList(RenderMode.values()));
    }

    private static boolean verifyGraphviz() {
        ReportLog reportLog = new ReportLog();
        if (GraphvizUtils.addDotStatus(reportLog, false) != 0) {
            for (String s : reportLog) {
                log.info(s);
            }
            return false;
        }
        return true;
    }
}
