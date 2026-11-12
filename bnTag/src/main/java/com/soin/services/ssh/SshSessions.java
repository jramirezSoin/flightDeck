package com.soin.services.ssh;

import java.io.File;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/** Crea sesiones SSH con la verificación de host key configurada en un solo lugar. */
final class SshSessions {

    private SshSessions() {
    }

    /**
     * Crea (sin conectar) una sesión SSH.
     *
     * @param knownHosts ruta del archivo known_hosts. Si es null/vacío NO se verifica la
     *                   huella del servidor (StrictHostKeyChecking=no, vulnerable a
     *                   man-in-the-middle). Si se indica, la huella debe estar registrada
     *                   en ese archivo (StrictHostKeyChecking=yes).
     */
    static Session create(
            String host,
            int port,
            String username,
            String privateKey,
            String knownHosts) throws Exception {

        JSch jsch = new JSch();
        jsch.addIdentity(privateKey);

        Session session;

        if (knownHosts != null && !knownHosts.trim().isEmpty()) {
            File file = new File(knownHosts.trim());

            if (!file.isFile()) {
                throw new IllegalStateException(
                    "server.known.hosts apunta a un archivo que no existe: " + file.getAbsolutePath()
                    + " (regístralo antes con: ssh-keyscan " + host + " >> <ruta>)");
            }

            jsch.setKnownHosts(file.getAbsolutePath());
            session = jsch.getSession(username, host, port);
            session.setConfig("StrictHostKeyChecking", "yes");

        } else {
            System.out.println("AVISO: server.known.hosts no está definida; no se verifica la huella del servidor SSH");
            session = jsch.getSession(username, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
        }

        return session;
    }
}
