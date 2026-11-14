package trajectorium.text;

import trajectorium.CelestialFactory.MassUnit;
import trajectorium.Units;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuantityParser {
    private QuantityParser() {}

    public record Quantity(double value, String unit) {}

    public record Mass(double value, MassUnit unit) {}

    private static final Pattern EXOTIC_SPACES = Pattern.compile("[\\u00A0\\u202F\\u2009]");
    private static final Pattern UNICODE_MINUS = Pattern.compile("[\\u2212\\u2013]");
    private static final Pattern HUMAN_EXPONENT = Pattern.compile("(\\d)\\s*[×xх*·]\\s*10\\s*\\^\\s*([+-]?\\d+)");
    private static final Pattern DECIMAL_COMMA = Pattern.compile("(\\d),(\\d)");

    private static final Pattern QUANTITY = Pattern.compile("^(?<value>[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)\\s*(?<unit>[^\\d\\s+-].*)?$");

    private static final Map<String, MassUnit> MASS_UNITS = Map.ofEntries(
            Map.entry("msun", MassUnit.SOLAR_MASSES),
            Map.entry("m_sun", MassUnit.SOLAR_MASSES),
            Map.entry("мсолн", MassUnit.SOLAR_MASSES),
            Map.entry("mearth", MassUnit.EARTH_MASSES),
            Map.entry("m_earth", MassUnit.EARTH_MASSES),
            Map.entry("мземл", MassUnit.EARTH_MASSES),
            Map.entry("кг", MassUnit.KILOGRAMS),
            Map.entry("kg", MassUnit.KILOGRAMS));

    private static final double M_S_TO_AU_YEAR = 1.0 / (Units.AU_YEAR_TO_KM_S * 1000);
    private static final double KM_S_TO_AU_YEAR = 1.0 / Units.AU_YEAR_TO_KM_S;

    private static final Map<String, Double> VELOCITY_UNITS = Map.ofEntries(
            Map.entry("а.е./год", 1.0),
            Map.entry("ае/год", 1.0),
            Map.entry("au/yr", 1.0),
            Map.entry("au/year", 1.0),
            Map.entry("км/с", KM_S_TO_AU_YEAR),
            Map.entry("km/s", KM_S_TO_AU_YEAR),
            Map.entry("м/с", M_S_TO_AU_YEAR),
            Map.entry("m/s", M_S_TO_AU_YEAR));

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim();
        s = EXOTIC_SPACES.matcher(s).replaceAll(" ");
        s = UNICODE_MINUS.matcher(s).replaceAll("-");
        s = HUMAN_EXPONENT.matcher(s).replaceAll("$1e$2");
        s = DECIMAL_COMMA.matcher(s).replaceAll("$1.$2");
        return s;
    }

    public static Optional<Quantity> parse(String text) {
        Matcher m = QUANTITY.matcher(normalize(text));
        if (!m.matches()) {
            return Optional.empty();
        }

        double value = Double.parseDouble(m.group("value"));
        if (!Double.isFinite(value)) {
            return Optional.empty();
        }

        String unit = m.group("unit");
        return Optional.of(new Quantity(value, unit == null ? null : unit.trim()));
    }

    public static Optional<Mass> parseMass(String text) {
        Optional<Quantity> q = parse(text);

        if (q.isEmpty()) {
            return Optional.empty();
        }
        if (q.get().unit() == null) {
            return Optional.of(new Mass(q.get().value(), null));
        }

        MassUnit unit = MASS_UNITS.get(q.get().unit().toLowerCase(Locale.ROOT));
        return unit == null ? Optional.empty() : Optional.of(new Mass(q.get().value(), unit));
    }

    public static OptionalDouble parseVelocity(String text) {
        Optional<Quantity> q = parse(text);
        if (q.isEmpty()) {
            return OptionalDouble.empty();
        }
        if (q.get().unit() == null) {
            return OptionalDouble.of(q.get().value());
        }

        Double factor = VELOCITY_UNITS.get(q.get().unit().toLowerCase(Locale.ROOT));
        return factor == null ? OptionalDouble.empty() : OptionalDouble.of(q.get().value() * factor);
    }
}
