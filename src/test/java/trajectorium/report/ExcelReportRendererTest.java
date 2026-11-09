package trajectorium.report;

import javafx.scene.paint.Color;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trajectorium.Celestial;
import trajectorium.CelestialFactory;
import trajectorium.Simulation;
import trajectorium.i18n.I18n;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelReportRendererTest {
    private static final double DT = 1.0 / 365.25;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 8, 12, 30);

    @BeforeEach
    void useRussian() {
        I18n.setLocale(I18n.RU);
    }

    @AfterEach
    void restoreRussian() {
        I18n.setLocale(I18n.RU);
    }

    private static ReportData sampleReport() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        Celestial sun = factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
        factory.createOrbiting("Earth", 3.003e-6, sun, 1.0, 5, Color.BLUE, true);
        factory.createOrbiting("Jupiter", 9.55e-4, sun, 5.2, 6, Color.ORANGERED, true);
        return ReportBuilder.build(sim, NOW);
    }

    private static ReportData syntheticReport(int n) {
        List<ReportData.BodyRow> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(new ReportData.BodyRow("B" + i, MassClass.SMALL, 1e-12, i, -i,
                    0.1, 0.2, 0.22, 1.05, 2, "#808080"));
        }
        ReportData.Summary summary = new ReportData.Summary(n, n * 1e-12, "B0", 1.0 / n,
                0.22, 0.22, 0.22, 0, 0, 0, 0, null);
        return new ReportData(NOW, "Verlet", 0, rows, summary, List.of());
    }

    private static XSSFWorkbook render(ReportData data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ExcelReportRenderer().render(data, out);
        return new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
    }

    private static XSSFCell valueOf(XSSFSheet sheet, String labelKey) {
        String label = I18n.get(labelKey);
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            XSSFRow row = sheet.getRow(r);
            if (row != null && row.getCell(0) != null
                    && row.getCell(0).getCellType() == CellType.STRING
                    && label.equals(row.getCell(0).getStringCellValue())) {
                return row.getCell(1);
            }
        }
        fail("Строка не найдена: " + label);
        return null;
    }

    @Test
    void bodiesSheetHasBoldBorderedHeaderAndFrozenRow() throws IOException {
        try (XSSFWorkbook workbook = render(sampleReport())) {
            XSSFSheet sheet = workbook.getSheet(I18n.get("report.sheet.bodies"));
            assertNotNull(sheet);

            XSSFCell header = sheet.getRow(0).getCell(0);
            assertEquals(I18n.get("report.col.name"), header.getStringCellValue());
            assertTrue(header.getCellStyle().getFont().getBold());
            assertEquals(BorderStyle.THIN, header.getCellStyle().getBorderBottom());

            assertTrue(sheet.getPaneInformation().isFreezePane());
            assertEquals(1, sheet.getPaneInformation().getHorizontalSplitPosition());
        }
    }

    @Test
    void bodyRowsHoldRealNumbersWithFormats() throws IOException {
        try (XSSFWorkbook workbook = render(sampleReport())) {
            XSSFSheet sheet = workbook.getSheet(I18n.get("report.sheet.bodies"));
            assertEquals(4, sheet.getPhysicalNumberOfRows());            // заголовок и три тела

            XSSFRow earth = sheet.getRow(2);
            assertEquals("Earth", earth.getCell(0).getStringCellValue());
            assertEquals(I18n.get("class.planet"), earth.getCell(1).getStringCellValue());
            assertEquals(CellType.NUMERIC, earth.getCell(2).getCellType());
            assertEquals(3.003e-6, earth.getCell(2).getNumericCellValue(), 1e-18);
            assertEquals("0.000E+00", earth.getCell(2).getCellStyle().getDataFormatString());
            assertEquals(29.78, earth.getCell(8).getNumericCellValue(), 0.05);   // км/с
        }
    }

    @Test
    void colorCellIsFilledWithTheBodyColor() throws IOException {
        try (XSSFWorkbook workbook = render(sampleReport())) {
            XSSFCell color = workbook.getSheet(I18n.get("report.sheet.bodies")).getRow(2).getCell(10);

            assertEquals("#0000FF", color.getStringCellValue());
            XSSFColor fill = color.getCellStyle().getFillForegroundXSSFColor();
            assertArrayEquals(new byte[]{0, 0, (byte) 255}, fill.getRGB());
        }
    }

    @Test
    void summarySheetHoldsAggregates() throws IOException {
        try (XSSFWorkbook workbook = render(sampleReport())) {
            XSSFSheet sheet = workbook.getSheet(I18n.get("report.sheet.summary"));
            assertNotNull(sheet);

            assertEquals(3, valueOf(sheet, "report.sum.count").getNumericCellValue(), 0.0);
            assertEquals("Sun", valueOf(sheet, "report.sum.heaviest").getStringCellValue());
            assertTrue(valueOf(sheet, "report.sum.share").getCellStyle().getDataFormatString().contains("%"));

            XSSFCell generated = valueOf(sheet, "report.sum.generated");
            assertTrue(DateUtil.isCellDateFormatted(generated));
            assertEquals(NOW, generated.getLocalDateTimeCellValue());
        }
    }

    @Test
    void summarySheetListsClassStatistics() throws IOException {
        try (XSSFWorkbook workbook = render(sampleReport())) {
            XSSFSheet sheet = workbook.getSheet(I18n.get("report.sheet.summary"));
            List<String> firstColumn = new ArrayList<>();
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                XSSFRow row = sheet.getRow(r);
                if (row != null && row.getCell(0) != null && row.getCell(0).getCellType() == CellType.STRING) {
                    firstColumn.add(row.getCell(0).getStringCellValue());
                }
            }
            assertTrue(firstColumn.contains(I18n.get("class.star")));
            assertTrue(firstColumn.contains(I18n.get("class.giant")));
            assertTrue(firstColumn.contains(I18n.get("class.planet")));
        }
    }

    @Test
    void labelsFollowTheLanguage() throws IOException {
        I18n.setLocale(I18n.EN);
        try (XSSFWorkbook workbook = render(sampleReport())) {
            assertNotNull(workbook.getSheet("Bodies"));
            assertNotNull(workbook.getSheet("Summary"));
            assertEquals("Name", workbook.getSheet("Bodies").getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void emptySceneGivesHeaderOnlyAndDashes() throws IOException {
        ReportData empty = ReportBuilder.build(new Simulation(DT), NOW);
        try (XSSFWorkbook workbook = render(empty)) {
            assertEquals(1, workbook.getSheet(I18n.get("report.sheet.bodies")).getPhysicalNumberOfRows());
            XSSFSheet summary = workbook.getSheet(I18n.get("report.sheet.summary"));
            assertEquals("—", valueOf(summary, "report.sum.heaviest").getStringCellValue());
            assertEquals(0, valueOf(summary, "report.sum.count").getNumericCellValue(), 0.0);
        }
    }

    @Test
    void cellStylesAreReusedAcrossRows() throws IOException {
        try (XSSFWorkbook workbook = render(syntheticReport(500))) {
            assertEquals(501, workbook.getSheet(I18n.get("report.sheet.bodies")).getPhysicalNumberOfRows());
            assertTrue(workbook.getNumCellStyles() < 30,
                    "стилей в книге: " + workbook.getNumCellStyles());
        }
    }

    @Test
    void fileNameHasTimestampAndExtension() {
        assertEquals("trajectorium-report-20261108-1230.xlsx",
                new ExcelReportRenderer().defaultFileName(NOW));
    }

    @Test
    void writesDemoFileForManualInspection() throws IOException {
        Path file = Path.of("target", "demo-report.xlsx");
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            new ExcelReportRenderer().render(sampleReport(), out);
        }
        assertTrue(Files.size(file) > 0);
    }
}