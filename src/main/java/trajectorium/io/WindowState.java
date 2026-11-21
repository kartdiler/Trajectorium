package trajectorium.io;

public record WindowState(double x, double y, double width, double height, boolean maximized) {
    public boolean boundsAreValid() {
        return Double.isFinite(x) && Double.isFinite(y) && Math.abs(x) <= 100_000 && Math.abs(y) < 100_000
                && Double.isFinite(width) && width >= 320 && width <= 10_000
                && Double.isFinite(height) && height >= 240 && height <= 10_000;
    }
}
