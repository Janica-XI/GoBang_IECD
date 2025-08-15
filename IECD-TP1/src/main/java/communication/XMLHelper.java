package communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * XMLHelper provides utility methods for creating, reading, writing, and
 * validating XML documents according to the protocol XSD.
 * <p>
 * Messages are framed with a 4-byte big-endian length prefix so that readers
 * know exactly how many bytes to consume for each XML document.
 */
public class XMLHelper {

	/**
	 * Appends a text element <tagName>textContent</tagName> to the specified parent
	 * element.
	 *
	 * @param document      the Document to which elements belong
	 * @param parentElement the parent Element to which the new element will be
	 *                      appended
	 * @param tagName       the tag name of the new element
	 * @param textContent   the text content to set on the new element
	 */
	public static void appendTextElement(Document document, Element parentElement, String tagName, String textContent) {
		Element newElement = document.createElement(tagName);
		newElement.setTextContent(textContent);
		parentElement.appendChild(newElement);
	}

	/**
	 * Creates a new XML Document with the root element <Protocolo> and a single
	 * child element named elementName.
	 *
	 * @param elementName the name of the payload element to create
	 * @return a new Document containing <Protocolo><elementName></Protocolo>
	 */
	public static Document createMessageDocument(String elementName) {
		try {
			var documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement("Protocolo");
			document.appendChild(rootElement);
			Element payloadElement = document.createElement(elementName);
			rootElement.appendChild(payloadElement);
			return document;
		} catch (Exception exception) {
			throw new RuntimeException("Error creating XML document for element: " + elementName, exception);
		}
	}

	/**
	 * Converts a W3C Document into its XML text form.
	 *
	 * @param document the XML Document to serialize
	 * @return the XML as a String (utf-8, with declaration)
	 * @throws RuntimeException on any transform error
	 */
	public static String documentToString(Document document) {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			// optional: indent
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			// ensure declaration
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException("Error serializing XML document", e);
		}
	}

	/**
	 * Retrieves the text content of the first child element with the given tag
	 * name.
	 *
	 * @param parentElement the Element from which to retrieve the text
	 * @param tagName       the tag name of the child element
	 * @return the text content, or null if the element is not found
	 */
	public static String getElementTextContent(Element parentElement, String tagName) {
		NodeList nodeList = parentElement.getElementsByTagName(tagName);
		return (nodeList.getLength() > 0) ? nodeList.item(0).getTextContent() : null;
	}

	/**
	 * Reads one length-prefixed XML message from the InputStream, parses it into a
	 * Document, and returns it.
	 *
	 * @param inputStream the InputStream containing length-prefixed XML data
	 * @return the parsed XML Document
	 * @throws RuntimeException if an I/O or parsing error occurs
	 */
	public static Document readDocumentFromStream(InputStream inputStream) {
		try {
			DataInputStream dataInput = new DataInputStream(inputStream);

			// Read 4-byte big-endian length
			int xmlLength = dataInput.readInt();

			// Read exactly xmlLength bytes
			byte[] xmlBytes = new byte[xmlLength];
			dataInput.readFully(xmlBytes);

			// Parse from the raw bytes
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			return documentBuilder.parse(new ByteArrayInputStream(xmlBytes));
		} catch (Exception exception) {
			// Add this check for cleaner logging
			if (exception instanceof java.net.SocketException
					|| (exception.getCause() instanceof java.net.SocketException)) {
				throw new RuntimeException("Connection closed", exception);
			}
			throw new RuntimeException("Error reading length-prefixed XML from input stream", exception);
		}
	}

	/**
	 * Validates the given XML Document against the protocolo XSD schema loaded from
	 * the classpath resource “protocolo.xsd”.
	 *
	 * @param document the Document to validate
	 * @return true if the document is valid, false otherwise
	 */
	public static boolean validateDocumentAgainstXsd(Document document) {
		try (InputStream xsdStream = XMLHelper.class.getClassLoader().getResourceAsStream("protocolo.xsd")) {
			if (xsdStream == null) {
				throw new RuntimeException("XSD not found on classpath: protocolo.xsd");
			}
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));
			Validator validator = schema.newValidator();
			validator.validate(new DOMSource(document));
			return true;
		} catch (Exception exception) {
			return false;
		}
	}

	/**
	 * Writes the given XML Document to the specified OutputStream, prefixed by a
	 * 4-byte length header, omitting the XML declaration, and then flushes the
	 * stream.
	 *
	 * @param document     the Document to write
	 * @param outputStream the OutputStream to which the XML will be written
	 * @throws RuntimeException if an I/O or transformation error occurs
	 */
	public static void writeDocumentToStream(Document document, OutputStream outputStream) {
		try {
			// First, transform Document to bytes
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(document), new StreamResult(byteBuffer));
			byte[] xmlBytes = byteBuffer.toByteArray();

			// Write length prefix + XML payload
			DataOutputStream dataOutput = new DataOutputStream(outputStream);
			dataOutput.writeInt(xmlBytes.length);
			dataOutput.write(xmlBytes);
			dataOutput.flush();
		} catch (Exception exception) {
			throw new RuntimeException("Error writing length-prefixed XML to output stream", exception);
		}
	}
}