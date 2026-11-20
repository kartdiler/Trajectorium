package trajectorium;

public class Check {
    private Check() {}

    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException("Параметр \"" + name + "\" не задан");
        }
        return value;
    }

    public static String text(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Параметр \"" + name + "\" не должен быть пустым");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException("Параметр \"" + name + "\" длиннее " + maxLength
                    + " символов: " + value.length());
        }
        return value;
    }

    public static double positive(double value, String name) {
        if (!(Double.isFinite(value) && value > 0)) {
            throw new IllegalArgumentException(
                    "Параметр \"" + name + "\" должен быть положительным конечным числом, получено: " + value);
        }
        return value;
    }

    public static double positiveAtMost(double value, double max, String name) {
        if (!(Double.isFinite(value) && value > 0 && value <= max)) {
            throw new IllegalArgumentException("Параметр \"" + name
                    + "\" должен быть положительным и не больше " + max + ", получено: " + value);
        }
        return value;
    }

    public static double inRange(double value, double min, double max, String name) {
        if (!(Double.isFinite(value) && value >= min && value <= max)) {
            throw new IllegalArgumentException("Параметр \"" + name
                    + "\" должен быть в пределах [" + min + "; " + max + "], получено: " + value);
        }
        return value;
    }

    public static int atLeast(int value, int min, String name) {
        if (value < min) {
            throw new IllegalArgumentException(
                    "Параметр \"" + name + "\" должен быть не меньше " + min + ", получено: " + value);
        }
        return value;
    }
}
