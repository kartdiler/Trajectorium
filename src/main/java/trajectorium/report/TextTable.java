package trajectorium.report;

import java.util.ArrayList;
import java.util.List;

public final class TextTable {
    private TextTable() {}

    public enum Align { LEFT, RIGHT }

    public static List<String> render(List<String> headers, List<List<String>> rows,
                                      List<Align> aligns, int maxColumnWidth) {
        int columns = headers.size();
        if (aligns.size() != columns) {
            throw new IllegalArgumentException("Выравниваний " + aligns.size() + ", колонок " + columns);
        }
        for (List<String> row : rows) {
            if (row.size() != columns) {
                throw new IllegalArgumentException("В строке " + row.size() + " ячеек, колонок " + columns);
            }
        }

        int[] widths = new int[columns];
        for (int c = 0; c < columns; c++) {
            widths[c] = fit(headers.get(c), maxColumnWidth).length();
            for (List<String> row : rows) widths[c] = Math.max(widths[c], fit(row.get(c), maxColumnWidth).length());
        }

        String border = border(widths);
        List<String> lines = new ArrayList<>();
        lines.add(border);
        lines.add(line(headers, widths, aligns, maxColumnWidth, true));
        lines.add(border);
        for (List<String> row : rows) lines.add(line(row, widths, aligns, maxColumnWidth, false));
        lines.add(border);
        return lines;
    }

    private static String fit(String text, int max) {
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "~";
    }

    private static String border(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) sb.append("-".repeat(w + 2)).append('+');
        return sb.toString();
    }

    private static String line(List<String> cells, int[] widths, List<Align> aligns, int max, boolean header) {
        StringBuilder sb = new StringBuilder("|");
        for (int c = 0; c < widths.length; c++) {
            String text = fit(cells.get(c), max);
            String pad = " ".repeat(widths[c] - text.length());
            boolean right = !header && aligns.get(c) == Align.RIGHT;
            sb.append(' ').append(right ? pad + text : text + pad).append(" |");
        }
        return sb.toString();
    }
}