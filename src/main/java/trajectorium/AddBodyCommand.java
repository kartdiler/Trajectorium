package trajectorium;

import java.util.function.Supplier;

public class AddBodyCommand implements Command {
    private final Simulation sim;
    private final Supplier<Celestial> creator;
    private Celestial body;

    public AddBodyCommand(Simulation sim, Supplier<Celestial> creator) {
        this.sim = sim;
        this.creator = creator;
    }

    @Override
    public void execute() {
        if (body == null) {
            body = creator.get();
        } else {
            sim.addBody(body);
        }
    }

    @Override
    public void undo() {
        sim.removeBody(body);
    }

    @Override
    public String getName() {
        return "Добавить тело" + (body != null ? " " + body.getName() : "");
    }
}
