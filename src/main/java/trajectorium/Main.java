package trajectorium;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import trajectorium.command.*;
import trajectorium.ui.BodyInfoPanel;
import trajectorium.ui.ControlPanel;
import trajectorium.ui.NewBodyDialog;

public class Main extends Application {
    public static void main() {
        launch();
    }

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    AppConfig config = AppConfig.load();
    Simulation simulation = new Simulation(config.getDt());
    CommandHistory history = new CommandHistory(50);

    double canvasWidth = config.getCanvasWidth();
    double canvasHeight = config.getCanvasHeight();
    double scale = config.getScale();

    CelestialFactory factory = new CelestialFactory(simulation, scale);
    int bodyCounter = 0;

    BodyInfoPanel infoPanel;

    Celestial trackedBody = null;
    double currentOrigX = canvasWidth / 2;
    double currentOrigY = canvasHeight / 2;

    @Override
    public void start(Stage stage) {
        simulation.setSimulationSpeed(config.getSimulationSpeed());
        factory.setTrailMaxPoints(config.getTrailMaxPoints());
        log.info("Запуск Trajectorium: окно {}x{}, масштаб {}, шаг {} года", canvasWidth, canvasHeight,
                scale, simulation.getDt());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: black");

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        ControlPanel panel = new ControlPanel(simulation, history, this::buildDefaultScene);
        infoPanel = new BodyInfoPanel(simulation, history);

        VBox sideBar = new VBox(panel, infoPanel);
        sideBar.setStyle("-fx-background-color: #222222");

        root.setCenter(canvas);
        root.setRight(sideBar);

        simulation.addListener(new SimulationListener() {
            @Override
            public void onBodyRemoved(Celestial body) {
                if (body == trackedBody) {
                    log.info("Отслеживаемое тело {} удалено, камера возвращена в центр", body.getName());
                    select(null);
                }
            }

            @Override
            public void onReset() {
                select(null);
            }
        });
        simulation.addListener(new EnergyMonitor(1000));

        buildDefaultScene();

        canvas.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                Celestial clicked = findBodyAt(event.getX(), event.getY(), currentOrigX, currentOrigY);
                select(clicked);
            } else if (event.getButton() == MouseButton.SECONDARY) {
                addBodyAt(event.getX(), event.getY(), canvas.getScene().getWindow());
            }
        });

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                simulation.step();

                double origX, origY;
                if(trackedBody != null) {
                    origX = canvasWidth / 2 - trackedBody.getPos()[0] * scale;
                    origY = canvasHeight / 2 - trackedBody.getPos()[1] * scale;
                } else {
                    origX = canvasWidth / 2;
                    origY = canvasHeight / 2;
                }

                currentOrigX = origX;
                currentOrigY = origY;

                for (Celestial cel : simulation.getBodies()) {
                    var points = cel.getTrail().getPoints();
                    int total = points.size();
                    int i = 0;

                    gc.setFill(cel.getColor());
                    for (double[] point : points) {
                        double sx = origX + point[0] * scale;
                        double sy = origY + point[1] * scale;

                        double opacity = total > 1 ? (double) i / (total - 1) : 1.0;
                        gc.setGlobalAlpha(opacity * 0.6);
                        gc.fillOval(sx - 1, sy - 1, 2, 2);
                        i++;
                    }

                    gc.setGlobalAlpha(1.0);
                }

                for (Celestial cel : simulation.getBodies()) {
                    gc.setFill(cel.getColor());
                    double x = cel.screenX(origX, scale) - cel.getCanvasRadius();
                    double y = cel.screenY(origY, scale) - cel.getCanvasRadius();
                    gc.fillOval(x, y, cel.getCanvasRadius() * 2, cel.getCanvasRadius() * 2);
                }

                if (trackedBody != null) {
                    double cx = trackedBody.screenX(origX, scale);
                    double cy = trackedBody.screenY(origY, scale);
                    double r = trackedBody.getCanvasRadius() + 4;
                    gc.setStroke(Color.WHITE);
                    gc.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                }
            }
        };

        timer.start();

        Scene scene = new Scene(root, Color.BLACK);
        stage.setScene(scene);
        stage.setTitle("Trajectorium");
        stage.show();

        stage.setOnCloseRequest(e -> log.info("Приложение закрыто"));

        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                history.undo();
            } else if (e.isControlDown() && e.getCode() == KeyCode.Y) {
                history.redo();
            } else if (e.getCode() == KeyCode.SPACE) {
                history.execute(new SetRunningCommand(simulation, !simulation.isRunning()));
            }
        });
    }

    Celestial findBodyAt(double scrX, double scrY, double origX, double origY) {
        for (Celestial cel : simulation.getBodies()) {
            double cx = cel.screenX(origX, scale);
            double cy = cel.screenY(origY, scale);

            double dx = scrX - cx;
            double dy = scrY - cy;

            double dist = Math.sqrt(dx * dx + dy * dy);
            double clickRadius = Math.max(cel.getCanvasRadius(), 8);
            if (dist <= clickRadius) {
                return cel;
            }
        }
        return null;
    }

    private void select(Celestial body) {
        if (body != trackedBody) {
            log.debug("Камера: {}", body == null ? "обзор сцены" : "слежение за " + body.getName());
        }
        trackedBody = body;
        if (infoPanel != null) {
            infoPanel.setBody(body);
        }
    }

    private void addBodyAt(double scrX, double scrY, Window owner) {
        double x = (scrX - currentOrigX) / scale;
        double y = (scrY - currentOrigY) / scale;

        boolean isRunning = simulation.isRunning();
        if (isRunning) {
            simulation.pause();
        }

        try {
            NewBodyDialog.show(owner, x, y, "Тело " + (bodyCounter + 1), speedHint(x, y))
                    .ifPresent(d -> {
                        history.execute(new AddBodyCommand(simulation, () ->
                                factory.createFree(d.name(), d.mass(), d.unit(), x, y,
                                        d.vx(), d.vy(), d.radius(), d.color())));
                        bodyCounter++;
                    });
        } finally {
            if (isRunning) {
                simulation.play();
            }
        }
    }

    private String speedHint(double x, double y) {
        Celestial heaviest = null;
        for (Celestial c : simulation.getBodies()) {
            if (heaviest == null || c.getMass() > heaviest.getMass()) {
                heaviest = c;
            }
        }
        if (heaviest == null) {
            return "";
        }

        double[] p = heaviest.getPos();
        double r = Math.hypot(x - p[0], y - p[1]);
        return String.format("Круговая скорость вокруг %s: %.3f а.е./год",
                heaviest.getName(), CelestialFactory.circularVelocity(heaviest.getMass(), r));
    }

    private void buildDefaultScene() {
        Celestial Sun = factory.createStar("Sun", 1, 0, 0, 20, Color.YELLOW);
        Celestial Mercury = factory.createOrbiting("Mercury", 1.66e-7, Sun, 0.39, 1,
                Color.GRAY, true);
        Celestial Venus = factory.createOrbiting("Venus", 2.45e-6, Sun, 0.72, 1.7,
                Color.LIGHTYELLOW, false);
        Celestial Earth = factory.createOrbiting("Earth", 3.003e-6, Sun, 1, 1.8,
                Color.BLUE, true);
        Celestial Mars = factory.createOrbiting("Mars", 3.23e-7, Sun, 1.52, 1.4,
                Color.INDIANRED, true);
        Celestial Jupiter = factory.createOrbiting("Jupiter", 9.55e-4, Sun, 5.2, 3,
                Color.ORANGERED, true);
        Celestial Moon = factory.createOrbiting("Moon", 3.69e-8, Jupiter, 0.11257, 0.5,
                Color.WHITE, true);
        Celestial Saturn = factory.createOrbiting("Saturn", 2.86e-4, Sun, 9.54, 2.6,
                Color.NAVAJOWHITE, false);
        Celestial Asteroid = factory.createFree("Asteroid", 1e-19, CelestialFactory.MassUnit.SOLAR_MASSES,
                10, -1, -1, 0.7, 0.1, Color.GRAY);
    }
}