package trajectorium.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class I18nTest {
    private static final Pattern SPEC = Pattern.compile("%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z%]");

    @AfterEach
    void restoreRussian() {
        I18n.setLocale(I18n.RU);
    }

    @Test
    void switchingLanguageChangesTextsAndNotifiesListeners() {
        AtomicInteger calls = new AtomicInteger();
        Runnable listener = calls::incrementAndGet;
        I18n.addListener(listener);
        try {
            I18n.setLocale(I18n.EN);
            assertEquals("Reset", I18n.get("control.reset"));
            I18n.setLocale(I18n.RU);
            assertEquals("Сброс", I18n.get("control.reset"));
            assertEquals(2, calls.get());
        } finally {
            I18n.removeListener(listener);
        }
    }

    @Test
    void settingTheSameLanguageDoesNotNotify() {
        AtomicInteger calls = new AtomicInteger();
        Runnable listener = calls::incrementAndGet;
        I18n.addListener(listener);
        try {
            I18n.setLocale(I18n.RU);
            assertEquals(0, calls.get());
        } finally {
            I18n.removeListener(listener);
        }
    }

    @Test
    void missingKeyIsMarkedInsteadOfThrowing() {
        assertEquals("!no.such.key!", I18n.get("no.such.key"));
    }

    @Test
    void numbersFollowTheLanguage() {
        assertTrue(I18n.format("control.speed", 1.5).contains("1,5"));
        I18n.setLocale(I18n.EN);
        assertTrue(I18n.format("control.speed", 1.5).contains("1.5"));
    }

    @Test
    void translationsHaveSameKeysAndPlaceholders() throws IOException {
        Properties ru = load("/i18n/messages.properties");
        Properties en = load("/i18n/messages_en.properties");

        assertEquals(ru.stringPropertyNames(), en.stringPropertyNames());
        for (String key : ru.stringPropertyNames()) {
            assertFalse(ru.getProperty(key).isBlank(), "пустое русское значение: " + key);
            assertFalse(en.getProperty(key).isBlank(), "пустое английское значение: " + key);
            assertEquals(placeholders(ru.getProperty(key)), placeholders(en.getProperty(key)),
                    "разные плейсхолдеры в " + key);
        }
    }

    private static Properties load(String path) throws IOException {
        Properties p = new Properties();
        try (InputStream in = I18nTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "не найден ресурс " + path);
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    private static List<String> placeholders(String text) {
        Matcher m = SPEC.matcher(text);
        List<String> found = new ArrayList<String>();
        while (m.find()) {
            found.add(m.group());
        }
        Collections.sort(found);
        return found;
    }
}
