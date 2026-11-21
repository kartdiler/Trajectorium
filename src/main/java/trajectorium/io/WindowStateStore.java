package trajectorium.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class WindowStateStore {
    private static final Logger log = LoggerFactory.getLogger(WindowStateStore.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Path file;

    public WindowStateStore(Path file) {
        this.file = file;
    }

    public Optional<WindowState> load() {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            WindowState state = MAPPER.readValue(file.toFile(), WindowState.class);
            if (state == null || !state.boundsAreValid()) {
                log.warn("Состояние окна в {} недопустимо и игнорируется", file);
                return Optional.empty();
            }
            return Optional.of(state);
        } catch (IOException e) {
            log.warn("Не удалось прочитать состояние окна {}", file, e);
            return Optional.empty();
        }
    }

    public void save(WindowState state) {
        if (!state.boundsAreValid()) {
            log.debug("Состояние окна не сохранено: размеры не допустимы ({})", state);
            return;
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            MAPPER.writeValue(file.toFile(), state);
        } catch (IOException e) {
            log.warn("Не удалось сохранить состояние окна {}", file, e);
        }
    }
}
