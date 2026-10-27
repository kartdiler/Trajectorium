package trajectorium.command;

import trajectorium.Simulation;

public class SetSpeedCommand implements Command {
    private final Simulation sim;
    private final double speed;
    private double previous;

    public SetSpeedCommand(Simulation sim, double speed) {
        this.sim = sim;
        this.speed = speed;
    }

    @Override
    public void execute() {
        previous = sim.getSimulationSpeed();
        sim.setSimulationSpeed(speed);
    }

    @Override
    public void undo() {
        sim.setSimulationSpeed(previous);
    }

    @Override
    public String getName() {
        return "Скорость " + speed;
    }
}
