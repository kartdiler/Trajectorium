package trajectorium.report;

import java.time.LocalDateTime;
import java.util.List;

public record ReportData(LocalDateTime generatedAt, String integrator, double timeYears,
                         List<BodyRow> bodies, Summary summary, List<ClassStat> classes) {
    public record BodyRow(String name, MassClass massClass, double mass, double x, double y,
                          double vx, double vy, double speed, double speedKmS, double radiusPx,
                          String color) {}

    public record Summary(int bodyCount, double totalMass, String heaviestName, double heaviestShare,
                          double minSpeed, double averageSpeed, double maxSpeed, double centerOfMassX,
                          double centerOfMassY, double totalEnergy, double angularMomentum, ClosestPair closestPair) {}

    public record ClosestPair(String first, String second, double distance) {}

    public record ClassStat(MassClass massClass, int count, double totalMass, double averageSpeed) {}
}
