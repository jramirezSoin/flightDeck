package com.soin.services.osb;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;

public class ConfigJarXmlGenerator {

        private static final String NAMESPACE = "http://www.bea.com/alsb/tools/configjar/config";

        public static void generate(
                        String projectDir,
                        String jarPath,
                        List<String> resourceName,
                        boolean isProject,
                        String outputFile) throws Exception {

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document document = factory.newDocumentBuilder().newDocument();

                Element configjarSettings = document.createElementNS(NAMESPACE, "configjarSettings");
                document.appendChild(configjarSettings);

                Element source = document.createElement("source");
                configjarSettings.appendChild(source);

                Element project = document.createElement("project");
                project.setAttribute("dir", projectDir);
                source.appendChild(project);

                Element configjar = document.createElement("configjar");
                configjar.setAttribute("jar", jarPath);
                configjarSettings.appendChild(configjar);

                Element resourceLevel = document.createElement("resourceLevel");
                configjar.appendChild(resourceLevel);

                if (!isProject) {
                        resourceLevel.setAttribute("includeDependencies", "false");
                        

                        Element resources = document.createElement("resources");
                        resourceLevel.appendChild(resources);

                        for (String name : resourceName) {
                                Element include = document.createElement("include");
                                include.setAttribute("name", name);
                                resources.appendChild(include);
                        }

                }

                saveXml(document, outputFile);

        }

        private static void saveXml(Document document, String outputFile) throws Exception {

                TransformerFactory transformerFactory = TransformerFactory.newInstance();

                Transformer transformer = transformerFactory.newTransformer();

                transformer.setOutputProperty(
                                OutputKeys.INDENT, "yes");

                transformer.setOutputProperty(
                                OutputKeys.ENCODING, "UTF-8");

                // Para controlar el número de espacios de indentación
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                transformer.transform(
                                new DOMSource(document),
                                new StreamResult(new File(outputFile)));

        }
}