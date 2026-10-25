package trajectorium;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

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

    Celestial trackedBody = null;
    double currentOrigX = canvasWidth / 2;
    double currentOrigY = canvasHeight / 2;

    @Override
    public void start(Stage stage) {
        simulation.setSimulationSpeed(config.getSimulationSpeed());
        log.info("Запуск Trajectorium: окно {}x{}, масштаб {}, шаг {} года", canvasWidth, canvasHeight,
                scale, simulation.getDt());

        CelestialFactory factory = new CelestialFactory(simulation, scale);
        factory.setTrailMaxPoints(config.getTrailMaxPoints());

        Group root = new Group();
        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        simulation.addListener(new SimulationListener() {
            @Override
            public void onBodyRemoved(Celestial body) {
                if (body == trackedBody) {
                    log.info("Отслеживаемое тело {} удалено, камера возвращена в центр", body.getName());
                    trackedBody = null;
                }
            }
        });
        simulation.addListener(new EnergyMonitor(1000));

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

        Celestial Sun2 = factory.createFree("Sun2", 1, CelestialFactory.MassUnit.SOLAR_MASSES,
                                            -100, -20, 2, 0.5, 20, Color.CYAN);

        canvas.setOnMouseClicked(event -> {
            Celestial clicked = findBodyAt(event.getX(), event.getY(), currentOrigX, currentOrigY);
            if (clicked != trackedBody) {
                log.debug("Камера: {}", clicked == null ? "обзор сцены" : "слежение за " + clicked.getName());
            }
            trackedBody = clicked;
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
            }
        };

        timer.start();

        Scene scene = new Scene(root, Color.BLACK);
        stage.setScene(scene);
        stage.setTitle("Trajectorium");
        stage.setWidth(canvasWidth);
        stage.setHeight(canvasHeight);
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
}