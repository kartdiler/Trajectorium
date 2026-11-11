package trajectorium.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trajectorium.Simulation;
import trajectorium.i18n.I18n;
import trajectorium.report.*;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.function.Supplier;

public class ReportFileController {
    private static final Logger log = LoggerFactory.getLogger(ReportFileController.class);

    private final Simulation sim;
    private final Supplier<byte[]> sceneImage;
    private File lastDirectory;

    public ReportFileController(Simulation sim, Supplier<byte[]> sceneImage) {
        this.sim = sim;
        this.sceneImage = sceneImage;
    }

    public void exportExcel(Window owner) {
        ReportData data = ReportBuilder.build(sim, LocalDateTime.now());
        export(owner, data, new ExcelReportRenderer(), "report.dialog.excel", "report.filter.excel");
    }

    public void exportWord(Window owner) {
        ReportData data = ReportBuilder.build(sim, LocalDateTime.now());
        byte[] image = sceneImage.get();
        export(owner, data, new WordReportRenderer(image), "report.dialog.word", "report.filter.word");
    }

    private void export(Window owner, ReportData data, ReportRenderer renderer, String titleKey, String filterKey) {
        boolean wasRunning = sim.isRunning();
        if (wasRunning) {
            sim.pause();
        }

        try {
            File file = choose(owner, data, renderer, I18n.get(titleKey), I18n.get(filterKey));
            if (file != null) {
                write(owner, data, renderer, file);
            }
        } finally {
            if (wasRunning) {
                sim.play();
            }
        }
    }

    private File choose(Window owner, ReportData data, ReportRenderer renderer, String title, String filterText) {
        String extension = renderer.extension();

        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filterText, "*." + extension));
        chooser.setInitialFileName(renderer.defaultFileName(data.generatedAt()));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }

        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return null;
        }
        if (!file.getName().toLowerCase().endsWith("." + extension)) {
            file = new File(file.getParentFile(), file.getName() + "." + extension);
        }
        lastDirectory = file.getParentFile();
        return file;
    }

    private void write(Window owner, ReportData data, ReportRenderer renderer, File file) {
        long started = System.nanoTime();

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            renderer.render(data, buffer);
            Files.write(file.toPath(), buffer.toByteArray());
        } catch (IOException | RuntimeException e) {
            log.error("Не удалось сохранить отчет {}", file, e);
            showError(owner, e);
            return;
        }

        long millis = (System.nanoTime() - started) / 1_000_000;
        log.info("Отчет сохранен: {} ({} тел, {} КБ, {} мс", file, data.summary().bodyCount(), file.length() / 1024, millis);
        showDone(owner, file);
    }

    private void showDone(Window owner, File file) {
        ButtonType open = new ButtonType(I18n.get("report.done.open"), ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType(I18n.get("report.done.close"), ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", open, close);
        alert.initOwner(owner);
        alert.setTitle("Trajectorium");
        alert.setHeaderText(I18n.get("report.done.header"));
        alert.getDialogPane().setContent(wrapped(file.getAbsolutePath()));
        alert.showAndWait().filter(button -> button == open).ifPresent(button -> openFile(file));
    }

    private void showError(Window owner, Exception e) {
        String message = e.getMessage() == null ? e.toString() : e.getMessage();

        Alert alert = new Alert(Alert.AlertType.ERROR, "",
                new ButtonType(I18n.get("dialog.ok"), ButtonBar.ButtonData.OK_DONE));
        alert.initOwner(owner);
        alert.setTitle("Trajectorium");
        alert.setHeaderText(I18n.get("report.error.write"));
        alert.getDialogPane().setContent(wrapped(message + "\n\n" + I18n.get("report.error.hint")));
        alert.showAndWait();
    }

    private static Label wrapped(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(460);
        return label;
    }

    private void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Не удалось открыть файл {}", file, e);
        }
    }
}
