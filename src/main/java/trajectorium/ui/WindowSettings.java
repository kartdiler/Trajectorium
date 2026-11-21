package trajectorium.ui;

import javafx.stage.Screen;
import javafx.stage.Stage;
import trajectorium.io.WindowState;

public final class WindowSettings {
    private WindowSettings() {}

    public static WindowState capture(Stage stage) {
        return new WindowState(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight(), stage.isMaximized());
    }

    public static void apply(Stage stage, WindowState state) {
        if (state.maximized()) {
            stage.setMaximized(true);
            return;
        }
        stage.setWidth(state.width());
        stage.setHeight(state.height());
        if (!Screen.getScreensForRectangle(state.x(), state.y(), state.width(), state.height()).isEmpty()) {
            stage.setX(state.x());
            stage.setY(state.y());
        }
    }
}
