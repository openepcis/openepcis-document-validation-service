package io.openepcis.core.xml;

import io.openepcis.core.exception.SchemaValidationException;

import javax.xml.stream.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventModifier {
  private final ExecutorService executorService;
  private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
  private final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
  private static final List<String> EPCIS_EVENT_TYPES =
      Arrays.asList(
          "ObjectEvent",
          "AggregationEvent",
          "TransactionEvent",
          "TransformationEvent",
          "AssociationEvent");

  private static final String EPCIS_DOCUMENT_NAMESPACE = "urn:epcglobal:epcis:xsd:2";
  private static final String EPCIS = "epcis";

  public EventModifier(final ExecutorService executorService) {
    this.executorService = executorService;
  }

  public EventModifier() {
    this(Executors.newWorkStealingPool());
  }
  public InputStream modifyEvent(final InputStream captureInput) {
    try {
      final byte[] preScan = new byte[64];
      final int len = captureInput.read(preScan);
      final String preScanType = new String(preScan, StandardCharsets.UTF_8);

      final PipedOutputStream pipedOutputStream = new PipedOutputStream();
      final PipedInputStream pipe = new PipedInputStream(pipedOutputStream);
      pipedOutputStream.write(preScan, 0, len);

      executorService.execute(
              () -> {
                try {
                  copy(captureInput, pipedOutputStream);
                } catch (Exception e) {
                  throw new SchemaValidationException(
                          "Exception occurred during reading of schema version from input document : "
                                  + e.getMessage(),
                          e);
                }
              });

      if (preScanType.contains("epcis:EPCISDocument")) {
        return pipe;
      } else if (EPCIS_EVENT_TYPES.stream().anyMatch(preScanType::contains)) {
        return addDocumentWrapper(pipe); // method to add the epcis document wrapper to single event
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return captureInput;
  }

  public void copy(final InputStream inputStream, final OutputStream outputStream) {
    try (ReadableByteChannel src = Channels.newChannel(inputStream);
        WritableByteChannel dest = Channels.newChannel(outputStream)) {
      final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
      while (src.read(buffer) != -1) {
        buffer.flip();
        dest.write(buffer);
        buffer.compact();
      }
      buffer.flip();
      while (buffer.hasRemaining()) {
        dest.write(buffer);
      }
    } catch (Exception e) {
      throw new SchemaValidationException(
          "Exception occurred during the copy of OutputStream to InputStream : "
              + e.getMessage()
              + e);
    }
  }

  // Add the document wrapper to single event
  public InputStream addDocumentWrapper(final InputStream captureInput) {
    try {
      final XMLStreamReader reader = inputFactory.createXMLStreamReader(captureInput);
      final PipedOutputStream pipedOutputStream = new PipedOutputStream();
      final PipedInputStream pipe = new PipedInputStream(pipedOutputStream);

      executorService.execute(
          () -> {
            try {
              XMLStreamWriter writer = outputFactory.createXMLStreamWriter(pipedOutputStream);

              writer.writeStartDocument();

              writer.writeStartElement(EPCIS, "EPCISDocument", EPCIS_DOCUMENT_NAMESPACE);
              writer.writeAttribute("creationDate", Instant.now().toString());
              writer.writeAttribute("schemaVersion", "2.0");
              writer.writeNamespace(EPCIS, EPCIS_DOCUMENT_NAMESPACE);
              writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

              writer.writeStartElement("EPCISBody");
              writer.writeStartElement("EventList");

              // read the event and append to writer
              copyXmlEvent(reader, writer);

              writer.writeEndElement(); // end EventList
              writer.writeEndElement(); // end EPCISBody
              writer.writeEndElement(); // end EPCISDocument

              writer.writeEndDocument();
              pipedOutputStream.close();
            } catch (Exception e) {
              try {
                pipedOutputStream.write(e.getMessage().getBytes());
                pipedOutputStream.close();
                throw new SchemaValidationException(
                    "Exception occurred during the adding of document wrapper to EPCIS event : "
                        + e.getMessage(),
                    e);
              } catch (IOException ex) {
                ex.printStackTrace();
              }
            }
          });
      return pipe;
    } catch (Exception e) {
      throw new SchemaValidationException(
          "Exception occurred during the addition of Document wrapper to event : "
              + e.getMessage()
              + e);
    }
  }

  private void copyXmlEvent(final XMLStreamReader reader, final XMLStreamWriter writer) {
    try {
      while (reader.hasNext()) {
        int event = reader.next();

        if (event == XMLStreamConstants.START_ELEMENT) {
          writer.writeStartElement(reader.getLocalName());

          // Add all the attributes associated with xml tag
          for (int i = 0; i < reader.getAttributeCount(); i++) {
            writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
          }

          // Add all the namespaces associated with xml tag
          for (int i = 0; i < reader.getNamespaceCount(); i++) {
            writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
          }

        } else if (event == XMLStreamConstants.END_ELEMENT) {
          writer.writeEndElement();
        } else if (event == XMLStreamConstants.CHARACTERS) {
          writer.writeCharacters(reader.getText());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
