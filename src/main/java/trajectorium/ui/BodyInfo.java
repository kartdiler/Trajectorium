package trajectorium.ui;

import trajectorium.Celestial;
import trajectorium.Simulation;

public record BodyInfo(String name, double mass, double x, double y,
                       double speedAuYear, double speedKmS, String refName,
                       double refDistance) {
    public static final double AU_YEAR_TO_KM_S = 4.74047;

    public static BodyInfo of(Celestial body, Simulation sim) {
        double[] p = body.getPos();
        double[] v = body.getVelocity(sim.getDt());
        double speed = Math.hypot(v[0], v[1]);

        Celestial ref = null;
        for (Celestial c : sim.getBodies()) {
            if (c != body && (ref == null || c.getMass() > ref.getMass())) {
                ref = c;
            }
        }

        String refName = null;
        double refDistance = Double.NaN;
        if (ref != null) {
            double[] q = ref.getPos();
            refName = ref.getName();
            refDistance = Math.hypot(p[0] - q[0], p[1] - q[1]);
        }

        return new BodyInfo(body.getName(), body.getMass(), p[0], p[1], speed,
                speed * AU_YEAR_TO_KM_S, refName, refDistance);
    }
}
