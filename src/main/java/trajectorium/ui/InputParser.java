package trajectorium.ui;

import java.util.OptionalDouble;
import java.util.regex.Pattern;

public final class InputParser {
    private InputParser() {}

    private static final Pattern NAME = Pattern.compile(
            "^[\\p{L}\\p{N}][\\p{L}\\p{N} _-]{0,19}$"
    );

    private static final Pattern NUMBER = Pattern.compile(
            "^[+-]?(\\d+([.,]\\d*)?|[.,]\\d+)([eE][+-]?\\d+)?$"
    );

    public static boolean isValidName(String s) {
        return s != null && NAME.matcher(s.trim()).matches();
    }

    public static OptionalDouble parseNumber(String s) {
        if (s == null) {
            return OptionalDouble.empty();
        }

        String t = s.trim();
        if (!NUMBER.matcher(t).matches()) {
            return OptionalDouble.empty();
        }

        double v = Double.parseDouble(t.replace(',', '.'));
        return Double.isFinite(v) ? OptionalDouble.of(v) : OptionalDouble.empty();
    }

    public static OptionalDouble parsePositive(String s) {
        OptionalDouble v = parseNumber(s);
        return v.isPresent() && v.getAsDouble() > 0 ? v : OptionalDouble.empty();
    }
}
