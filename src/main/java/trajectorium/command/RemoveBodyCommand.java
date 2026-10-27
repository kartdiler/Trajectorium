package trajectorium.command;

import trajectorium.Celestial;
import trajectorium.Simulation;

public class RemoveBodyCommand implements Command {
    private final Simulation sim;
    private final Celestial body;

    public RemoveBodyCommand(Simulation sim, Celestial body) {
        this.sim = sim;
        this.body = body;
    }

    @Override
    public void execute() {
        sim.removeBody(body);
    }

    @Override
    public void undo() {
        sim.addBody(body);
    }

    @Override
    public String getName() {
        return "Удалить тело " + body.getName();
    }
}
