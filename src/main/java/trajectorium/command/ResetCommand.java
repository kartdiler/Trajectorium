package trajectorium.command;

import trajectorium.Simulation;

public class ResetCommand implements Command {
    private final Simulation sim;

    public ResetCommand(Simulation sim) {
        this.sim = sim;
    }

    @Override
    public void execute() {
        sim.reset();
    }

    @Override
    public void undo() {}

    @Override
    public boolean isUndoable() {
        return false;
    }

    @Override
    public String getName() {
        return "Сброс";
    }
}
