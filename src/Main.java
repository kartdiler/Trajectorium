import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class Main extends Application {
    static void main() {
        launch();
    }

    Simulation simulation = new Simulation(1 / 365.25);

    double canvasWidth = 1200;
    double canvasHeight = 800;
    double scale = 100;

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        Celestial Sun = new Celestial("Sun", 1, 0, 0,
                1, 0, 20, Color.YELLOW,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Sun);

        Celestial Earth = new Celestial("Earth", 3.003e-6, 1, 0,
                1, circularVelocity(Sun.getMass(), 1), 5, Color.BLUE,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Earth);

        double origX = canvasWidth / 2;
        double origY = canvasHeight / 2;

        Celestial trackedBody = Sun;

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
    }

    double circularVelocity(double parentMass, double r) {
        double GM = 4 * Math.PI * Math.PI * parentMass;
        return Math.sqrt(GM / r);
    }
}