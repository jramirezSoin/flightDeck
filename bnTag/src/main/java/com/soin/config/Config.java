package com.soin.config;

import java.util.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final Logger LOG = Logger.getLogger(Config.class.getName());

    public static Properties prop = new Properties();
    final static String propertiesFilePath = "config/application.properties";

    /**
     * Carga application.properties.
     *
     * @throws IOException si el archivo no existe o no se puede leer
     */
    public static void loadProperties() throws IOException {
        File file = new File(propertiesFilePath);
        LOG.info("Cargando propiedades desde: " + file.getAbsolutePath());

        try (InputStream input = new FileInputStream(file)) {
            prop.load(input);
        } catch (IOException e) {
            throw new IOException(
                    "No se pudo leer " + file.getAbsolutePath()
                    + " (¿se generó con generate-config.sh?): " + e.getMessage(), e);
        }
    }

}
