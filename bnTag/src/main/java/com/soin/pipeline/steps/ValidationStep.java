package com.soin.pipeline.steps;

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
            if (!errors.isEmpty()) {
                return StepResult.failed("Validación con errores: " + String.join("; ", errors));
            }
            return StepResult.success("Validación ejecutada correctamente (" + jar.size() + " archivos)");

        } catch (Exception e) {
            return StepResult.failed(
                "Error ejecutando Validación: "
                + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }
}
