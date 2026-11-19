package com.soin.services.release;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class CreateRelease {

    private static final String TEMPLATE_PATH = "/templates/Release_Template";
    private static final String PLACEHOLDER_NAME = "{NAME}";

    public static void create(String releaseName, Path destination) throws IOException {

        // createTempDirectory no crea el directorio base; java.io.tmpdir (java_opts.txt) puede no existir
        // porque el lanzador lo limpia antes de cada ejecución.
        Files.createDirectories(Paths.get(System.getProperty("java.io.tmpdir")));
        Path tempDirectory = Files.createTempDirectory("osb-template-");

        try {
            copyResourceDirectory(TEMPLATE_PATH, tempDirectory);

            Files.walkFileTree(tempDirectory, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(
                        Path file, BasicFileAttributes attrs) throws IOException {

                    Path relativePath = tempDirectory.relativize(file);
                    Path renamed = replaceName(relativePath, releaseName);
                    Path target = destination.resolve(renamed);

                    Files.createDirectories(target.getParent());

                    byte[] content = Files.readAllBytes(file);

                    if (isTextFile(content)) {
                        String text = new String(content, StandardCharsets.UTF_8);
                        text = replacePlaceholders(text, releaseName);

                        Files.write(target, text.getBytes(StandardCharsets.UTF_8));
                    } else {
                        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(
                        Path dir, BasicFileAttributes attrs) throws IOException {

                    Path relative = tempDirectory.relativize(dir);
                    Path renamed = replaceName(relative, releaseName);

                    Files.createDirectories(destination.resolve(renamed));

                    return FileVisitResult.CONTINUE;
                }
            });

        } finally {
            deleteDirectory(tempDirectory);
        }
    }

    private static String replacePlaceholders(String value, String releaseName) {
        return value.replace(PLACEHOLDER_NAME, releaseName);
    }

    private static void copyResourceDirectory(String resourcePath, Path destination ) throws IOException {

        String indexPath = resourcePath + "/template.index";

        try (InputStream input = CreateRelease.class.getResourceAsStream(indexPath)) {

            if (input == null) {
                throw new IOException(
                        "No se encontró el índice del template: " + indexPath);
            }

            String[] entries = new String(readFully(input),
                    StandardCharsets.UTF_8).split("\\r?\\n");

            for (String entry : entries) {
                entry = entry.trim();

                if (entry.isEmpty()) {
                    continue;
                }

                String resource = resourcePath + "/" + entry;
                Path output = destination.resolve(entry);

                if (entry.endsWith("/")) {
                    Files.createDirectories(output);
                } else {
                    try (InputStream file = CreateRelease.class.getResourceAsStream(resource)) {

                        if (file == null) {
                            throw new IOException(
                                    "No se encontró el recurso: " + resource);
                        }

                        Files.createDirectories(output.getParent());
                        Files.copy(file, output,
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static boolean isTextFile(byte[] content) {
        // Evita tratar archivos binarios como texto.
        int limit = Math.min(content.length, 8192);

        for (int i = 0; i < limit; i++) {
            if (content[i] == 0) {
                return false;
            }
        }

        return true;
    }

    private static Path replaceName(Path relative, String releaseName) {
        Path result = relative.getRoot();

        for (Path part : relative) {
            String name = replacePlaceholders(part.toString(), releaseName);

            result = result == null
                    ? Paths.get(name)
                    : result.resolve(name);
        }

        return result == null ? Paths.get("") : result;
    }

    private static byte[] readFully(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

        byte[] data = new byte[8192];
        int bytesRead;

        while ((bytesRead = input.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }

        return buffer.toByteArray();
    }

    private static void deleteDirectory(Path directory)
            throws IOException {

        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attrs)
                    throws IOException {

                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(
                    Path dir, IOException exc) throws IOException {

                if (exc != null) {
                    throw exc;
                }

                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}