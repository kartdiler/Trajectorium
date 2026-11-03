package trajectorium.io;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import trajectorium.i18n.I18n;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SceneStorage {
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SceneStorage() {}

    public static void save(Path file, SceneData data) throws IOException, SceneFormatException {
        SceneValidator.validate(data);
        MAPPER.writeValue(file.toFile(), data);
    }

    public static SceneData load(Path file) throws IOException, SceneFormatException {
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new SceneFormatException(I18n.get("error.file.large"));
        } try (InputStream in = Files.newInputStream(file)) {
            return parse(in);
        }
    }

    public static SceneData loadResource(String path) throws IOException, SceneFormatException {
        try (InputStream in = SceneStorage.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(I18n.format("error.resource.missing", path));
            }
            return parse(in);
        }
    }

    private static SceneData parse(InputStream in) throws IOException, SceneFormatException {
        SceneData data;
        try {
            data = MAPPER.readValue(in, SceneData.class);
        } catch (JsonProcessingException e) {
            JsonLocation loc = e.getLocation();
            String message = (loc == null || loc.getLineNr() < 0) ? I18n.get("error.file.broken")
                    : I18n.format("error.file.broken.at", loc.getLineNr(), loc.getColumnNr());
            throw new SceneFormatException(message, e);
        }

        SceneValidator.validate(data);
        return data;
    }
}
