package trajectorium.command;

import trajectorium.Integrator;
import trajectorium.Simulation;

public class SetIntegratorCommand implements Command {
    private final Simulation sim;
    private final Integrator integrator;
    private Integrator previous;

    public SetIntegratorCommand(Simulation sim, Integrator integrator) {
        this.sim = sim;
        this.integrator =integrator;
    }

    @Override
    public void execute() {
        previous = sim.getIntegrator();
        sim.setIntegrator(integrator);
    }

    @Override
    public void undo() {
        sim.setIntegrator(previous);
    }

    @Override
    public String getName() {
        return "Интегратор " + integrator.getName();
    }
}
