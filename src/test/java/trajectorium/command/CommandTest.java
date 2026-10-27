package trajectorium.command;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import trajectorium.Celestial;
import trajectorium.CelestialFactory;
import trajectorium.Simulation;

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

    @Test
    void pauseCommandIsUndoable() {
        Simulation sim = new Simulation(DT);
        CommandHistory history = new CommandHistory(10);

        history.execute(new SetRunningCommand(sim, false));
        assertFalse(sim.isRunning());

        history.undo();
        assertTrue(sim.isRunning());
    }

    @Test
    void speedCommandRestoresPreviousSpeed() {
        Simulation sim = new Simulation(DT);
        CommandHistory history = new CommandHistory(10);
        double before = sim.getSimulationSpeed();

        history.execute(new SetSpeedCommand(sim, 5));
        assertEquals(5, sim.getSimulationSpeed(), 1e-12);

        history.undo();
        assertEquals(before, sim.getSimulationSpeed(), 1e-12);
    }

    @Test
    void newCommandClearRedoStack() {
        Simulation sim = new Simulation(DT);
        CommandHistory history = new CommandHistory(10);

        history.execute(new SetSpeedCommand(sim, 2));
        history.undo();
        assertTrue(history.canRedo());

        history.execute(new SetSpeedCommand(sim, 3));
        assertFalse(history.canRedo());
    }

    @Test
    void resetCannotBeUndoneAndClearsHistory() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        CommandHistory history = new CommandHistory(10);

        history.execute(new AddBodyCommand(sim, () -> factory.createStar("Sun",
                                        1, 0, 0, 20, Color.YELLOW)));
        history.execute(new ResetCommand(sim));

        assertTrue(sim.getBodies().isEmpty());
        assertFalse(history.canUndo());
    }

    @Test
    void historyRespectsLimit() {
        Simulation sim = new Simulation(DT);
        CommandHistory history = new CommandHistory(2);

        history.execute(new SetSpeedCommand(sim, 2));
        history.execute(new SetSpeedCommand(sim, 3));
        history.execute(new SetSpeedCommand(sim, 4));

        assertTrue(history.undo());
        assertTrue(history.undo());
        assertFalse(history.undo());
    }
}
