package trajectorium.report;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trajectorium.Simulation;
import trajectorium.i18n.I18n;
import trajectorium.report.ReportTables.Column;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WordReportRendererTest {

    @BeforeEach
    void useRussian() {
        I18n.setLocale(I18n.RU);
    }

    @AfterEach
    void restoreRussian() {
        I18n.setLocale(I18n.RU);
    }

    private static XWPFDocument render(ReportData data, byte[] image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new WordReportRenderer(image).render(data, out);
        return new XWPFDocument(new ByteArrayInputStream(out.toByteArray()));
    }

    private static List<String> paragraphs(XWPFDocument doc) {
        return doc.getParagraphs().stream().map(XWPFParagraph::getText).toList();
    }

    @Test
    void documentStartsWithTitleAndDescriptionParagraph() throws IOException {
        try (XWPFDocument doc = render(ReportFixtures.sampleReport(), null)) {
            List<String> texts = paragraphs(doc);

            assertEquals(I18n.get("report.title"), texts.get(0));
            String intro = texts.get(1);
            assertTrue(intro.contains("2026"), intro);
            assertTrue(intro.contains("3 тел"), intro);
            assertTrue(intro.contains("Verlet"), intro);
            assertTrue(intro.contains("Sun"), intro);
        }
    }

    @Test
    void bodiesTableHasShadedBoldHeaderAndDataRows() throws IOException {
        try (XWPFDocument doc = render(ReportFixtures.sampleReport(), null)) {
            assertEquals(3, doc.getTables().size());

            XWPFTable bodies = doc.getTables().get(0);
            List<Column> columns = ReportTables.compactBodyColumns();
            assertEquals(4, bodies.getRows().size());
            assertEquals(columns.size(), bodies.getRow(0).getTableCells().size());
            for (int i = 0; i < columns.size(); i++) {
                assertEquals(I18n.get(columns.get(i).labelKey()), bodies.getRow(0).getCell(i).getText());
            }

            XWPFTableCell header = bodies.getRow(0).getCell(0);
            assertEquals("D9D9D9", header.getColor());
            assertTrue(header.getParagraphs().get(0).getRuns().get(0).isBold());

            assertEquals("Earth", bodies.getRow(2).getCell(0).getText());
            assertEquals("3,003e-06", bodies.getRow(2).getCell(2).getText());
        }
    }

    @Test
    void colorCellIsShadedWithTheBodyColor() throws IOException {
        try (XWPFDocument doc = render(ReportFixtures.sampleReport(), null)) {
            XWPFTableCell color = doc.getTables().get(0).getRow(2).getCell(7);

            assertEquals("#0000FF", color.getText());
            assertEquals("0000FF", color.getColor());
        }
    }

    @Test
    void summaryRowsAreTheSameInWordAndExcel() throws IOException {
        ReportData data = ReportFixtures.sampleReport();
        List<String> expected = ReportTables.summaryItems(data).stream()
                .map(item -> I18n.get(item.labelKey())).toList();

        List<String> inWord = new ArrayList<>();
        try (XWPFDocument doc = render(data, null)) {
            XWPFTable summary = doc.getTables().get(1);
            for (int r = 1; r < summary.getRows().size(); r++) {
                inWord.add(summary.getRow(r).getCell(0).getText());
            }
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ExcelReportRenderer().render(data, bytes);
        List<String> inExcel = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes.toByteArray()))) {
            XSSFSheet sheet = workbook.getSheet(I18n.get("report.sheet.summary"));
            for (int r = 3; r < 3 + expected.size(); r++) {
                inExcel.add(sheet.getRow(r).getCell(0).getStringCellValue());
            }
        }

        assertEquals(expected, inWord);
        assertEquals(expected, inExcel);
    }

    @Test
    void pictureIsEmbeddedOnlyWhenProvided() throws IOException {
        try (XWPFDocument with = render(ReportFixtures.sampleReport(), ReportFixtures.pngImage())) {
            assertEquals(1, with.getAllPictures().size());
        }
        try (XWPFDocument without = render(ReportFixtures.sampleReport(), null)) {
            assertTrue(without.getAllPictures().isEmpty());
        }
    }

    @Test
    void corruptImageIsRejectedWithAnIoException() {
        WordReportRenderer renderer = new WordReportRenderer(new byte[]{1, 2, 3});
        assertThrows(IOException.class,
                () -> renderer.render(ReportFixtures.sampleReport(), new ByteArrayOutputStream()));
    }

    @Test
    void emptySceneGivesTitleAndMessageWithoutTables() throws IOException {
        ReportData empty = ReportBuilder.build(new Simulation(ReportFixtures.DT), ReportFixtures.NOW);
        try (XWPFDocument doc = render(empty, null)) {
            assertTrue(doc.getTables().isEmpty());
            assertTrue(paragraphs(doc).contains(I18n.get("report.word.empty")));
        }
    }

    @Test
    void textsFollowTheLanguage() throws IOException {
        I18n.setLocale(I18n.EN);
        try (XWPFDocument doc = render(ReportFixtures.sampleReport(), null)) {
            assertEquals("Trajectorium simulation report", paragraphs(doc).get(0));
            assertEquals("Name", doc.getTables().get(0).getRow(0).getCell(0).getText());
            assertTrue(paragraphs(doc).get(1).contains("3 bodies"));
        }
    }

    @Test
    void documentPropertiesAreFilledIn() throws IOException {
        try (XWPFDocument doc = render(ReportFixtures.sampleReport(), null)) {
            assertEquals(I18n.get("report.title"), doc.getProperties().getCoreProperties().getTitle());
            assertEquals("Trajectorium", doc.getProperties().getCoreProperties().getCreator());
        }
    }

    @Test
    void fileNameHasTimestampAndExtension() {
        assertEquals("trajectorium-report-20261108-1230.docx",
                new WordReportRenderer().defaultFileName(ReportFixtures.NOW));
    }

    @Test
    void writesDemoFileForManualInspection() throws IOException {
        Path file = Path.of("target", "demo-report.docx");
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            new WordReportRenderer(ReportFixtures.pngImage()).render(ReportFixtures.sampleReport(), out);
        }
        assertTrue(Files.size(file) > 0);
    }
}
