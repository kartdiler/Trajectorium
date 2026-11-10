package trajectorium.report;

import trajectorium.i18n.I18n;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public final class ReportTables {
    private ReportTables() {}

    public enum Kind { TEXT, INTEGER, DECIMAL, SCIENTIFIC, PERCENT, DATETIME, COLOR }

    public record Column(String labelKey, Kind kind, int width, Function<ReportData.BodyRow, Object> value) {}

    public record Item(String labelKey, Kind kind, Object value) {}

    private static final String DASH = "—";
    private static final Set<String> WIDE_ONLY = Set.of("report.col.vx", "report.col.vy", "report.col.radius");

    public static List<Column> bodyColumns() {
        return List.of(
                new Column("report.col.name", Kind.TEXT, 16, ReportData.BodyRow::name),
                new Column("report.col.class", Kind.TEXT, 24, b -> I18n.get(b.massClass().labelKey())),
                new Column("report.col.mass", Kind.SCIENTIFIC, 14, ReportData.BodyRow::mass),
                new Column("report.col.x", Kind.DECIMAL, 11, ReportData.BodyRow::x),
                new Column("report.col.y", Kind.DECIMAL, 11, ReportData.BodyRow::y),
                new Column("report.col.vx", Kind.DECIMAL, 13, ReportData.BodyRow::vx),
                new Column("report.col.vy", Kind.DECIMAL, 13, ReportData.BodyRow::vy),
                new Column("report.col.speed", Kind.DECIMAL, 14, ReportData.BodyRow::speed),
                new Column("report.col.speedkms", Kind.DECIMAL, 14, ReportData.BodyRow::speedKmS),
                new Column("report.col.radius", Kind.DECIMAL, 11, ReportData.BodyRow::radiusPx),
                new Column("report.col.color", Kind.COLOR, 11, ReportData.BodyRow::color));
    }

    public static List<Column> compactBodyColumns() {
        return bodyColumns().stream().filter(c -> !WIDE_ONLY.contains(c.labelKey())).toList();
    }

    public static List<Item> summaryItems(ReportData d) {
        ReportData.Summary s = d.summary();
        ReportData.ClosestPair pair = s.closestPair();
        return List.of(
                new Item("report.sum.generated", Kind.DATETIME, d.generatedAt()),
                new Item("report.sum.integrator", Kind.TEXT, d.integrator()),
                new Item("report.sum.time", Kind.DECIMAL, d.timeYears() * 365.25),
                new Item("report.sum.count", Kind.INTEGER, s.bodyCount()),
                new Item("report.sum.totalmass", Kind.SCIENTIFIC, s.totalMass()),
                new Item("report.sum.heaviest", Kind.TEXT, s.heaviestName() == null ? DASH : s.heaviestName()),
                new Item("report.sum.share", Kind.PERCENT, s.heaviestShare()),
                new Item("report.sum.minspeed", Kind.DECIMAL, s.minSpeed()),
                new Item("report.sum.avgspeed", Kind.DECIMAL, s.averageSpeed()),
                new Item("report.sum.maxspeed", Kind.DECIMAL, s.maxSpeed()),
                new Item("report.sum.comx", Kind.SCIENTIFIC, s.centerOfMassX()),
                new Item("report.sum.comy", Kind.SCIENTIFIC, s.centerOfMassY()),
                new Item("report.sum.energy", Kind.SCIENTIFIC, s.totalEnergy()),
                new Item("report.sum.angular", Kind.SCIENTIFIC, s.angularMomentum()),
                new Item("report.sum.closest", Kind.TEXT,
                        pair == null ? DASH : pair.first() + " — " + pair.second()),
                new Item("report.sum.closestdist", pair == null ? Kind.TEXT : Kind.DECIMAL,
                        pair == null ? DASH : pair.distance()));
    }

    public static String format(Kind kind, Object value, Locale locale) {
        if (value == null) return "";
        return switch (kind) {
            case TEXT, COLOR -> value.toString();
            case INTEGER -> String.format(locale, "%d", ((Number) value).longValue());
            case DECIMAL -> String.format(locale, "%.3f", ((Number) value).doubleValue());
            case SCIENTIFIC -> String.format(locale, "%.3e", ((Number) value).doubleValue());
            case PERCENT -> String.format(locale, "%.1f%%", ((Number) value).doubleValue() * 100);
            case DATETIME -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(locale).format((LocalDateTime) value);
        };
    }
}