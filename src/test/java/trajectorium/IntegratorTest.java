package trajectorium;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegratorTest {
    private static final double DT = 1.0 / 365.25;

    @Test
    void verletConservesTotalMomentum() {
        List<Celestial> system = new ArrayList<Celestial>();

        system.add(new Celestial("A", 1.0, -0.5, 0, 0, 0.3, 5, Color.RED, system, DT, 100));
        system.add(new Celestial("B", 0.5, 0.5, 0, 0, -0.6, 5, Color.RED, system, DT, 100));

        double[] p0 = momentum(system);
        Integrator verlet = new VerletIntegrator();
        for (int i = 0; i < 1000; i++) {
            verlet.step(system, DT);
        }
        double[] p1 = momentum(system);

        assertEquals(p0[0], p1[0], 1e-9);
        assertEquals(p0[1], p1[1], 1e-9);
    }

    private static double[] momentum(List<Celestial> bodies) {
        double px = 0;
        double py = 0;

        for (Celestial c : bodies) {
            double[] v = c.getVelocity(DT);
            px += c.getMass() * v[0];
            py += c.getMass() * v[1];
        }

        return new double[] {px, py};
    }
}
