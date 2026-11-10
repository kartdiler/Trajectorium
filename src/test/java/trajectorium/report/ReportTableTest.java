package trajectorium.report;

import org.junit.jupiter.api.Test;
import trajectorium.report.ReportTables.Kind;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReportTableTest {
    private static final Locale RU = Locale.forLanguageTag("ru");

    @Test
    void numbersFollowTheLocale() {
        assertEquals("1,235", ReportTables.format(Kind.DECIMAL, 1.23456, RU));
        assertEquals("1.235", ReportTables.format(Kind.DECIMAL, 1.23456, Locale.ENGLISH));
        assertEquals("3,003e-06", ReportTables.format(Kind.SCIENTIFIC, 3.003e-6, RU));
        assertEquals("50,0%", ReportTables.format(Kind.PERCENT, 0.5, RU));
        assertEquals("3", ReportTables.format(Kind.INTEGER, 3, RU));
    }

    @Test
    void dateIsLocalizedAndNullIsEmpty() {
        String text = ReportTables.format(Kind.DATETIME, LocalDateTime.of(2026, 11, 8, 12, 30), RU);
        assertTrue(text.contains("2026"), text);
        assertEquals("", ReportTables.format(Kind.TEXT, null, RU));
    }

    @Test
    void columnsAndSummaryAreDescribedOnceForAllFormats() {
        assertEquals(11, ReportTables.bodyColumns().size());
        ReportData empty = new ReportData(LocalDateTime.of(2026, 11, 8, 12, 30), "Verlet", 0,
                List.of(), new ReportData.Summary(0, 0, null, 0, 0, 0, 0, 0, 0, 0,
                0,null), List.of());
        assertEquals(16, ReportTables.summaryItems(empty).size());
    }

    @Test
    void compactColumnsOmitVelocityComponentsAndRadius() {
        assertEquals(8, ReportTables.compactBodyColumns().size());
        assertTrue(ReportTables.compactBodyColumns().stream()
                .noneMatch(c -> c.labelKey().equals("report.col.vx") || c.labelKey().equals("report.col.radius")));
    }
}
