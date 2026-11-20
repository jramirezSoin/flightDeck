package com.soin.services.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Información del release: <release>/control/release.json (nombre, descripción, fecha de creación).
 * Su existencia es lo que define que un release "existe" para poder instalar en él.
 */
public class ReleaseInfo {

    private static final Logger LOG = Logger.getLogger(ReleaseInfo.class.getName());

    public static Path file(String releaseDir, String name) {
        return Paths.get(releaseDir, name, "control", "release.json");
    }

    public static boolean exists(String releaseDir, String name) {
        return Files.isRegularFile(file(releaseDir, name));
    }

    /** Escribe release.json; si ya existe no lo toca (se conserva la fecha de creación original). */
    public static void writeIfAbsent(String releaseDir, String name, String description) throws IOException {
        Path target = file(releaseDir, name);
        if (Files.exists(target)) {
            LOG.info("release.json ya existe, no se modifica: " + target);
            return;
        }

        String json = "{\n"
                + "  \"name\": \"" + escape(name) + "\",\n"
                + "  \"description\": \"" + escape(description == null ? "" : description) + "\",\n"
                + "  \"createdAt\": \"" + LocalDateTime.now().withNano(0) + "\"\n"
                + "}\n";

        Files.createDirectories(target.getParent());
        Files.write(target, json.getBytes(StandardCharsets.UTF_8));
        LOG.info("Creado " + target);
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
