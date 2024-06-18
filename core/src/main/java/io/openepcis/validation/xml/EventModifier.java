/*
 * Copyright 2022-2024 benelog GmbH & Co. KG
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package io.openepcis.validation.xml;

import io.openepcis.validation.exception.SchemaValidationException;

import javax.xml.stream.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

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
  private static final String FIRST_TAG_REGEX = "<(?!\\?|\\!--)([\\S\\s]*?)>";
  private static final Pattern FIRST_TAG_PATTERN = Pattern.compile(FIRST_TAG_REGEX);

  public EventModifier(final ExecutorService executorService) {
    this.executorService = executorService;
  }

  public EventModifier() {
    this(Executors.newWorkStealingPool());
  }

  public InputStream modifyEvent(final InputStream input) {
    final BufferedInputStream captureInput = BufferedInputStream.class.isAssignableFrom(input.getClass())?(BufferedInputStream)input:new BufferedInputStream(input);
    try {
      final String preScan = XMLTagPreScanUtil.scanFirstTag(captureInput);
      final PipedOutputStream pipedOutputStream = new PipedOutputStream();
      final PipedInputStream pipe = new PipedInputStream(pipedOutputStream);
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

      if (preScan.contains("EPCISDocument")) {
        return pipe;
      } else if (EPCIS_EVENT_TYPES.stream().anyMatch(preScan::contains)) {
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
              + e.getMessage(), e);
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
              + e.getMessage(), e);
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
