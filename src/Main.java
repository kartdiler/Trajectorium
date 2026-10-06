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

    Celestial trackedBody = null;
    double currentOrigX = canvasWidth / 2;
    double currentOrigY = canvasHeight / 2;

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        Celestial Sun = new Celestial("Sun", 1, 0, 0,
                0, 0, 20, Color.YELLOW,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Sun);

        Celestial Mercury = new Celestial("Mercury", 1.66e-7, 0.39, 0,
                0, -circularVelocity(Sun.getMass(), 0.39), 2, Color.GRAY,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Mercury);

        Celestial Venus = new Celestial("Venus", 2.45e-6, 0.72, 0,
                0, circularVelocity(Sun.getMass(), 0.72), 4.5, Color.LIGHTYELLOW,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Venus);

        Celestial Earth = new Celestial("Earth", 3.003e-6, 1, 0,
                0, -circularVelocity(Sun.getMass(), 1), 0.5, Color.BLUE,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Earth);

        Celestial Mars = new Celestial("Mars", 3.23e-7, 1.52, 0,
                0, -circularVelocity(Sun.getMass(), 1.52), 4, Color.INDIANRED,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Mars);

        Celestial Jupiter = new Celestial("Jupiter", 9.55e-4, 5.2, 0,
                0, -circularVelocity(Sun.getMass(), 5.2), 2, Color.ORANGERED,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Jupiter);

        Celestial Moon = new Celestial("Moon", 3.69e-8, 5.31257, 0,
                0, -circularVelocity(Jupiter.getMass(), 0.11257) - circularVelocity(Sun.getMass(), 5.31257), 0.5, Color.WHITE,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Moon);
        Moon.getTrail().setEnabled(false);

        Celestial Saturn = new Celestial("Saturn", 2.86e-4, 9.54, 0,
                0, -circularVelocity(Sun.getMass(), 9.54), 0.5, Color.NAVAJOWHITE,
                simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Saturn);

        Celestial Asteroid = new Celestial("Asteroid", 1e-19, 9.74, -0.5,
                -1, 0.7, 0.5, Color.GRAY, simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Asteroid);
//        Asteroid.getTrail().setEnabled(false);

        Celestial Asteroid1 = new Celestial("Asteroid", 1e-19, 5, 1,
                -2, 2, 0.5, Color.GRAY, simulation.getBodies(), simulation.getDt(), scale);
        simulation.addBody(Asteroid1);

//        Celestial Sun1 = new Celestial("Sun1", 10, -50, 5,
//                2, -1, 30, Color.HOTPINK,
//                simulation.getBodies(), simulation.getDt(), scale);
//        simulation.addBody(Sun1);

        canvas.setOnMouseClicked(event -> {
            trackedBody = findBodyAt(event.getX(), event.getY(), currentOrigX, currentOrigY);
        });

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

                simulation.step();

                if (trackedBody != null && !simulation.getBodies().contains(trackedBody)) {
                    trackedBody = null;
                }

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
    }

    double circularVelocity(double parentMass, double r) {
        double GM = 4 * Math.PI * Math.PI * parentMass;
        return Math.sqrt(GM / r);
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