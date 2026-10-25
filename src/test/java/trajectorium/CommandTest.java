package trajectorium;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {
    private static final double DT = 1.0 / 365.25;

    @Test
    void addBodyCanBeUndoneAndRedone() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        CommandHistory history = new CommandHistory(10);

        history.execute(new AddBodyCommand(sim, () -> factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW)));
        assertEquals(1, sim.getBodies().size());
        Celestial sun = sim.getBodies().get(0);

        history.undo();
        assertEquals(0, sim.getBodies().size());

        history.redo();
        assertEquals(1, sim.getBodies().size());
        assertSame(sun, sim.getBodies().get(0));
    }

    @Test
    void removeBodyCanBeUndone() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        CommandHistory history = new CommandHistory(10);
        Celestial sun = factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);

        history.execute(new RemoveBodyCommand(sim, sun));
        assertTrue(sim.getBodies().isEmpty());

        history.undo();
        assertTrue(sim.getBodies().contains(sun));
    }
}
