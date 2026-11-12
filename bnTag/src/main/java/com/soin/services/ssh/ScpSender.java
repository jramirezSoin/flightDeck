package com.soin.services.ssh;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class ScpSender {

    public static void sendFile(
            String host,
            int port,
            String username,
            String privateKey,
            String localFile,
            String remoteFile) throws Exception {

        JSch jsch = new JSch();

        // Cargar id_rsa
        jsch.addIdentity(privateKey);

        Session session = jsch.getSession(username, host, port);

        // Solo para pruebas. En producción deberíamos validar known_hosts.
        session.setConfig("StrictHostKeyChecking", "no");

        session.connect();

        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        try {
            channel.put(localFile, remoteFile);
        } finally {
            channel.disconnect();
            session.disconnect();
        }
    }
}
