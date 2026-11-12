package com.soin.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    public static Properties prop = new Properties();
    final static String propertiesFilePath = "config/application.properties";

    public static void loadProperties() {
        System.out.println("Cargando propiedades desde: " + propertiesFilePath);
        try (InputStream input = new FileInputStream(propertiesFilePath)) {
            prop.load(input);

        } catch (Exception e) {
            System.err.println("Error cargando propiedades: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
