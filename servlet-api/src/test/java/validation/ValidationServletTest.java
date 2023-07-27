package validation;

import io.openepcis.validation.restassured.AbstractValidationResourceTest;
import io.openepcis.validation.servlet.ValidationServlet;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

import java.net.URL;

@QuarkusTest
public class ValidationServletTest extends AbstractValidationResourceTest {

    @TestHTTPEndpoint(ValidationServlet.class)
    @TestHTTPResource
    URL url;

    @Override
    protected String url() {
        return url.toString();
    }
}
