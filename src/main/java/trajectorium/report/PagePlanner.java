package trajectorium.report;

import trajectorium.Check;

import java.util.ArrayList;
import java.util.List;

public final class PagePlanner {
    private PagePlanner() {}

    public static final double MIN_FONT = 6;
    public static final double MAX_FONT = 11;
    private static final int FOOTER_LINES = 2;

    public record Plan(double fontSize, double lineHeight, int linesPerPage, List<List<String>> pages) {}

    /**
     * @param charWidthPerPt  ширина символа моноширинного шрифта на 1 пункт размера
     * @param lineHeightPerPt высота строки на 1 пункт размера
     */
    public static Plan plan(List<String> lines, double printableWidth, double printableHeight,
                            double charWidthPerPt, double lineHeightPerPt) {
        Check.positive(printableWidth, "ширина области печати");
        Check.positive(printableHeight, "высота области печати");
        Check.positive(charWidthPerPt, "ширина символа");
        Check.positive(lineHeightPerPt, "высота строки");
        Check.notNull(lines, "строки отчёта");

        int longest = lines.stream().mapToInt(String::length).max().orElse(1);
        double fit = printableWidth / (Math.max(1, longest) * charWidthPerPt);
        double font = Math.floor(Math.min(MAX_FONT, Math.max(MIN_FONT, fit)) * 2) / 2;

        double lineHeight = font * lineHeightPerPt;
        int perPage = Math.max(1, (int) Math.floor(printableHeight / lineHeight) - FOOTER_LINES);

        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += perPage) {
            pages.add(List.copyOf(lines.subList(i, Math.min(lines.size(), i + perPage))));
        }
        if (pages.isEmpty()) pages.add(List.of());
        return new Plan(font, lineHeight, perPage, List.copyOf(pages));
    }
}