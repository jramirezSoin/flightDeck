package com.soin.services.osb;

import java.util.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ImportService {

    private static final Logger LOG = Logger.getLogger(ImportService.class.getName());

    public static void importConfigJar(String configXmlPath) throws Exception {

        Class<?> configJarClass =
                Class.forName("com.bea.alsb.tools.configjar.ConfigJar");

        Method mainMethod =
                configJarClass.getMethod("main", String[].class);

        String[] configJarArgs = {
            "-settingsfile",
            configXmlPath
        };

        try {
            mainMethod.invoke(null, (Object) configJarArgs);
        } catch (InvocationTargetException e) {
            // Relanzar el error real de ConfigJar, no el wrapper de reflection
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }

        LOG.info("Import exitoso.");
    }
}
