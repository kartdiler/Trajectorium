package trajectorium;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CelestialTest {
    private static final double DT = 1.0 / 365.25;
    private static final Integrator VERLET = new VerletIntegrator();
    private static final double GM_SUN = 4 * Math.PI * Math.PI;

    @Test
    void initialVelocityMatchesConstructorInput() {
        List<Celestial> bodies = new ArrayList<Celestial>();

        Celestial body = new Celestial("Test", 1.0, 1, 0, 0, 2 * Math.PI,
                5, Color.WHITE, bodies, DT, 100);
        double[] v = body.getVelocity(DT);
        assertEquals(0, v[0], 1e-9);
        assertEquals(2 * Math.PI, v[1], 1e-9);
    }

    @Test
    void accelerationIsZeroWithoutOtherBodies() {
        Celestial one = new Celestial("One", 1.0, 1, 0, 0, 0,
                5, Color.WHITE, List.of(), DT, 100);
        double[] a = Celestial.acceleration(one, List.of());
        assertEquals(0, a[0], 1e-12);
        assertEquals(0, a[1], 1e-12);
    }

    @Test
    void accelerationPointsTowardAttractor() {
        List<Celestial> system =  new ArrayList<Celestial>();

        Celestial Sun = new Celestial("Sun", 1.0, 0, 0, 0, 0,
                20, Color.YELLOW, system, DT, 100);
        system.add(Sun);

        Celestial Earth = new Celestial("Earth", 3.003e-6, 1, 0, 0, 2 * Math.PI,
                        5, Color.BLUE, system, DT, 100);

        double[] a = Celestial.acceleration(Earth, system);
        assertTrue(a[0] < 0);
        assertEquals(0, a[1], 1e-9);
    }

    @Test
    void accelerationMagnitudeMatchesNewtonLaw() {
        List<Celestial> system =  new ArrayList<Celestial>();
        Celestial Sun = new Celestial("Sun", 1.0, 0, 0, 0, 0,
                20, Color.YELLOW, system, DT, 100);
        system.add(Sun);

        Celestial Planet = new Celestial("Planet", 1e-6, 2, 0, 0, 0,
                5, Color.BLUE, system, DT, 100);

        double[] a = Celestial.acceleration(Planet, system);
        double expected = -1 * 4 * Math.PI * Math.PI * 1.0 / (2.0 * 2.0);
        assertEquals(expected, a[0], 1e-9);
    }

    @Test
    void newtonThirdLawHolds() {
        List<Celestial> system = new ArrayList<Celestial>();

        Celestial a = new Celestial("A", 2.0, 0, 0, 0, 0,
                5, Color.RED, system, DT, 100);
        system.add(a);

        Celestial b = new Celestial("B", 0.5, 1, 0, 0, 0,
                5, Color.BLUE, system, DT, 100);
        system.add(b);

        double[] aa = Celestial.acceleration(a, system);
        double[] ab = Celestial.acceleration(b, system);

        assertEquals(0, a.getMass() * aa[0] + b.getMass() * ab[0], 1e-9);
        assertEquals(0, a.getMass() * aa[1] + b.getMass() * ab[1], 1e-9);
    }

    @Test
    void freeBodyMovesInStraightLine() {
        List<Celestial> system = new ArrayList<Celestial>();
        system.add(new Celestial("Body", 1.0, 0, 0, 1.0, 0.5,
                5, Color.WHITE, system, DT, 100));

        run(system, 100);

        double[] pos = system.get(0).getPos();
        assertEquals(1.0 * 100 * DT, pos[0], 1e-9);
        assertEquals(0.5 * 100 * DT, pos[1], 1e-9);
    }

    @Test
    void circularOrbitKeepsRadius() {
        List<Celestial> system = sunEarthSystem();
        Celestial Sun = system.get(0);
        Celestial Earth = system.get(1);

        double maxDeviation = 0;
        for (int i = 0; i < 400; i++) {
            VERLET.step(system, DT);
            maxDeviation = Math.max(maxDeviation, Math.abs(distance(Sun, Earth) - 1.0));
        }

        assertTrue(maxDeviation < 1e-3, "Радиус орбиты ушел на " + maxDeviation + " а.е.");
    }

    @Test
    void totalAngularMomentumIsConserved() {
        List<Celestial> system = sunEarthSystem();

        double l0 = totalAngularMomentum(system);
        run(system, 1000);

        assertEquals(l0, totalAngularMomentum(system), 1e-9);
    }

    @Test
    void energyDoesNotDrift() {
        List<Celestial> system = sunEarthSystem();

        double e0 = totalEnergy(system);
        double maxRel = 0;
        for (int i = 0; i < 3650; i++) {
            VERLET.step(system, DT);
            maxRel = Math.max(maxRel, Math.abs(totalEnergy(system) - e0) / Math.abs(e0));
        }

        assertTrue(maxRel < 1e-3, "Энергия ушла на " + maxRel);
    }

    private static void run(List<Celestial> system, int steps) {
        for (int i = 0; i < steps; i++) {
            VERLET.step(system, DT);
        }
    }

    private static List<Celestial> sunEarthSystem() {
        List<Celestial> system = new ArrayList<Celestial>();
        system.add(new Celestial("Sun", 1.0, 0, 0, 0, 0, 20, Color.YELLOW, system, DT, 100));
        system.add(new Celestial("Earth", 3.003e-6, 1, 0, 0, 2 * Math.PI, 5, Color.BLUE, system, DT, 100));

        return system;
    }

    private static double distance(Celestial a, Celestial b) {
        double[] pa = a.getPos();
        double[] pb = b.getPos();

        return Math.hypot(pa[0] - pb[0], pa[1] - pb[1]);
    }

    private static double totalAngularMomentum(List<Celestial> bodies) {
        double l = 0;

        for (Celestial c : bodies) {
            double[] p = c.getPos();
            double[] v = c.getVelocity(DT);
            l += c.getMass() * (p[0] * v[1] - p[1] * v[0]);
        }

        return l;
    }

    private static double totalEnergy(List<Celestial> bodies) {
        double kinetic = 0;
        double potential = 0;

        for (int i = 0; i < bodies.size(); i++) {
            Celestial c = bodies.get(i);
            double[] v = c.getVelocity(DT);
            kinetic += 0.5 * c.getMass() * (v[0] * v[0] + v[1] * v[1]);
            for (int j = i + 1; j < bodies.size(); j++) {
                potential -= GM_SUN * c.getMass() * bodies.get(j).getMass() / distance(c, bodies.get(j));
            }
        }

        return kinetic + potential;
    }
}
