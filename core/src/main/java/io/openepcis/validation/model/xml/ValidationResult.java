package io.openepcis.validation.model.xml;

import io.openepcis.validation.model.ValidationError;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@XmlRootElement(name = "ValidationResult")
@XmlType(name = "ValidationResult")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ValidationResult {


    @XmlElementWrapper(name = "errors")
    @XmlElement(name = "ValidationError")
    private List<ValidationError> errors;

}
