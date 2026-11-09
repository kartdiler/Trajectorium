package trajectorium.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import trajectorium.i18n.I18n;
import trajectorium.report.ReportTables.Column;
import trajectorium.report.ReportTables.Item;
import trajectorium.report.ReportTables.Kind;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelReportRenderer implements ReportRenderer {
    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public void render(ReportData data, OutputStream out) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            writeBodies(workbook, styles, data);
            writeSummary(workbook, styles, data);
            workbook.write(out);
        }
    }

    private void writeBodies(XSSFWorkbook workbook, Styles styles, ReportData data) {
        Sheet sheet = workbook.createSheet(I18n.get("report.sheet.bodies"));
        List<Column> columns = ReportTables.bodyColumns();

        Row header = sheet.createRow(0);
        header.setHeightInPoints(32);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(I18n.get(columns.get(i).labelKey()));
            cell.setCellStyle(styles.header);
            sheet.setColumnWidth(i, columns.get(i).width() * 256);
        }

        int rowIndex = 1;
        for (ReportData.BodyRow body : data.bodies()) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < columns.size(); i++) {
                put(row, i, columns.get(i).kind(), columns.get(i).value().apply(body), styles);
            }
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rowIndex - 1), 0, columns.size() - 1));
    }

    private void writeSummary(XSSFWorkbook workbook, Styles styles, ReportData data) {
        Sheet sheet = workbook.createSheet(I18n.get("report.sheet.summary"));
        sheet.setColumnWidth(0, 40 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 22 * 256);
        sheet.setColumnWidth(3, 26 * 256);

        int r = 0;
        Cell title = sheet.createRow(r++).createCell(0);
        title.setCellValue(I18n.get("report.title"));
        title.setCellStyle(styles.title);
        r++;

        r = headerRow(sheet, r, styles, "report.sum.indicator", "report.sum.value");
        for (Item item : ReportTables.summaryItems(data)) {
            Row row = sheet.createRow(r++);
            put(row, 0, Kind.TEXT, I18n.get(item.labelKey()), styles);
            put(row, 1, item.kind(), item.value(), styles);
        }
        r++;

        Cell classesTitle = sheet.createRow(r++).createCell(0);
        classesTitle.setCellValue(I18n.get("report.classes.title"));
        classesTitle.setCellStyle(styles.title);
        r = headerRow(sheet, r, styles, "report.classes.class", "report.classes.count",
                "report.classes.mass", "report.classes.speed");
        for (ReportData.ClassStat c : data.classes()) {
            Row row = sheet.createRow(r++);
            put(row, 0, Kind.TEXT, I18n.get(c.massClass().labelKey()), styles);
            put(row, 1, Kind.INTEGER, c.count(), styles);
            put(row, 2, Kind.SCIENTIFIC, c.totalMass(), styles);
            put(row, 3, Kind.DECIMAL, c.averageSpeed(), styles);
        }
    }

    private static int headerRow(Sheet sheet, int r, Styles styles, String... keys) {
        Row row = sheet.createRow(r);
        for (int i = 0; i < keys.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(I18n.get(keys[i]));
            cell.setCellStyle(styles.header);
        }
        return r + 1;
    }

    private static void put(Row row, int col, Kind kind, Object value, Styles styles) {
        Cell cell = row.createCell(col);
        switch (kind) {
            case TEXT -> {
                cell.setCellValue(String.valueOf(value));
                cell.setCellStyle(styles.text);
            }
            case COLOR -> {
                String hex = String.valueOf(value);
                cell.setCellValue(hex);
                cell.setCellStyle(styles.color(hex));
            }
            case DATETIME -> {
                cell.setCellValue((LocalDateTime) value);
                cell.setCellStyle(styles.date);
            }
            default -> {
                cell.setCellValue(((Number) value).doubleValue());
                cell.setCellStyle(styles.numeric(kind));
            }
        }
    }

    private static final class Styles {
        private final XSSFWorkbook workbook;
        final CellStyle header;
        final CellStyle text;
        final CellStyle title;
        final CellStyle date;
        private final CellStyle integer;
        private final CellStyle decimal;
        private final CellStyle scientific;
        private final CellStyle percent;
        private final Map<String, CellStyle> colors = new HashMap<>();

        Styles(XSSFWorkbook workbook) {
            this.workbook = workbook;

            Font bold = workbook.createFont();
            bold.setBold(true);
            header = bordered();
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setWrapText(true);

            text = bordered();
            integer = number("0");
            decimal = number("0.000");
            scientific = number("0.000E+00");
            percent = number("0.0%");

            date = bordered();
            date.setDataFormat(workbook.createDataFormat().getFormat("m/d/yy h:mm"));
            date.setAlignment(HorizontalAlignment.LEFT);

            Font big = workbook.createFont();
            big.setBold(true);
            big.setFontHeightInPoints((short) 14);
            title = workbook.createCellStyle();
            title.setFont(big);
        }

        CellStyle numeric(Kind kind) {
            return switch (kind) {
                case INTEGER -> integer;
                case SCIENTIFIC -> scientific;
                case PERCENT -> percent;
                default -> decimal;
            };
        }

        CellStyle color(String hex) {
            return colors.computeIfAbsent(hex, h -> {
                int rgb = Integer.parseInt(h.substring(1), 16);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                XSSFCellStyle style = (XSSFCellStyle) bordered();
                style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) red, (byte) green, (byte) blue}, null));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                Font font = workbook.createFont();
                double luminance = 0.299 * red + 0.587 * green + 0.114 * blue;
                font.setColor(luminance < 128 ? IndexedColors.WHITE.getIndex() : IndexedColors.BLACK.getIndex());
                style.setFont(font);
                return style;
            });
        }

        private CellStyle number(String pattern) {
            CellStyle style = bordered();
            style.setDataFormat(workbook.createDataFormat().getFormat(pattern));
            return style;
        }

        private CellStyle bordered() {
            CellStyle style = workbook.createCellStyle();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
