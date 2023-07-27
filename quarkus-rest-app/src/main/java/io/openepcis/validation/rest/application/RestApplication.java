package io.openepcis.validation.rest.application;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;

@OpenAPIDefinition(
        info =
        @Info(
                title = "OpenEPCIS Event Validation REST API",
                description = "Validate for EPCIS documents in XML/JSON format.",
                version = "1.0.0",
                contact = @Contact(name = "OpenEPCIS", email = "info@openepcis.io"),
                license =
                @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0")))
@ApplicationPath("/")
public class RestApplication extends Application {

}
