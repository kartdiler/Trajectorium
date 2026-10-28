package trajectorium;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import trajectorium.command.*;
import trajectorium.ui.BodyInfoPanel;
import trajectorium.ui.Camera;
import trajectorium.ui.ControlPanel;
import trajectorium.ui.NewBodyDialog;

import java.util.List;

public class Main extends Application {
    public static void main() {
        launch();
    }

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final double MIN_BODY_PX = 2;
    private static final double CLICK_MIN_PX = 8;
    private static final double DRAG_THRESHOLD = 4;

    AppConfig config = AppConfig.load();
    Simulation simulation = new Simulation(config.getDt());
    CommandHistory history = new CommandHistory(50);
    CelestialFactory factory = new CelestialFactory(simulation, config.getScale());
    Camera camera = new Camera(config.getScale());
    EnergyMonitor energyMonitor = new EnergyMonitor(2000);

    BodyInfoPanel infoPanel;
    boolean following = false;
    Celestial trackedBody = null;
    int bodyCounter = 0;

    double pressX, pressY, lastX, lastY;
    boolean dragged = false;

    @Override
    public void start(Stage stage) {
        simulation.setSimulationSpeed(config.getSimulationSpeed());
        factory.setTrailMaxPoints(config.getTrailMaxPoints());
        log.info("Запуск Trajectorium: масштаб {}, шаг {} года", config.getScale(), simulation.getDt());

        Canvas canvas = new Canvas();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Pane canvasHolder = new Pane(canvas);
        canvasHolder.setPrefSize(config.getCanvasWidth(), config.getCanvasHeight());
        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());

        ControlPanel controlPanel = new ControlPanel(simulation, history, this::buildDefaultScene);
        infoPanel = new BodyInfoPanel(simulation, history);
        VBox sideBar = new VBox(controlPanel, infoPanel);
        sideBar.setStyle("-fx-background-color: #222222");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: black");
        root.setCenter(canvasHolder);
        root.setRight(sideBar);

        simulation.addListener(energyMonitor);
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

        buildDefaultScene();

        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                pressX = lastX = event.getX();
                pressY = lastY = event.getY();
                dragged = false;
            }
        });

        canvas.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) return;
            if (!dragged && Math.hypot(event.getX() - pressX, event.getY() - pressY) < DRAG_THRESHOLD) return;

            dragged = true;
            following = false;
            camera.panByPixels(event.getX() - lastX, event.getY() - lastY);
            lastX = event.getX();
            lastY = event.getY();
        });

        canvas.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (dragged) {
                    dragged = false;
                    return;
                }
                Celestial clicked = findBodyAt(event.getX(), event.getY());
                select(clicked);
                following = clicked != null;
            } else if (event.getButton() == MouseButton.SECONDARY) {
                addBodyAt(event.getX(), event.getY(), canvas.getScene().getWindow());
            }
        });

        canvas.setOnScroll(event -> {
            double factor = Math.exp(event.getDeltaY() * 0.003);
            if (following) {
                camera.zoomAt(factor, camera.getViewWidth() / 2, camera.getViewHeight() / 2);
            }
        });

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                simulation.step();
                render(gc, canvas.getWidth(), canvas.getHeight());
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
            } else if (e.getCode() == KeyCode.F) {
                fitView();
            } else if (e.getCode() == KeyCode.DIGIT0) {
                following = false;
                camera.reset();
            } else if (e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
    }

    private double bodyRadiusPx(Celestial cel) {
        return Math.max(MIN_BODY_PX, cel.getRealRadius() * camera.getScale());
    }

    private Celestial findBodyAt(double scrX, double scrY) {
        Celestial best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Celestial cel : simulation.getBodies()) {
            double[] p = cel.getPos();

            double dist = Math.hypot(scrX - camera.toScreenX(p[0]), scrY - camera.toScreenY(p[1]));
            if (dist <= Math.max(bodyRadiusPx(cel), CLICK_MIN_PX) && dist < bestDistance) {
                best = cel;
                bestDistance = dist;
            }
        }
        return best;
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

    private void fitView() {
        List<Celestial> bodies = simulation.getBodies();
        if (bodies.isEmpty()) return;

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (Celestial cel : bodies) {
            double[] p = cel.getPos();
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }

        following = false;
        camera.fit(minX, minY, maxX, maxY, 0.1);
    }

    private void render(GraphicsContext gc, double w, double h) {
        camera.setViewSize(w, h);
        if (following && trackedBody != null) {
            double[] p = trackedBody.getPos();
            camera.centerOn(p[0], p[1]);
        }

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        for (Celestial cel : simulation.getBodies()) {
            var points = cel.getTrail().getPoints();
            int total = points.size();
            int i = 0;

            gc.setFill(cel.getColor());
            for (double[] point : points) {
                double sx = camera.toScreenX(point[0]);
                double sy = camera.toScreenY(point[1]);

                double opacity = total > 1 ? (double) i / (total - 1) : 1.0;
                gc.setGlobalAlpha(opacity * 0.6);
                gc.fillOval(sx - 1, sy - 1, 2, 2);
                i++;
            }
        }
        gc.setGlobalAlpha(1.0);

        for (Celestial cel : simulation.getBodies()) {
            double[] p = cel.getPos();
            double cx = camera.toScreenX(p[0]);
            double cy = camera.toScreenY(p[1]);
            double r = bodyRadiusPx(cel);
            gc.setFill(cel.getColor());
            gc.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        }

        if (trackedBody != null) {
            double[] p = trackedBody.getPos();
            double r = bodyRadiusPx(trackedBody) + 4;
            gc.setStroke(Color.WHITE);
            gc.strokeOval(camera.toScreenX(p[0]) - r, camera.toScreenY(p[1]) - r, 2 * r, 2 * r);
        }
    }

    private void addBodyAt(double scrX, double scrY, Window owner) {
        double x = camera.toWorldX(scrX);
        double y = camera.toWorldY(scrY);

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