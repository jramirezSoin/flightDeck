package com.soin;

import com.soin.pipeline.Pipeline;
import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.steps.*;

/**
 * Punto de entrada de bnTag: arma el pipeline según los parámetros y lo ejecuta.
 * Termina con exit code 1 si los parámetros son inválidos o algún paso falla.
 */
public class App {

    private static void printUsage() {
        System.out.println("Uso: bntag.sh -osb <OSB name> [--project] [--skipCompile] [--skipImport]");
        System.out.println("     bntag.sh -osb <OSB name> --create [--rest]");
        System.out.println("     bntag.sh -soa <SOA name> [--skipImport]");
        System.out.println("-osb <nombre>: Proyecto OSB a procesar");
        System.out.println("-soa <nombre>: Proyecto SOA a procesar");
        System.out.println("--project: Indica que se está ejecutando un OSB completo y no solo resources individuales de git status");
        System.out.println("--skipCompile: Omite la compilación durante la ejecución");
        System.out.println("--skipImport: Omite la importación durante la ejecución");
        System.out.println("--create: Crea un proyecto OSB nuevo en el workspace local a partir de la plantilla");
        System.out.println("--rest: Con --create, incluye el ProxyService REST");
    }

    private static void fail(String message) {
        System.out.println(message);
        printUsage();
        System.exit(1);
    }

    public static void main(String[] args) {

        PipelineContext context = new PipelineContext();

        try {

            for (int i = 0; i < args.length; i++) {

                switch (args[i]) {

                    case "-osb":
                        if (i + 1 >= args.length) {
                            fail("Falta el nombre del OSB después de -osb");
                        }
                        context.setOSB(true);
                        context.setArchivo(args[++i]);
                        break;

                    case "-soa":
                        if (i + 1 >= args.length) {
                            fail("Falta el nombre del SOA después de -soa");
                        }
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
                        fail("Parámetro desconocido: " + args[i]);
                }
            }

            if (context.isOSB() && context.isSOA()) {
                fail("No se puede usar -osb y -soa en la misma ejecución");
            }

            Pipeline pipeline = new Pipeline();

            if (context.isSOA()) {
                pipeline
                    .addStep(new LoadContext())
                    .addStep(new UploadFileStep(DeployTarget.SOA))
                    .addStep(new ImportStep(DeployTarget.SOA));
            }
            else if (context.isOSB() && context.isCreate()) {
                pipeline
                    .addStep(new LoadContext())
                    .addStep(new CreateOSBStep());
            }
            else if (context.isOSB()) {
                pipeline
                    .addStep(new LoadContext())
                    .addStep(new GitStatusStep())
                    .addStep(new OSBCompile())
                    .addStep(new UploadFileStep(DeployTarget.OSB))
                    .addStep(new ImportStep(DeployTarget.OSB));
            } else {
                fail("No se especificó ni un OSB ni un SOA para ejecutar el pipeline");
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
