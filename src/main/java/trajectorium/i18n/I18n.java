package trajectorium.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class I18n {
    public static final Locale RU = Locale.forLanguageTag("ru");
    public static final Locale EN = Locale.ENGLISH;

    private static final Logger log = LoggerFactory.getLogger(I18n.class);
    private static final String BUNDLE_NAME = "i18n.messages";
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<Runnable>();

    private static Locale locale = RU;
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, RU);

    private I18n() {}

    public static Locale getLocale() {
        return locale;
    }

    public static void setLocale(Locale newLocale) {
        if (newLocale.getLanguage().equals(locale.getLanguage())) {
            return;
        }
        locale = newLocale;
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, newLocale);
        log.info("Язык интерфейса: {}", newLocale.getLanguage());
        for (Runnable l : listeners) {
            try {
                l.run();
            } catch (RuntimeException e) {
                log.error("Ошибка при смене языка", e);
            }
        }
    }

    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            log.warn("Нет строки для ключа {}", key);
            return "!" + key + "!";
        }
    }

    public static String format(String key, Object... args) {
        try {
            return String.format(locale, get(key), args);
        } catch (IllegalFormatException e) {
            log.error("Неверный шаблон для ключа {}", key, e);
            return "!" + key + "!";
        }
    }
}
