package trajectorium.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import trajectorium.CelestialFactory.MassUnit;

import static org.junit.jupiter.api.Assertions.*;

public class QuantityParserTest {

    @Test
    void normalizeRewritesHumanNotationWithGroups() {
        assertEquals("3.003e-6 Msun", QuantityParser.normalize("3,003 × 10^-6 Msun"));
        assertEquals("6.674e-11", QuantityParser.normalize("6.674x10^-11"));
        assertEquals("2e3", QuantityParser.normalize("2 · 10^3"));
        assertEquals("1.2e+5", QuantityParser.normalize("1,2 х 10^+5"));
        assertEquals("-5.5", QuantityParser.normalize("\u22125,5"));
        assertEquals("5 Mearth", QuantityParser.normalize("5\u00A0Mearth"));
    }

    @Test
    void parsesNumbersWithAndWithoutUnit() {
        assertEquals(5.0, QuantityParser.parse("5").orElseThrow().value(), 0.0);
        assertNull(QuantityParser.parse("5").orElseThrow().unit());
        assertEquals(-2.5, QuantityParser.parse("-2,5").orElseThrow().value(), 1e-12);
        assertEquals(0.5, QuantityParser.parse(".5").orElseThrow().value(), 1e-12);
        assertEquals(1e-3, QuantityParser.parse("1E-3").orElseThrow().value(), 1e-18);
        assertEquals("кг", QuantityParser.parse("2e24 кг").orElseThrow().unit());
    }

    @Test
    void massUnitsAreRecognisedInAnyWritingStyle() {
        assertEquals(MassUnit.EARTH_MASSES, QuantityParser.parseMass("5 Mearth").orElseThrow().unit());
        assertEquals(MassUnit.EARTH_MASSES, QuantityParser.parseMass("5Mearth").orElseThrow().unit());
        assertEquals(MassUnit.EARTH_MASSES, QuantityParser.parseMass("5 M_earth").orElseThrow().unit());
        assertEquals(MassUnit.EARTH_MASSES, QuantityParser.parseMass("5 Мземл").orElseThrow().unit());
        assertEquals(MassUnit.EARTH_MASSES, QuantityParser.parseMass("5 MEARTH").orElseThrow().unit());
        assertEquals(MassUnit.SOLAR_MASSES, QuantityParser.parseMass("1,5e-3 Msun").orElseThrow().unit());
        assertEquals(MassUnit.SOLAR_MASSES, QuantityParser.parseMass("2 Мсолн").orElseThrow().unit());
        assertEquals(MassUnit.KILOGRAMS, QuantityParser.parseMass("2E24 KG").orElseThrow().unit());

        QuantityParser.Mass noUnit = QuantityParser.parseMass("7").orElseThrow();
        assertEquals(7.0, noUnit.value(), 0.0);
        assertNull(noUnit.unit());
    }

    @Test
    void scientificNotationWithUnitIsParsed() {
        QuantityParser.Mass m = QuantityParser.parseMass("3,003 × 10^-6 Msun").orElseThrow();
        assertEquals(3.003e-6, m.value(), 1e-18);
        assertEquals(MassUnit.SOLAR_MASSES, m.unit());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "1.2.3", "5 5", "1e", "--1", "NaN", "Infinity", "1e999",
            "5 парсек", "2x100", "Mearth"})
    void invalidMassesAreRejected(String input) {
        assertTrue(QuantityParser.parseMass(input).isEmpty(), input);
    }

    @Test
    void nullIsRejected() {
        assertTrue(QuantityParser.parse(null).isEmpty());
        assertTrue(QuantityParser.parseVelocity(null).isEmpty());
    }

    @Test
    void velocityIsConvertedToAuPerYear() {
        assertEquals(6.28, QuantityParser.parseVelocity("6.28").orElseThrow(), 1e-12);
        assertEquals(6.28, QuantityParser.parseVelocity("6,28 а.е./год").orElseThrow(), 1e-12);
        assertEquals(29.78 / 4.74047, QuantityParser.parseVelocity("29,78 км/с").orElseThrow(), 1e-9);
        assertEquals(1000 / 4740.47, QuantityParser.parseVelocity("1000 m/s").orElseThrow(), 1e-9);
        assertEquals(-1.0, QuantityParser.parseVelocity("-1 au/yr").orElseThrow(), 1e-12);
    }

    @Test
    void velocityWithMassUnitIsRejected() {
        assertTrue(QuantityParser.parseVelocity("5 кг").isEmpty());
        assertTrue(QuantityParser.parseMass("5 км/с").isEmpty());
    }
}