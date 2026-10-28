package trajectorium.ui;

public class Camera {
    public static final double MIN_SCALE = 2;
    public static final double MAX_SCALE = 20000;

    private final double defaultScale;
    private double scale;
    private double centerX = 0;
    private double centerY = 0;
    private double viewWidth = 1;
    private double viewHeight = 1;

    public Camera(double scale) {
        this.defaultScale = clamp(scale);
        this.scale = this.defaultScale;
    }

    public void setViewSize(double width, double height) {
        viewWidth = Math.max(1, width);
        viewHeight = Math.max(1, height);
    }

    public double getScale() {
        return scale;
    }

    public double getViewWidth() {
        return viewWidth;
    }

    public double getViewHeight() {
        return viewHeight;
    }

    public double toScreenX(double wx) {
        return viewWidth / 2 + (wx - centerX) * scale;
    }

    public double toScreenY(double wy) {
        return viewHeight / 2 + (wy - centerY) * scale;
    }

    public double toWorldX(double sx) {
        return centerX + (sx - viewWidth / 2) / scale;
    }

    public double toWorldY(double sy) {
        return centerY + (sy - viewHeight / 2) / scale;
    }

    public void centerOn(double wx, double wy) {
        centerX = wx;
        centerY = wy;
    }

    public void panByPixels(double dx, double dy) {
        centerX -= dx / scale;
        centerY -= dy / scale;
    }

    public void zoomAt(double factor, double sx, double sy) {
        double wx = toWorldX(sx);
        double wy = toWorldY(sy);
        scale = clamp(scale * factor);
        centerX = wx - (sx - viewWidth / 2) / scale;
        centerY = wy - (sy - viewHeight / 2) / scale;
    }

    public void reset() {
        scale = defaultScale;
        centerX = 0;
        centerY = 0;
    }

    public void fit(double minX, double minY, double maxX, double maxY, double margin) {
        double w = Math.max(maxX - minX, 1.0);
        double h = Math.max(maxY - minY, 1.0);
        double usable = 1 - 2 * margin;
        scale = clamp(Math.min(viewWidth * usable / w, viewHeight * usable / h));
        centerX = (minX + maxX) / 2;
        centerY = (minY + maxY) / 2;
    }

    private static double clamp(double s) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }
}
