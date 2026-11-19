package com.soin.services.validation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** Validaciones del JAR de un OSB. Las reglas se agregan aquí poco a poco. */
public class OsbJarValidator implements JarValidator {

    private static final Logger LOG = Logger.getLogger(OsbJarValidator.class.getName());

    private static final String BUSINESS_SERVICE_EXT = ".businessservice";
    private static final String HTTP_NS = "http://www.bea.com/wli/sb/transports/http";

    @Override
    public List<String> validate(JarFile jar) {
        List<String> errors = new ArrayList<>();

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(BUSINESS_SERVICE_EXT)) {
                validateBusinessService(jar, entry, errors);
            }
        }
        return errors;
    }

    /** Regla: http:chunked-streaming-mode, si existe, debe ser false. */
    private void validateBusinessService(JarFile jar, JarEntry entry, List<String> errors) {
        String name = entry.getName();
        try (InputStream in = jar.getInputStream(entry)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // El XML viene de un JAR: sin DTD ni entidades externas.
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(in);

            NodeList nodes = doc.getElementsByTagNameNS(HTTP_NS, "chunked-streaming-mode");
            if (nodes.getLength() == 0) {
                // Sin endpoint HTTP (otro transporte): la regla no aplica.
                LOG.info("Validación " + name + ": sin chunked-streaming-mode, no aplica");
                return;
            }
            for (int i = 0; i < nodes.getLength(); i++) {
                String value = nodes.item(i).getTextContent().trim();
                if (!"false".equals(value)) {
                    errors.add(name + ": chunked-streaming-mode es '" + value + "' y debe ser 'false'");
                } else {
                    LOG.info("Validación " + name + ": chunked-streaming-mode = false (OK)");
                }
            }
        } catch (Exception e) {
            errors.add(name + ": no se pudo leer el XML ("
                + (e.getMessage() != null ? e.getMessage() : e.toString()) + ")");
        }
    }
}
