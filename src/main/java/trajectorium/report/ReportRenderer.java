package trajectorium.report;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface ReportRenderer {
    void render(ReportData data, OutputStream out) throws IOException;

    String extension();

    default String defaultFileName(LocalDateTime time) {
        return "trajectorium-report-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(time)
                + "." + extension();
    }
}
