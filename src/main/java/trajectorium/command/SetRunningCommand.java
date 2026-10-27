package trajectorium.command;

import trajectorium.Simulation;

public class SetRunningCommand implements Command {
    private final Simulation sim;
    private final boolean running;
    private boolean previous;

    public SetRunningCommand(Simulation sim, boolean running) {
        this.sim = sim;
        this.running = running;
    }

    @Override
    public void execute() {
        previous = sim.isRunning();
        if (running) {
            sim.play();
        } else {
            sim.pause();
        }
    }

    @Override
    public void undo() {
        if (previous) {
            sim.play();
        } else {
            sim.pause();
        }
    }

    @Override
    public String getName() {
        return running ? "Продолжить" : "Пауза";
    }
}
