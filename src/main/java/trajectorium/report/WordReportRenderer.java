package trajectorium.report;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import trajectorium.i18n.I18n;
import trajectorium.report.ReportTables.Kind;
import trajectorium.report.ReportTables.Column;
import trajectorium.report.ReportTables.Item;

import java.math.BigInteger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WordReportRenderer implements ReportRenderer {
    private static final String HEADER_FILL = "D9D9D9";
    private static final String BORDER_COLOR = "808080";
    private static final double MAX_IMAGE_WIDTH = 450;

    private record Content(String text, ParagraphAlignment align, String fill) {}

    private final byte[] image;

    public WordReportRenderer() {
        this(null);
    }

    public WordReportRenderer(byte[] pngImage) {
        this.image = pngImage;
    }

    @Override
    public String extension() {
        return "docx";
    }

    @Override
    public void render(ReportData data, OutputStream out) throws IOException {
        Locale locale = I18n.getLocale();
        BufferedImage picture = decodeImage();

        try (XWPFDocument doc = new XWPFDocument()) {
            doc.getProperties().getCoreProperties().setTitle(I18n.get("report.title"));
            doc.getProperties().getCoreProperties().setCreator("Trajectorium");

            text(doc, I18n.get("report.title"), 20, true, false, ParagraphAlignment.CENTER);

            if (data.summary().bodyCount() == 0) {
                text(doc, I18n.get("report.word.empty"), 11, false, false, ParagraphAlignment.LEFT);
                doc.write(out);
                return;
            }

            text(doc, intro(data, locale), 11, false, false, ParagraphAlignment.BOTH);
            if (picture != null) {
                addPicture(doc, picture);
            }
            addBodiesTable(doc, data, locale);
            addSummaryTable(doc, data, locale);
            addClassesTable(doc, data, locale);

            doc.write(out);
        }
    }

    private BufferedImage decodeImage() throws IOException {
        if (image == null) {
            return null;
        }

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
        if (decoded == null) {
            throw new IOException(I18n.get("report.error.image"));
        }

        return decoded;
    }

    private static String intro(ReportData d, Locale locale) {
        ReportData.Summary s = d.summary();
        return I18n.format("report.word.intro",
                ReportTables.format(Kind.DATETIME, d.generatedAt(), locale),
                s.bodyCount(),
                ReportTables.format(Kind.DECIMAL, d.timeYears() * 365.25, locale),
                d.integrator(),
                s.heaviestName(),
                ReportTables.format(Kind.PERCENT, s.heaviestShare(), locale));
    }

    private void addPicture(XWPFDocument doc, BufferedImage picture) throws IOException {
        heading(doc, I18n.get("report.word.h.picture"));

        double scale = Math.min(1.0, MAX_IMAGE_WIDTH / picture.getWidth());
        int width = Units.toEMU(picture.getWidth() * scale);
        int height = Units.toEMU(picture.getHeight() * scale);

        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        try {
            paragraph.createRun().addPicture(new ByteArrayInputStream(image),
                    XWPFDocument.PICTURE_TYPE_PNG, "scene.png", width, height);
        } catch (InvalidFormatException e) {
            throw new IOException(I18n.get("report.error.image"), e);
        }
        caption(doc, I18n.get("report.word.caption.picture"));
    }

    private void addBodiesTable(XWPFDocument doc, ReportData data, Locale locale) {
        heading(doc, I18n.get("report.word.h.bodies"));
        List<Column> columns = ReportTables.compactBodyColumns();

        List<String> headers = new ArrayList<String>();
        for (Column c : columns) {
            headers.add(I18n.get(c.labelKey()));
        }

        List<List<Content>> rows = new ArrayList<>();
        for (ReportData.BodyRow body : data.bodies()) {
            List<Content> row = new ArrayList<>();
            for (Column c : columns) {
                String cellText = ReportTables.format(c.kind(), c.value().apply(body), locale);
                row.add(c.kind() == Kind.COLOR
                        ? new Content(cellText, ParagraphAlignment.CENTER, cellText.substring(1))
                        : new Content(cellText, alignmentOf(c.kind()), null));
            }
            rows.add(row);
        }
        table(doc, headers, rows, 8, 100);
        caption(doc, I18n.get("report.word.caption.bodies"));
    }

    private void addSummaryTable(XWPFDocument doc, ReportData data, Locale locale) {
        heading(doc, I18n.get("report.word.h.summary"));

        List<List<Content>> rows = new ArrayList<>();
        for (Item item : ReportTables.summaryItems(data)) {
            rows.add(List.of(
                    new Content(I18n.get(item.labelKey()), ParagraphAlignment.LEFT, null),
                    new Content(ReportTables.format(item.kind(), item.value(), locale), ParagraphAlignment.RIGHT, null)));
        }
        table(doc, List.of(I18n.get("report.sum.indicator"), I18n.get("report.sum.value")), rows, 10, 70);
        caption(doc, I18n.get("report.word.caption.summary"));
    }

    private void addClassesTable(XWPFDocument doc, ReportData data, Locale locale) {
        if (data.classes().isEmpty()) return;
        heading(doc, I18n.get("report.classes.title"));

        List<List<Content>> rows = new ArrayList<>();
        for (ReportData.ClassStat c : data.classes()) {
            rows.add(List.of(
                    new Content(I18n.get(c.massClass().labelKey()), ParagraphAlignment.LEFT, null),
                    new Content(ReportTables.format(Kind.INTEGER, c.count(), locale), ParagraphAlignment.RIGHT, null),
                    new Content(ReportTables.format(Kind.SCIENTIFIC, c.totalMass(), locale), ParagraphAlignment.RIGHT, null),
                    new Content(ReportTables.format(Kind.DECIMAL, c.averageSpeed(), locale), ParagraphAlignment.RIGHT, null)));
        }
        table(doc, List.of(I18n.get("report.classes.class"), I18n.get("report.classes.count"),
                I18n.get("report.classes.mass"), I18n.get("report.classes.speed")), rows, 10, 80);
        caption(doc, I18n.get("report.word.caption.classes"));
    }

    private static ParagraphAlignment alignmentOf(Kind kind) {
        return switch (kind) {
            case TEXT -> ParagraphAlignment.LEFT;
            case COLOR -> ParagraphAlignment.CENTER;
            default -> ParagraphAlignment.RIGHT;
        };
    }

    private static void table(XWPFDocument doc, List<String> headers, List<List<Content>> rows, int fontSize, int widthPercent) {
        XWPFTable table = doc.createTable(rows.size() + 1, headers.size());
        setWidthPercent(table, widthPercent);
        centerTable(table);
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDER_COLOR);
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDER_COLOR);
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDER_COLOR);
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDER_COLOR);
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDER_COLOR);
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDER_COLOR);

        XWPFTableRow headerRow = table.getRow(0);
        headerRow.setRepeatHeader(true);
        for (int c = 0; c < headers.size(); c++) {
            XWPFTableCell cell = headerRow.getCell(c);
            cell.setColor(HEADER_FILL);
            write(cell, headers.get(c), ParagraphAlignment.CENTER, true, fontSize, null);
        }

        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = table.getRow(r + 1);
            for (int c = 0; c < headers.size(); c++) {
                Content content = rows.get(r).get(c);
                XWPFTableCell cell = row.getCell(c);
                String textColor = null;
                if (content.fill() != null) {
                    cell.setColor(content.fill());
                    textColor = contrastColor(content.fill());
                }
                write(cell, content.text(), content.align(), false, fontSize, textColor);
            }
        }
    }

    private static CTTblPr properties(XWPFTable table) {
        CTTbl ctTable = table.getCTTbl();
        CTTblPr properties = ctTable.getTblPr();
        return properties != null ? properties : ctTable.addNewTblPr();
    }

    private static void setWidthPercent(XWPFTable table, int percent) {
        CTTblPr properties = properties(table);
        CTTblWidth width = properties.isSetTblW() ? properties.getTblW() : properties.addNewTblW();
        width.setType(STTblWidth.PCT);
        width.setW(BigInteger.valueOf(percent * 50L));
    }

    private static void centerTable(XWPFTable table) {
        CTTblPr properties = properties(table);
        CTJcTable alignment = properties.isSetJc() ? properties.getJc() : properties.addNewJc();
        alignment.setVal(STJcTable.CENTER);
    }

    private static void write(XWPFTableCell cell, String text, ParagraphAlignment align,
                              boolean bold, int fontSize, String color) {
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.setAlignment(align);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(fontSize);
        if (color != null) {
            run.setColor(color);
        }
    }

    private static String contrastColor(String rgb) {
        int value = Integer.parseInt(rgb, 16);
        double luminance = 0.299 * ((value >> 16) & 0xFF) + 0.587 * ((value >> 8) & 0xFF) + 0.114 * (value & 0xFF);
        return luminance < 128 ? "FFFFFF" : "000000";
    }

    private static void heading(XWPFDocument doc, String text) {
        XWPFParagraph paragraph = text(doc, text, 14, true, false, ParagraphAlignment.LEFT);
        paragraph.setSpacingBefore(240);
    }

    private static void caption(XWPFDocument doc, String text) {
        XWPFParagraph paragraph = text(doc, text, 9, false, true, ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(240);
    }

    private static XWPFParagraph text(XWPFDocument doc, String text, int size, boolean bold, boolean italic,
                                      ParagraphAlignment align) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setAlignment(align);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setBold(bold);
        run.setItalic(italic);
        return paragraph;
    }
}
