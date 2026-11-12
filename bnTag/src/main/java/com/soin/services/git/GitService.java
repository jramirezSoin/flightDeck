package com.soin.services.git;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class GitService {

    /**
     * Devuelve los recursos con cambios (git status) que pertenecen al OSB indicado,
     * con el formato de include de ConfigJar: "**" + "/ruta/dentro/del/osb".
     *
     * Reglas:
     * - El OSB debe coincidir con un segmento completo de la ruta (no por substring).
     * - Renombrados: se usa la ruta nueva.
     * - Archivos borrados: se omiten (no se pueden exportar).
     *
     * @throws Exception si git no se puede ejecutar o termina con error
     */
    public static ArrayList<String> getStatus(String gitPath, String OSB) throws Exception {
        System.out.println("Git Path: " + gitPath);
        System.out.println("OSB: " + OSB);

        ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain", "-z", "--untracked-files=all");
        pb.directory(new File(gitPath));
        // El error de git va directo a la consola; evita bloqueos por buffer lleno.
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();

        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                raw.write(buf, 0, n);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(
                    "git status falló (exit code " + exitCode + ") en " + gitPath
                    + ". Revisa el mensaje de error de git en la salida.");
        }

        // Con -z las entradas van separadas por NUL y las rutas no se entrecomillan.
        String[] entries = new String(raw.toByteArray(), StandardCharsets.UTF_8).split("\0");

        ArrayList<String> statusLines = new ArrayList<>();

        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];

            if (entry.length() < 4) {
                continue;
            }

            char x = entry.charAt(0);
            char y = entry.charAt(1);
            String path = entry.substring(3);

            // Renombrado/copia: la siguiente entrada es la ruta original, se descarta.
            if (x == 'R' || y == 'R' || x == 'C' || y == 'C') {
                i++;
            }

            if (x == 'D' || y == 'D') {
                System.out.println("Omitido (borrado): " + path);
                continue;
            }

            String[] segments = path.split("/");
            int k = Arrays.asList(segments).indexOf(OSB);
            if (k < 0 || k == segments.length - 1) {
                continue;
            }

            String resource = "**/" + String.join("/", Arrays.copyOfRange(segments, k + 1, segments.length));
            System.out.println(resource);
            statusLines.add(resource);
        }

        return statusLines;
    }

}
