package trajectorium.report;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvReportRenderer implements ReportRenderer {
    static final List<String> HEADER = List.of("name", "class", "mass_msun", "x_au", "y_au",
            "vx_au_yr", "vy_au_yr", "speed_au_yr", "speed_km_s", "radius_px", "color");

    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public void render(ReportData data, OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder(String.join(",", HEADER)).append('\n');
        for (ReportData.BodyRow b : data.bodies()) {
            sb.append(text(b.name())).append(',')
                    .append(b.massClass().name()).append(',')
                    .append(number(b.mass())).append(',')
                    .append(number(b.x())).append(',')
                    .append(number(b.y())).append(',')
                    .append(number(b.vx())).append(',')
                    .append(number(b.vy())).append(',')
                    .append(number(b.speed())).append(',')
                    .append(number(b.speedKmS())).append(',')
                    .append(number(b.radiusPx())).append(',')
                    .append(text(b.color())).append('\n');
        }
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String number(double value) {
        return Double.toString(value);
    }

    static String text(String value) {
        String s = value;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
