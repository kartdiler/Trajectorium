package trajectorium.io;

public record BodyData(String name, double mass, double x, double y,
                       double vx, double vy, double canvasRadius, String color) {}
