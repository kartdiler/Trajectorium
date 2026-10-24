package trajectorium;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trajectorium.CelestialTest.energy;

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

    @Test
    void verletDriftsLessThanEulerOnCircularOrbit() {
        double verletDrift = energyDrift(new VerletIntegrator());
        double eulerDrift = energyDrift(new EulerIntegrator());
        assertTrue(eulerDrift > 10 * verletDrift, "Euler=" + eulerDrift + " Verlet=" + verletDrift);
    }

    private static double energyDrift(Integrator integrator) {
        List<Celestial> system = new ArrayList<Celestial>();
        system.add(new Celestial("Sun", 1.0, 0, 0, 0, 0, 20, Color.YELLOW, system, DT, 100));
        system.add(new Celestial("Earth", 3.003e-6, 1, 0, 0, 2 * Math.PI, 5, Color.BLUE, system, DT, 100));
        Celestial Earth = system.get(1);

        double e0 = energy(Earth);
        double maxRel = 0;
        for (int i = 0; i < 365; i++) {
            integrator.step(system, DT);
            maxRel = Math.max(maxRel, Math.abs(energy(Earth) - e0) / Math.abs(e0));
        }

        return maxRel;
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
