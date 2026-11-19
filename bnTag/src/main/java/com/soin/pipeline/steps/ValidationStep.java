package com.soin.pipeline.steps;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.validation.JarValidator;
import com.soin.services.validation.OsbJarValidator;
import com.soin.services.validation.SoaJarValidator;

/**
 * Valida el JAR que ReleaseStep copió al release. Por ahora imprime su contenido;
 * las reglas de cada tipo (OSB / SOA) viven en services/validation.
 */
public class ValidationStep implements PipelineStep {

    private static final Logger LOG = Logger.getLogger(ValidationStep.class.getName());

    @Override
    public StepResult execute(PipelineContext context) {
        String jarPath = context.getReleasedJar();
        if (jarPath == null) {
            return StepResult.skipped("No se copió ningún JAR al release; nada que validar");
        }

        JarValidator validator = context.isOSB() ? new OsbJarValidator() : new SoaJarValidator();

        try (JarFile jar = new JarFile(jarPath)) {
            LOG.info("Contenido de " + jarPath + " (" + jar.size() + " archivos)");
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                LOG.info("  " + entries.nextElement().getName());
            }

            List<String> errors = validator.validate(jar);
            String controlNote = writeControlFile(context, jarPath, errors);
            if (!errors.isEmpty()) {
                return StepResult.failed("Validación con errores: " + String.join("; ", errors) + controlNote);
            }
            return StepResult.success("Validación ejecutada correctamente (" + jar.size() + " archivos)");

        } catch (Exception e) {
            return StepResult.failed(
                "Error ejecutando Validación: "
                + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    /**
     * Deja los errores en <release>/control/<nombre del jar>.error (un error por línea).
     * Sin errores borra el archivo de una ejecución anterior para que no quede desactualizado.
     * Un problema al escribir no cambia el resultado de la validación; solo se avisa.
     *
     * @return texto para agregar al mensaje del step ("" si no hay nada que avisar)
     */
    private String writeControlFile(PipelineContext context, String jarPath, List<String> errors) {
        try {
            String jarName = Paths.get(jarPath).getFileName().toString();
            String base = jarName.toLowerCase().endsWith(".jar")
                    ? jarName.substring(0, jarName.length() - ".jar".length())
                    : jarName;
            Path controlDir = Paths.get(context.getReleaseDir(), context.getRelease(), "control");
            Path errorFile = controlDir.resolve(base + ".error");

            if (errors.isEmpty()) {
                Files.deleteIfExists(errorFile);
                return "";
            }
            Files.createDirectories(controlDir);
            Files.write(errorFile, errors, StandardCharsets.UTF_8);
            LOG.info("Errores guardados en " + errorFile);
            return " (detalle en " + errorFile + ")";
        } catch (IOException e) {
            LOG.warning("No se pudo escribir el archivo de control: " + e.getMessage());
            return " (no se pudo escribir el archivo de control: " + e.getMessage() + ")";
        }
    }
}
