package trajectorium.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trajectorium.i18n.I18n;
import trajectorium.text.LogAnalyzer;
import trajectorium.text.LogSummary;
import trajectorium.text.LogSummaryFormatter;

import java.io.File;
import java.io.IOException;

public class LogAnalysisController {
    private static final Logger log = LoggerFactory.getLogger(LogAnalysisController.class);

    private File lastDirectory = new File("logs");

    public void show(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("log.dialog.open"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.get("log.filter"), "*.log"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }

        File file = chooser.showOpenDialog(owner);
        if (file == null) return;
        lastDirectory = file.getParentFile();

        try {
            LogSummary summary = LogAnalyzer.analyze(file.toPath());
            log.info("Проанализирован журнал {}: {} записей", file, summary.entries());
            showSummary(owner, LogSummaryFormatter.format(summary, file.getName()));
        } catch (IOException e) {
            log.error("Не удалось прочитать журнал {}", file, e);
            showError(owner, e);
        }
    }

    private void showSummary(Window owner, String text) {
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setStyle("-fx-font-family: 'Consolas', monospace;");
        area.setPrefSize(640, 420);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "",
                new ButtonType(I18n.get("report.done.close"), ButtonBar.ButtonData.CANCEL_CLOSE));
        alert.initOwner(owner);
        alert.setTitle("Trajectorium");
        alert.setHeaderText(I18n.get("log.title"));
        alert.getDialogPane().setContent(area);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private void showError(Window owner, IOException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "",
                new ButtonType(I18n.get("dialog.ok"), ButtonBar.ButtonData.OK_DONE));
        alert.initOwner(owner);
        alert.setTitle("Trajectorium");
        alert.setHeaderText(I18n.get("log.error.read"));
        Label message = new Label(e.getMessage() == null ? e.toString() : e.getMessage());
        message.setWrapText(true);
        message.setMaxWidth(460);
        alert.getDialogPane().setContent(message);
        alert.showAndWait();
    }
}