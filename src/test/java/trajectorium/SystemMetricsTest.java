package trajectorium;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SystemMetricsTest {
    private static final double DT = 1.0 / 365.25;

    private static List<Celestial> twinSystem() {
        List<Celestial> list = new ArrayList<Celestial>();
        list.add(new Celestial("A", 1.0, 1, 0, 0, 1, 5, Color.RED, list, DT, 100));
        list.add(new Celestial("B", 1.0, -1, 0, 0, -1, 5, Color.BLUE, list, DT, 100));

        list.get(0).setVelocity(0, 1, list, DT);
        list.get(1).setVelocity(0, -1, list, DT);
        return list;
    }

    @Test
    void totalEnergyIsKineticPlusPairwisePotential() {
        assertEquals(1 - 2 * Math.PI * Math.PI, SystemMetrics.totalEnergy(twinSystem(), DT), 1e-9);
    }

    @Test
    void centerOfMassIsMassWeighted() {
        List<Celestial> list = new ArrayList<Celestial>();
        list.add(new Celestial("A", 1.0, 0, 0, 0, 0, 5, Color.RED, list, DT, 100));
        list.add(new Celestial("B", 3.0, 4, 0, 0, 0, 5, Color.BLUE, list, DT, 100));

        double[] com = SystemMetrics.centerOfMass(list);

        assertEquals(3.0, com[0], 1e-12);
        assertEquals(0.0, com[1], 1e-12);
    }

    @Test
    void emptySystemGivesZeros() {
        List<Celestial> empty = List.of();
        assertEquals(0.0, SystemMetrics.totalEnergy(empty, DT), 0.0);
        assertEquals(0.0, SystemMetrics.angularMomentum(empty, DT), 0.0);
        assertEquals(0.0, SystemMetrics.centerOfMass(empty)[0], 0.0);
    }
}
