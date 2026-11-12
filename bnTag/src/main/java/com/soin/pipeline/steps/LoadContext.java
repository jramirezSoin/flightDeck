package com.soin.pipeline.steps;


import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.config.Config;

public class LoadContext implements PipelineStep {

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

            System.out.println("Cargando contexto...");

            Config.loadProperties();

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

            System.out.println("OSB Path: " + context.getOsbPath());
            System.out.println("SOA Path: " + context.getSoaPath());
            System.out.println("Local File: " + context.getLocalFile());
            System.out.println("Host: " + context.getHost() + ":" + context.getPort());
            System.out.println("Script Dir: " + context.getScriptDir());
            System.out.println("Username: " + context.getUsername());
            System.out.println("Remote File: " + context.getRemoteFile());
            System.out.println("Contexto cargado correctamente");

            return StepResult.success("Contexto cargado correctamente");

        } catch (Exception e) {

            return StepResult.failed(
                "Error cargando contexto: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

}
