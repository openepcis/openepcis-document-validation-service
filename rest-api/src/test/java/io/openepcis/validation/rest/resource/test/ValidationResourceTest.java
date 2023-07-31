package io.openepcis.validation.rest.resource.test;

import io.openepcis.validation.rest.resource.ValidationResource;
import io.openepcis.validation.restassured.AbstractValidationTest;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

import java.net.URL;

@QuarkusTest
public class ValidationResourceTest extends AbstractValidationTest {

    @TestHTTPEndpoint(ValidationResource.class)
    @TestHTTPResource
    URL url;

    @Override
    protected String url() {
        return url.toString()+"/events/validate";
    }
}
