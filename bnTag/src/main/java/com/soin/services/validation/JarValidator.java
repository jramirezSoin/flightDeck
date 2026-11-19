package com.soin.services.validation;

import java.util.List;
import java.util.jar.JarFile;

/**
 * Reglas de validación de un JAR ya copiado al release. Hay una implementación
 * por tipo (OSB / SOA); cada regla nueva se agrega dentro de su validador.
 */
public interface JarValidator {

    /** Devuelve los errores encontrados; lista vacía si el JAR es válido. */
    List<String> validate(JarFile jar);
}
