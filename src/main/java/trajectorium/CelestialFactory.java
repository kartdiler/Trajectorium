package trajectorium;

import javafx.scene.paint.Color;

public class CelestialFactory {
    public static final double G = 4 * Math.pow(Math.PI, 2);

    private static final double SOLAR_MASS_KG = 1.989e30;
    private static final double EARTH_MASS_KG = 5.972e24;

    private int trailMaxPoints = 150;

    public enum MassUnit {
        SOLAR_MASSES(1.0),
        EARTH_MASSES(EARTH_MASS_KG / SOLAR_MASS_KG),
        KILOGRAMS(1.0 / SOLAR_MASS_KG);

        private final double toSolar;

        MassUnit(double toSolar) {
            this.toSolar = toSolar;
        }

        public double toSolarMasses(double value) {
            return value * toSolar;
        }
    }

    private final Simulation simulation;
    private final double scale;

    public CelestialFactory(Simulation simulation, double scale) {
        this.simulation = simulation;
        this.scale = scale;
    }

    public static double circularVelocity(double centralMass, double r) {
        return Math.sqrt(G * centralMass / r);
    }

    public Celestial createStar(String name, double massSolar, double x, double y,
                                double canvasRadius, Color color) {
        return register(new Celestial(name, massSolar, x, y, 0, 0, canvasRadius,
                                    color, simulation.getBodies(), simulation.getDt(), scale));
    }

    public Celestial createOrbiting(String name, double massSolar, Celestial parent,
                                    double orbitRadius, double canvasRadius, Color color,
                                    boolean counterClockwise) {
        double dt = simulation.getDt();
        double[] parentPos = parent.getPos();
        double[] parentVelocity = parent.getVelocity(dt);

        double vRel = circularVelocity(parent.getMass(), orbitRadius);
        double sign = counterClockwise ? -1.0 : 1.0;

        return  register(new Celestial(name, massSolar, parentPos[0] + orbitRadius, parentPos[1],
                                        parentVelocity[0], parentVelocity[1] + sign * vRel, canvasRadius,
                                        color, simulation.getBodies(), dt, scale));
    }

    public Celestial createFree(String name, double massValue, MassUnit unit, double x, double y,
                                double vx, double vy, double canvasRadius, Color color) {
        return register(new Celestial(name, unit.toSolarMasses(massValue), x, y, vx, vy, canvasRadius,
                                      color, simulation.getBodies(), simulation.getDt(), scale));
    }

    public void setTrailMaxPoints(int n) {
        this.trailMaxPoints = n;
    }

    private Celestial register(Celestial body) {
        body.getTrail().setMaxPoints(trailMaxPoints);
        simulation.addBody(body);
        return body;
    }
}
