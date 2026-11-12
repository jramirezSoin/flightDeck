package com.soin.services.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SshCommand {

    /** Entrecomilla un argumento para el shell remoto (comillas simples, escapando las internas). */
    public static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    public static String execute(
            String host,
            int port,
            String username,
            String privateKey,
            String command) throws Exception {

        JSch jsch = new JSch();
        jsch.addIdentity(privateKey);

        Session session = jsch.getSession(username, host, port);

        // Para pruebas
        session.setConfig("StrictHostKeyChecking", "no");

        session.connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        // STDOUT en tiempo real + guardar resultado
        channel.setOutputStream(new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                output.write(b);
                System.out.print((char) b);
                System.out.flush();
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                output.write(b, off, len);
                System.out.print(new String(b, off, len, StandardCharsets.UTF_8));
                System.out.flush();
            }
        });

        // STDERR en tiempo real + guardar resultado
        channel.setErrStream(new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                error.write(b);
                System.err.print((char) b);
                System.err.flush();
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                error.write(b, off, len);
                System.err.print(new String(b, off, len, StandardCharsets.UTF_8));
                System.err.flush();
            }
        });

        channel.setCommand(command);
        channel.connect();

        while (!channel.isClosed()) {
            Thread.sleep(100);
        }

        int exitCode = channel.getExitStatus();

        channel.disconnect();
        session.disconnect();

        String stdout = output.toString(StandardCharsets.UTF_8.name());
        String stderr = error.toString(StandardCharsets.UTF_8.name());

        if (exitCode != 0) {
            throw new RuntimeException(
                    "Comando falló. Exit code: " + exitCode
                    + "\nSTDERR: " + stderr
            );
        }

        return stdout;
    }
}