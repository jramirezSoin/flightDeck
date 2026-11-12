package com.soin.services.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class SshCommand {

    /** Tiempo máximo para establecer la conexión SSH. */
    private static final int CONNECT_TIMEOUT_MS = 30_000;

    /** Tiempo máximo que se espera a que termine el comando remoto (30 min). */
    private static final long COMMAND_TIMEOUT_MS = 30L * 60 * 1000;

    /** Entrecomilla un argumento para el shell remoto (comillas simples, escapando las internas). */
    public static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    public static String execute(
            String host,
            int port,
            String username,
            String privateKey,
            String knownHosts,
            String command) throws Exception {

        Session session = SshSessions.create(host, port, username, privateKey, knownHosts);

        ChannelExec channel = null;

        try {
            session.connect(CONNECT_TIMEOUT_MS);

            channel = (ChannelExec) session.openChannel("exec");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayOutputStream error = new ByteArrayOutputStream();

            // STDOUT/STDERR en tiempo real + guardar resultado
            channel.setOutputStream(new TeeOutputStream(output, System.out));
            channel.setErrStream(new TeeOutputStream(error, System.err));

            channel.setCommand(command);
            channel.connect();

            long deadline = System.currentTimeMillis() + COMMAND_TIMEOUT_MS;

            while (!channel.isClosed()) {
                if (System.currentTimeMillis() > deadline) {
                    throw new RuntimeException(
                            "Timeout: el comando remoto no terminó en "
                            + (COMMAND_TIMEOUT_MS / 60000) + " minutos");
                }
                Thread.sleep(100);
            }

            int exitCode = channel.getExitStatus();

            String stdout = output.toString(StandardCharsets.UTF_8.name());
            String stderr = error.toString(StandardCharsets.UTF_8.name());

            if (exitCode != 0) {
                throw new RuntimeException(
                        "Comando falló. Exit code: " + exitCode
                        + "\nSTDERR: " + stderr
                );
            }

            return stdout;

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            session.disconnect();
        }
    }

    /**
     * Escribe los bytes tal cual a un buffer y a una consola. Al pasar bytes crudos
     * (no chars) los caracteres UTF-8 multibyte se muestran bien aunque lleguen partidos.
     */
    private static class TeeOutputStream extends OutputStream {

        private final ByteArrayOutputStream buffer;
        private final PrintStream console;

        TeeOutputStream(ByteArrayOutputStream buffer, PrintStream console) {
            this.buffer = buffer;
            this.console = console;
        }

        @Override
        public void write(int b) throws IOException {
            buffer.write(b);
            console.write(b);
            console.flush();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            buffer.write(b, off, len);
            console.write(b, off, len);
            console.flush();
        }
    }
}
