package com.soin.pipeline.steps;

import java.io.IOException;
import java.util.logging.Logger;

import com.soin.logging.Logging;


import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.config.Config;

public class LoadContext implements PipelineStep {

    private static final Logger LOG = Logger.getLogger(LoadContext.class.getName());

    /** Devuelve el valor de una propiedad obligatoria o falla nombrándola. */
    private static String required(String key) {
        String value = Config.prop.getProperty(key);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                "Falta la propiedad requerida '" + key + "' en application.properties");
        }

        return value.trim();
    }

    @Override
    public StepResult execute(PipelineContext context) {

        try {

            LOG.info("Cargando contexto...");

            Config.loadProperties();

            // Opcional: además de la consola, guardar el log en un archivo
            String logFile = Config.prop.getProperty("log.file");
            if (logFile != null && !logFile.trim().isEmpty()) {
                try {
                    Logging.addFile(logFile.trim());
                    LOG.info("Log también en: " + logFile.trim());
                } catch (IOException e) {
                    // Un problema con el log no debe impedir la ejecución
                    LOG.warning("No se pudo abrir el archivo de log '" + logFile.trim() + "': " + e.getMessage());
                }
            }

            context.setOsbPath(required("osb.dir"));
            context.setSoaPath(required("soa.dir"));
            context.setLocalFile(required("dest.jar.dir"));
            context.setHost(required("server.host"));
            context.setUsername(required("server.user"));
            context.setPrivateKey(required("server.key"));
            context.setRemoteFile(required("server.remote.path"));
            context.setScriptDir(required("server.script.dir"));

            String port = required("server.port");
            try {
                context.setPort(Integer.parseInt(port));
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                    "server.port inválido: '" + port + "' (debe ser un número)");
            }

            // Opcional: si no se define, no se verifica la huella del servidor SSH
            String knownHosts = Config.prop.getProperty("server.known.hosts");
            if (knownHosts != null && !knownHosts.trim().isEmpty()) {
                context.setKnownHosts(knownHosts.trim());
            }

            LOG.info("OSB Path: " + context.getOsbPath());
            LOG.info("SOA Path: " + context.getSoaPath());
            LOG.info("Local File: " + context.getLocalFile());
            LOG.info("Host: " + context.getHost() + ":" + context.getPort());
            LOG.info("Script Dir: " + context.getScriptDir());
            LOG.info("Username: " + context.getUsername());
            LOG.info("Remote File: " + context.getRemoteFile());
            LOG.info("Contexto cargado correctamente");

            return StepResult.success("Contexto cargado correctamente");

        } catch (Exception e) {

            return StepResult.failed(
                "Error cargando contexto: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

}
