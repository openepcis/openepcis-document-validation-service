package io.openepcis.validation.rest.resource;

import io.openepcis.validation.SchemaValidator;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

public class SchemaValidatorProvider {
    @Produces
    @RequestScoped
    public SchemaValidator schemaValidator() {
        return new SchemaValidator();
    }

}
