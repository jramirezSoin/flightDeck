package com.soin.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Configuración de logging de bnTag (java.util.logging, sin dependencias externas).
 *
 * - Consola: INFO sale por stdout tal cual el mensaje; WARNING/SEVERE por stderr con
 *   prefijo "AVISO:" / "ERROR:" y el stack trace si hay excepción.
 * - Archivo (opcional, ver {@link #addFile}): fecha, nivel y mensaje en cada línea.
 *
 * Cada clase obtiene su logger con Logger.getLogger(Clase.class.getName()); todos
 * cuelgan de "com.soin", que es el logger que se configura aquí.
 */
public final class Logging {

    private static final Logger ROOT = Logger.getLogger("com.soin");

    /** Logger que escribe solo al archivo (nunca a consola). Ver {@link #fileOnly}. */
    private static final Logger FILE_ONLY = Logger.getLogger("bntag.file");

    private static Handler fileHandler;

    private Logging() {
    }

    /** Configura la salida por consola. Se puede llamar más de una vez. */
    public static synchronized void configure() {
        ROOT.setUseParentHandlers(false);
        ROOT.setLevel(Level.INFO);
        removeHandlers(ROOT);

        ConsoleFormatter formatter = new ConsoleFormatter();

        Handler out = new PrintStreamHandler(System.out, formatter);
        out.setFilter(new Filter() {
            @Override
            public boolean isLoggable(LogRecord record) {
                return record.getLevel().intValue() < Level.WARNING.intValue();
            }
        });

        Handler err = new PrintStreamHandler(System.err, formatter);
        err.setLevel(Level.WARNING);

        ROOT.addHandler(out);
        ROOT.addHandler(err);

        // Si ya había un archivo abierto, mantenerlo
        if (fileHandler != null) {
            ROOT.addHandler(fileHandler);
        }

        FILE_ONLY.setUseParentHandlers(false);
        FILE_ONLY.setLevel(Level.INFO);
    }

    /**
     * Además de la consola, escribe el log en un archivo (se agrega al final si ya existe).
     *
     * @throws IOException si no se puede crear o abrir el archivo
     */
    public static synchronized void addFile(String path) throws IOException {
        File file = new File(path).getAbsoluteFile();

        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("No se pudo crear el directorio " + parent);
        }

        if (fileHandler != null) {
            ROOT.removeHandler(fileHandler);
            FILE_ONLY.removeHandler(fileHandler);
            fileHandler.close();
        }

        fileHandler = new FileLogHandler(file);
        ROOT.addHandler(fileHandler);
        FILE_ONLY.addHandler(fileHandler);

        FILE_ONLY.info("---------------- nueva ejecución ----------------");
    }

    /**
     * Escribe un texto solo en el archivo de log (no hace nada si no hay archivo).
     * Sirve para la salida del servidor remoto, que ya se muestra en consola en vivo.
     */
    public static void fileOnly(String message) {
        FILE_ONLY.info(message);
    }

    private static void removeHandlers(Logger logger) {
        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /** Formato de consola: el mensaje solo, con prefijo para avisos y errores. */
    private static class ConsoleFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            int level = record.getLevel().intValue();
            String prefix = level >= Level.SEVERE.intValue() ? "ERROR: "
                    : level >= Level.WARNING.intValue() ? "AVISO: "
                    : "";

            StringBuilder sb = new StringBuilder();
            sb.append(prefix).append(formatMessage(record)).append(System.lineSeparator());

            if (record.getThrown() != null) {
                sb.append(stackTrace(record.getThrown()));
            }
            return sb.toString();
        }
    }

    /** Formato de archivo: fecha, nivel y mensaje. */
    private static class FileFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            // SimpleDateFormat no es thread-safe: se crea uno por registro.
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis()));

            StringBuilder sb = new StringBuilder();
            sb.append(date).append(' ')
              .append(String.format("%-7s", record.getLevel().getName())).append(' ')
              .append(formatMessage(record)).append(System.lineSeparator());

            if (record.getThrown() != null) {
                sb.append(stackTrace(record.getThrown()));
            }
            return sb.toString();
        }
    }

    /** Escribe en un PrintStream (conserva la codificación de System.out/System.err). */
    private static class PrintStreamHandler extends Handler {
        private final PrintStream stream;

        PrintStreamHandler(PrintStream stream, Formatter formatter) {
            this.stream = stream;
            setFormatter(formatter);
            setLevel(Level.ALL);
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }
            stream.print(getFormatter().format(record));
            stream.flush();
        }

        @Override
        public void flush() {
            stream.flush();
        }

        @Override
        public void close() {
            // System.out / System.err no se cierran
        }
    }

    /** Escribe en un archivo UTF-8, vaciando el buffer en cada registro. */
    private static class FileLogHandler extends Handler {
        private final Writer writer;

        FileLogHandler(File file) throws IOException {
            this.writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
            setFormatter(new FileFormatter());
            setLevel(Level.ALL);
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }
            try {
                writer.write(getFormatter().format(record));
                writer.flush();
            } catch (IOException e) {
                reportError("No se pudo escribir en el archivo de log", e, java.util.logging.ErrorManager.WRITE_FAILURE);
            }
        }

        @Override
        public synchronized void flush() {
            try {
                writer.flush();
            } catch (IOException e) {
                reportError("No se pudo vaciar el archivo de log", e, java.util.logging.ErrorManager.FLUSH_FAILURE);
            }
        }

        @Override
        public synchronized void close() {
            try {
                writer.close();
            } catch (IOException e) {
                reportError("No se pudo cerrar el archivo de log", e, java.util.logging.ErrorManager.CLOSE_FAILURE);
            }
        }
    }
}
