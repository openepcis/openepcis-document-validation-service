package io.openepcis.validation.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

@Data
@XmlRootElement(name ="ValidationError")
@XmlType(name ="ValidationError",
        propOrder = {
        "type",
        "line",
        "column",
        "message"
})
public class ValidationError {

  @XmlAttribute
  private String type;

  @XmlElement
  private String line;

  @XmlElement
  private String column;

  @XmlElement
  private String message;
}
