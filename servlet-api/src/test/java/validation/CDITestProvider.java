package validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.openepcis.model.rest.ProblemResponseBody;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.model.xml.ValidationResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

public class CDITestProvider {

    @Produces
    @RequestScoped
    public SchemaValidator schemaValidator() {
        return new SchemaValidator();
    }


    @Produces
    @Singleton
    ObjectMapper objectMapper() {
        final ObjectMapper mapper =
                new ObjectMapper()
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .registerModule(new JavaTimeModule())
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Produces
    @Singleton
    Marshaller marhaller() {
        try {
            final JAXBContext ctx =
                    JAXBContext.newInstance(ValidationResult.class, ProblemResponseBody.class);
            Log.info("created Validation Test JAXBContext : " + ctx.getClass().getName());
            return ctx.createMarshaller();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
