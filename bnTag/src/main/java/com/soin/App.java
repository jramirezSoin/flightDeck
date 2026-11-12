package com.soin;

import com.soin.pipeline.Pipeline;
import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.steps.*;



/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {


        PipelineContext context = new PipelineContext();

        try {            

            for (int i = 0; i < args.length; i++) {

                switch (args[i]) {

                    case "-osb":
                        context.setOSB(true);
                        context.setArchivo(args[++i]);
                        break;

                    case "-soa":
                        context.setSOA(true);
                        context.setArchivo(args[++i]);
                        break;

                    case "--project":
                        context.setProject(true);
                        break;

                    case "--skipCompile":
                        context.setCompile(false);
                        break;

                    case "--skipImport":
                        context.setImport(false);
                        break;

                    case "--create":
                        context.setCreate(true);
                        break;

                    case "--rest":
                        context.setRest(true);
                        break;

                    default:
                        System.out.println("Parámetro desconocido: " + args[i]);
                        System.out.println("Uso: bntag.sh [-osb <OSB name>] [-soa <SOA name>] [--project] [--skipCompile] [--skipImport]");
                        System.out.println("--project: Indica que se está ejecutando un OSB completo y no solo resources individuales de git status");
                        System.out.println("--skipCompile: Omite la compilación durante la ejecución");
                        System.out.println("--skipImport: Omite la importación durante la ejecución");
                        System.out.println("--create: Indica que se debe crear el proyecto en el servidor remoto");
                        System.out.println("--rest: Indica que se debe crear ProxyService REST");
                        System.exit(1);
                        return;
                }
            }
            
            Pipeline pipeline = new Pipeline();

            if(context.isSOA()) {
                pipeline
                    .addStep(new LoadContext())
                    .addStep(new SOAUploadFileStep())
                    .addStep(new SOAImport());
            } 
            else if(context.isOSB() && context.isCreate()) {
                pipeline
                    .addStep(new LoadContext())
                    .addStep(new CreateOSBStep());
            }
            else if(context.isOSB()) {
            pipeline
                .addStep(new LoadContext())
                .addStep(new GitStatusStep())
                .addStep(new OSBCompile())
                .addStep(new OSBUploadFileStep())
                .addStep(new OSBImport());
            } else {
                System.out.println("No se especificó ni un OSB ni un SOA para ejecutar el pipeline");
                System.exit(1);
                return;
            }

            if (!pipeline.execute(context)) {
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
