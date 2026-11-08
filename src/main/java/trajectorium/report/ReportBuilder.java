package trajectorium.report;

import trajectorium.Celestial;
import trajectorium.Simulation;
import trajectorium.SystemMetrics;
import trajectorium.Units;
import trajectorium.io.SceneMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ReportBuilder {
    private ReportBuilder() {}

    public static ReportData build(Simulation sim, LocalDateTime generatedAt) {
        List<Celestial> bodies = sim.getBodies();
        double dt = sim.getDt();

        List<ReportData.BodyRow> rows = new ArrayList<ReportData.BodyRow>(bodies.size());
        for (Celestial c : bodies) {
            double[] p = c.getPos();
            double[] v = c.getVelocity(dt, bodies);
            double speed = Math.hypot(v[0], v[1]);
            rows.add(new ReportData.BodyRow(c.getName(), MassClass.of(c.getMass()), c.getMass(),
                    p[0], p[1], v[0], v[1], speed, speed * Units.AU_YEAR_TO_KM_S,
                    c.getCanvasRadius(), SceneMapper.toHex(c.getColor())));
        }

        return new ReportData(generatedAt, sim.getIntegrator(). getName(), sim.getTime(),
                List.copyOf(rows), summarize(rows, bodies, dt), List.copyOf(classStats(rows)));
    }

    private static ReportData.Summary summarize(List<ReportData.BodyRow> rows, List<Celestial> bodies, double dt) {
        int n = rows.size();
        double totalMass = 0;
        double speedSum = 0;
        double minSpeed = Double.MAX_VALUE;
        double maxSpeed = 0;
        ReportData.BodyRow heaviest = null;

        for (ReportData.BodyRow r : rows) {
            totalMass += r.mass();
            speedSum += r.speed();
            minSpeed = Math.min(minSpeed, r.speed());
            maxSpeed = Math.max(maxSpeed, r.speed());
            if (heaviest == null || r.mass() > heaviest.mass()) heaviest = r;
        }
        if (n == 0) {
            minSpeed = 0;
        }

        double[] com = SystemMetrics.centerOfMass(bodies);
        return new ReportData.Summary(
                n, totalMass,
                heaviest == null ? null : heaviest.name(),
                heaviest == null || totalMass == 0 ? 0 : heaviest.mass() / totalMass,
                minSpeed, n == 0 ? 0 : speedSum / n, maxSpeed,
                com[0], com[1],
                SystemMetrics.totalEnergy(bodies, dt),
                SystemMetrics.angularMomentum(bodies, dt),
                closestPair(rows)
        );
    }

    private static ReportData.ClosestPair closestPair(List<ReportData.BodyRow> rows) {
        ReportData.ClosestPair best = null;
        for (int i = 0; i < rows.size(); i++) {
            for (int j = i + 1; j < rows.size(); j++) {
                ReportData.BodyRow a = rows.get(i);
                ReportData.BodyRow b = rows.get(j);
                double d = Math.hypot(a.x() - b.x(), a.y() - b.y());
                if (best == null || d < best.distance()) {
                    best = new ReportData.ClosestPair(a.name(), b.name(), d);
                }
            }
        }
        return best;
    }

    private static List<ReportData.ClassStat> classStats(List<ReportData.BodyRow> rows) {
        List<ReportData.ClassStat> result = new ArrayList<ReportData.ClassStat>();
        for (MassClass c : MassClass.values()) {
            int count = 0;
            double mass = 0;
            double speed = 0;
            for (ReportData.BodyRow r : rows) {
                if (r.massClass() == c) {
                    count++;
                    mass += r.mass();
                    speed += r.speed();
                }
            }
            if (count > 0) {
                result.add(new ReportData.ClassStat(c, count, mass, speed));
            }
        }
        return result;
    }
}
