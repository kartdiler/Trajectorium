package trajectorium.report;

public enum MassClass {
    STAR(0.08, "class.star"),
    GIANT(1e-5, "class.giant"),
    PLANET(1e-7, "class.planet"),
    SMALL(0, "class.small");

    private final double minMass;
    private final String labelKey;

    MassClass(double minMass, String labelKey) {
        this.minMass = minMass;
        this.labelKey = labelKey;
    }

    public String labelKey() {
        return labelKey;
    }

    public static MassClass of(double massSolar) {
        for (MassClass c : values()) {
            if (massSolar >= c.minMass) return c;
        }
        return SMALL;
    }
}
