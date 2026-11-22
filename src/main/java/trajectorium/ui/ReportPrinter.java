package trajectorium.ui;

import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Window;
import trajectorium.i18n.I18n;
import trajectorium.report.PagePlanner;

import java.util.List;

public final class ReportPrinter {
    private ReportPrinter() {}

    public enum Status { DONE, CANCELLED, NO_PRINTER, FAILED }

    public record Result(Status status, int pages, String message) {}

    private static final String FONT = "Monospaced";

    public static Result print(Window owner, List<String> lines, String jobName) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return new Result(Status.NO_PRINTER, 0, null);

        job.getJobSettings().setJobName(jobName);
        if (!job.showPrintDialog(owner)) {
            job.cancelJob();
            return new Result(Status.CANCELLED, 0, null);
        }

        try {
            PageLayout layout = job.getJobSettings().getPageLayout();
            double width = layout.getPrintableWidth();
            double height = layout.getPrintableHeight();

            Font probeFont = Font.font(FONT, 10);
            Bounds latin = measure(probeFont, "MMMMMMMMMM");
            Bounds cyrillic = measure(probeFont, "ЖЖЖЖЖЖЖЖЖЖЖ");
            Bounds tall = measure(probeFont, "Мj,gйQ|");
            double charWidthPerPt = Math.max(latin.getWidth(), cyrillic.getWidth()) / 10 / 10;
            double lineHeightPerPt = Math.max(latin.getHeight(), Math.max(cyrillic.getHeight(), tall.getHeight())) / 10;

            PagePlanner.Plan plan = PagePlanner.plan(lines, width, height, charWidthPerPt, lineHeightPerPt);

            int total = plan.pages().size();
            for (int i = 0; i < total; i++) {
                if (!job.printPage(layout, pageNode(plan, i, total, width, height))) {
                    job.cancelJob();
                    return new Result(Status.FAILED, i, "printPage");
                }
            }
            return job.endJob() ? new Result(Status.DONE, total, null) : new Result(Status.FAILED, total, "endJob");
        } catch (RuntimeException e) {
            job.cancelJob();
            return new Result(Status.FAILED, 0, e.getMessage());
        }
    }

    private static Bounds measure(Font font, String sample) {
        Text text = new Text(sample);
        text.setFont(font);
        return text.getLayoutBounds();
    }

    private static Node pageNode(PagePlanner.Plan plan, int index, int total, double width, double height) {
        Group page = new Group();

        Rectangle bounds = new Rectangle(width, height);
        bounds.setFill(Color.TRANSPARENT);
        page.getChildren().add(bounds);

        Font font = Font.font(FONT, plan.fontSize());
        List<String> lines = plan.pages().get(index);
        for (int i = 0; i < lines.size(); i++) {
            page.getChildren().add(line(lines.get(i), font, i * plan.lineHeight()));
        }

        page.getChildren().add(line(I18n.format("print.page", index + 1, total), font,
                (plan.linesPerPage() + 1) * plan.lineHeight()));
        return page;
    }

    private static Text line(String content, Font font, double y) {
        Text text = new Text(content);
        text.setFont(font);
        text.setTextOrigin(VPos.TOP);
        text.setLayoutY(y);
        return text;
    }
}