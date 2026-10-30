package trajectorium.ui;

public class DragVelocity {
    private DragVelocity() {}

    public static final double ARROW_YEARS = 0.1;

    public static double[] velocity(double startX, double startY, double endX, double endY) {
        return new double[] {(endX - startX) / ARROW_YEARS, (endY - startY) / ARROW_YEARS};
    }
}