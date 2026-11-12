package com.soin.pipeline.steps;


import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.pipeline.StepStatus;
import com.soin.config.Config;

public class LoadContext implements PipelineStep {

@Override
    public StepResult execute(PipelineContext context) {

        try {

            System.out.println("Cargando contexto...");

            Config.loadProperties();

            context.setOsbPath(Config.prop.getProperty("osb.dir"));
            context.setSoaPath(Config.prop.getProperty("soa.dir"));
            context.setLocalFile(Config.prop.getProperty("dest.jar.dir"));
            context.setHost(Config.prop.getProperty("server.host"));
            context.setUsername(Config.prop.getProperty("server.user"));
            context.setPrivateKey(Config.prop.getProperty("server.key"));
            context.setRemoteFile(Config.prop.getProperty("server.remote.path"));
            context.setCommandPath(Config.prop.getProperty("server.script.path"));

            String scriptDir = Config.prop.getProperty("server.script.dir");
            if (scriptDir == null || scriptDir.trim().isEmpty()) {
                return new StepResult(
                    StepStatus.FAILED,
                    "Falta la propiedad requerida 'server.script.dir' en application.properties"
                );
            }
            context.setScriptDir(scriptDir.trim());

            String port = Config.prop.getProperty("server.port");
            if (port == null || port.trim().isEmpty()) {
                return new StepResult(
                    StepStatus.FAILED,
                    "Falta la propiedad requerida 'server.port' en application.properties"
                );
            }
            try {
                context.setPort(Integer.parseInt(port.trim()));
            } catch (NumberFormatException e) {
                return new StepResult(
                    StepStatus.FAILED,
                    "server.port inválido: '" + port.trim() + "' (debe ser un número)"
                );
            }

            System.out.println("OSB Path: " + context.getOsbPath());
            System.out.println("SOA Path: " + context.getSoaPath());
            System.out.println("Local File: " + context.getLocalFile());
            System.out.println("Host: " + context.getHost() + ":" + context.getPort());
            System.out.println("Script Dir: " + context.getScriptDir());
            System.out.println("Username: " + context.getUsername());
            System.out.println("Remote File: " + context.getRemoteFile());
            System.out.println("Command Path: " + context.getCommandPath());
            System.out.println("Contexto cargado correctamente");

            return new StepResult(
                StepStatus.SUCCESS,
                "Contexto cargado correctamente"
            );

            

        } catch (Exception e) {

            return new StepResult(
                StepStatus.FAILED,
                "Error cargando contexto: " + e.getMessage()
            );
        }
    }
    
}
