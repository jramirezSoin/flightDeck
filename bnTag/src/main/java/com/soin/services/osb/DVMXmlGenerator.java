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

public class DVMXmlGenerator {

        private static final String NAMESPACE = "http://www.bea.com/alsb/tools/configjar/config";

        public static void generate(
                        String projectName, String dvmNumber,
                        String outputFile) throws Exception {

                String[] values = {
                    dvmNumber,
                    projectName,
                    "1",
                    projectName,
                    projectName,
                    "CREDITO_DIGITAL",
                    "false",
                    "true",
                    "1",
                    "complex"
                };

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document document = factory.newDocumentBuilder().newDocument();

                Element row = document.createElement("row");
                document.appendChild(row);

                for (String value: values){
                    Element cell = document.createElement("cell");
                    cell.setTextContent(value);
                    row.appendChild(cell);
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