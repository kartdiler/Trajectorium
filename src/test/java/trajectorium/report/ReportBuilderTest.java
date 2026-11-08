package trajectorium.report;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import trajectorium.Celestial;
import trajectorium.CelestialFactory;
import trajectorium.CelestialFactory.MassUnit;
import trajectorium.Simulation;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportBuilderTest {
    private static final double DT = 1.0 / 365.25;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 8, 12, 0);

    @Test
    void massClassBoundaries() {
        assertEquals(MassClass.STAR, MassClass.of(0.08));
        assertEquals(MassClass.GIANT, MassClass.of(0.0799));
        assertEquals(MassClass.GIANT, MassClass.of(1e-5));
        assertEquals(MassClass.PLANET, MassClass.of(9.99e-6));
        assertEquals(MassClass.PLANET, MassClass.of(1e-7));
        assertEquals(MassClass.SMALL, MassClass.of(1e-19));
        assertEquals(MassClass.GIANT, MassClass.of(9.55e-4));
        assertEquals(MassClass.PLANET, MassClass.of(3.003e-6));
        assertEquals(MassClass.SMALL, MassClass.of(3.69e-8));
    }

    @Test
    void emptySceneGivesZerosWithoutNan() {
        Simulation sim = new Simulation(DT);

        ReportData report = ReportBuilder.build(sim, NOW);
        ReportData.Summary s = report.summary();

        assertTrue(report.bodies().isEmpty());
        assertTrue(report.classes().isEmpty());
        assertEquals(0, s.bodyCount());
        assertNull(s.heaviestName());
        assertNull(s.closestPair());
        for (double v : new double[]{s.totalMass(), s.heaviestShare(), s.minSpeed(), s.averageSpeed(),
                s.maxSpeed(), s.centerOfMassX(), s.centerOfMassY(), s.totalEnergy(), s.angularMomentum()}) {
            assertEquals(0.0, v, 0.0);
        }
    }

    @Test
    void bodyTableAndSummaryOfSunAndEarth() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        Celestial sun = factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
        factory.createOrbiting("Earth", 3.003e-6, sun, 1.0, 5, Color.BLUE, true);

        ReportData report = ReportBuilder.build(sim, NOW);
        ReportData.BodyRow earth = report.bodies().get(1);

        assertEquals(2, report.bodies().size());
        assertEquals("Earth", earth.name());
        assertEquals(MassClass.PLANET, earth.massClass());
        assertEquals(2 * Math.PI, earth.speed(), 1e-9);
        assertEquals(29.78, earth.speedKmS(), 0.05);
        assertEquals("#0000FF", earth.color());

        ReportData.Summary s = report.summary();
        assertEquals(2, s.bodyCount());
        assertEquals(1.0 + 3.003e-6, s.totalMass(), 1e-12);
        assertEquals("Sun", s.heaviestName());
        assertEquals(1.0 / (1.0 + 3.003e-6), s.heaviestShare(), 1e-9);
        assertEquals(3.003e-6 / (1.0 + 3.003e-6), s.centerOfMassX(), 1e-12);
        assertEquals(2 * Math.PI, s.maxSpeed(), 1e-9);
        assertEquals("Sun", s.closestPair().first());
        assertEquals("Earth", s.closestPair().second());
        assertEquals(1.0, s.closestPair().distance(), 1e-9);
    }

    @Test
    void classStatsGroupBodiesByMass() {
        Simulation sim = new Simulation(DT);
        CelestialFactory f = new CelestialFactory(sim, 100);
        f.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
        f.createFree("Jupiter", 9.55e-4, MassUnit.SOLAR_MASSES, 5.2, 0, 0, 0, 6, Color.ORANGERED);
        f.createFree("Earth", 3.003e-6, MassUnit.SOLAR_MASSES, 1, 0, 0, 0, 5, Color.BLUE);
        f.createFree("Rock", 1e-12, MassUnit.SOLAR_MASSES, 0, 3, 0, 0, 2, Color.GRAY);
        f.createFree("Dust", 2e-12, MassUnit.SOLAR_MASSES, 0, -3, 0, 0, 2, Color.GRAY);

        List<ReportData.ClassStat> classes = ReportBuilder.build(sim, NOW).classes();

        assertEquals(List.of(MassClass.STAR, MassClass.GIANT, MassClass.PLANET, MassClass.SMALL),
                classes.stream().map(ReportData.ClassStat::massClass).toList());
        assertEquals(2, classes.get(3).count());
        assertEquals(3e-12, classes.get(3).totalMass(), 1e-18);
    }

    @Test
    void singleBodyHasNoClosestPair() {
        Simulation sim = new Simulation(DT);
        new CelestialFactory(sim, 100).createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);

        ReportData.Summary s = ReportBuilder.build(sim, NOW).summary();

        assertNull(s.closestPair());
        assertEquals(1.0, s.heaviestShare(), 1e-12);
        assertEquals(s.minSpeed(), s.maxSpeed(), 1e-12);
    }

    @Test
    void reportCarriesMetadataAndIsImmutable() {
        Simulation sim = new Simulation(DT);
        new CelestialFactory(sim, 100).createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
        for (int i = 0; i < 10; i++) sim.step();

        ReportData report = ReportBuilder.build(sim, NOW);

        assertEquals(NOW, report.generatedAt());
        assertEquals("Verlet", report.integrator());
        assertEquals(10 * DT, report.timeYears(), 1e-12);
        assertThrows(UnsupportedOperationException.class, () -> report.bodies().clear());
        assertThrows(UnsupportedOperationException.class, () -> report.classes().clear());
    }
}
