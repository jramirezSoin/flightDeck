package com.soin.services.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/** Validaciones del JAR de un OSB. Por ahora sin reglas: se agregan aquí poco a poco. */
public class OsbJarValidator implements JarValidator {

    @Override
    public List<String> validate(JarFile jar) {
        List<String> errors = new ArrayList<>();
        return errors;
    }
}
