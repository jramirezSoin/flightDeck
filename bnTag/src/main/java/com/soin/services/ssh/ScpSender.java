package com.soin.services.ssh;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

public class ScpSender {

    /** Tiempo máximo para establecer la conexión SSH. */
    private static final int CONNECT_TIMEOUT_MS = 30_000;

    public static void sendFile(
            String host,
            int port,
            String username,
            String privateKey,
            String knownHosts,
            String localFile,
            String remoteFile) throws Exception {

        Session session = SshSessions.create(host, port, username, privateKey, knownHosts);

        ChannelSftp channel = null;

        try {
            session.connect(CONNECT_TIMEOUT_MS);

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CONNECT_TIMEOUT_MS);

            channel.put(localFile, remoteFile);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            session.disconnect();
        }
    }
}
