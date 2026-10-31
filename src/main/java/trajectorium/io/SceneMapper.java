package trajectorium.io;

import javafx.scene.paint.Color;
import trajectorium.*;

import java.util.ArrayList;
import java.util.List;

public final class SceneMapper {
    private SceneMapper() {}

    public static boolean isKnownIntegrator(String name) {
        return "Verlet".equals(name) || "Euler".equals(name);
    }

    private static Integrator integratorByName(String name) {
        return "Euler".equals(name) ? new EulerIntegrator() : new VerletIntegrator();
    }

    public static SceneData capture(Simulation sim) {
        List<Celestial> bodies = sim.getBodies();
        List<BodyData> list = new ArrayList<BodyData>();
        for (Celestial c : bodies) {
            double[] p = c.getPos();
            double[] v = c.getVelocity(sim.getDt(), bodies);
            list.add(new BodyData(c.getName(), c.getMass(), p[0], p[1], v[0], v[1],
                                  c.getCanvasRadius(), toHex(c.getColor())));
        }

        return new SceneData(SceneData.CURRENT_VERSION, sim.getIntegrator().getName(),
                Math.max(0.1, sim.getSimulationSpeed()), list);
    }

    public static void apply(SceneData data, Simulation sim, CelestialFactory factory) {
        sim.reset();

        List<Celestial> created = new ArrayList<Celestial>();
        for (BodyData b : data.bodies()) {
            created.add(factory.createFree(b.name(), b.mass(), CelestialFactory.MassUnit.SOLAR_MASSES,
                    b.x(), b.y(), b.vx(), b.vy(), b.canvasRadius(), Color.web(b.color())));
        }

        for (int i = 0; i < created.size(); i++) {
            BodyData b = data.bodies().get(i);
            created.get(i).setVelocity(b.vx(), b.vy(), sim.getBodies(), sim.getDt());
        }

        sim.setIntegrator(integratorByName(data.integrator()));
        sim.setSimulationSpeed(data.simulationSpeed());
    }

    static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}
