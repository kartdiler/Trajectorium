package trajectorium.report;

import trajectorium.i18n.I18n;
import trajectorium.report.ReportTables.Column;
import trajectorium.report.ReportTables.Kind;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TextReportRenderer implements ReportRenderer {
    private static final int MAX_COLUMN_WIDTH = 24;

    @Override
    public String extension() { return "txt"; }

    @Override
    public void render(ReportData data, OutputStream out) throws IOException {
        String text = String.join(System.lineSeparator(), lines(data)) + System.lineSeparator();
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }

    public static List<String> lines(ReportData data) {
        Locale locale = I18n.getLocale();
        List<String> out = new ArrayList<>();

        String title = I18n.get("report.title");
        out.add(title);
        out.add("=".repeat(title.length()));
        out.add("");

        if (data.summary().bodyCount() == 0) {
            out.add(I18n.get("report.word.empty"));
            return out;
        }

        List<Column> columns = ReportTables.compactBodyColumns();
        List<String> headers = columns.stream().map(c -> I18n.get(c.labelKey())).toList();
        List<TextTable.Align> aligns = columns.stream().map(c -> alignmentOf(c.kind())).toList();

        out.add(I18n.get("report.word.h.bodies"));
        out.addAll(TextTable.render(headers, ReportTables.bodyRows(data, columns, locale), aligns, MAX_COLUMN_WIDTH));
        out.add("");

        out.add(I18n.get("report.word.h.summary"));
        out.addAll(TextTable.render(
                List.of(I18n.get("report.sum.indicator"), I18n.get("report.sum.value")),
                ReportTables.summaryRows(data, locale),
                List.of(TextTable.Align.LEFT, TextTable.Align.RIGHT), 48));
        out.add("");

        if (!data.classes().isEmpty()) {
            out.add(I18n.get("report.classes.title"));
            out.addAll(TextTable.render(
                    List.of(I18n.get("report.classes.class"), I18n.get("report.classes.count"),
                            I18n.get("report.classes.mass"), I18n.get("report.classes.speed")),
                    ReportTables.classRows(data, locale),
                    List.of(TextTable.Align.LEFT, TextTable.Align.RIGHT, TextTable.Align.RIGHT, TextTable.Align.RIGHT),
                    MAX_COLUMN_WIDTH + 8));
        }
        return out;
    }

    private static TextTable.Align alignmentOf(Kind kind) {
        return (kind == Kind.TEXT || kind == Kind.COLOR) ? TextTable.Align.LEFT : TextTable.Align.RIGHT;
    }
}