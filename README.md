<p align="center">
  <img src="https://openepcis.io/img/openepcis-logo.svg" alt="OpenEPCIS" width="30%">
</p>

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/openepcis/openepcis-document-validation-service)](https://github.com/openepcis/openepcis-document-validation-service/releases)
[![Stars](https://img.shields.io/github/stars/openepcis/openepcis-document-validation-service?style=social)](https://github.com/openepcis/openepcis-document-validation-service)

<h1 align="center">OpenEPCIS Document Validation Service</h1>

Validates if a provided [EPCIS](https://www.gs1.org/standards/epcis) document follows the GS1 standard, and
informs in plain language what is wrong when it does not. Works with EPCIS 2.0 JSON/JSON-LD and with
EPCIS 1.2 and 2.0 XML.

## Why

An EPCIS document that looks fine can still be rejected, and the reason is usually buried in schema. A validator that responds with `does not match the uri pattern` leaves users
guessing which field/attribute is not adhering to the standard. This service answers with the field, the users provided, and where in the document issue lies: For example if EPCIS
document contains invalid `bizStep` such as `transforming` then tool responds with:

```
"transforming" is not a standard EPCIS business step for bizStep in event 1 (ObjectEvent).
Did you mean "transporting"? Use one of the 41 standard steps (accepting, arriving, assembling,
collecting, commissioning, consigning and 35 more), or, for a step of your own, a full web address
such as https://example.com/bizStep/transforming.
```

Each invalid value corresponds to one error to avoid chain of errors due to one field violation. All validation processing occur locally nothing reaches network, since every schema
is
bundled in the jar.

## Getting started

Run the demo service in dev mode. It listens on port 9000, with Swagger UI at
[localhost:9000/q/swagger-ui](http://localhost:9000/q/swagger-ui):

```bash
git clone https://github.com/openepcis/openepcis-document-validation-service.git
cd openepcis-document-validation-service
mvn -pl quarkus/rest-app -am quarkus:dev
```

Send it a document:

```bash
curl -X POST 'http://localhost:9000/api/events/validate' \
  -H 'Content-Type: application/json' \
  -H 'GS1-EPCIS-Version: 2.0.0' \
  --data-binary @my-epcis-document.json
```

An empty `200` means the document is valid. A `400` returns the list of problems.

## The API

`POST /api/events/validate`

|                             |                                                                           |
|-----------------------------|---------------------------------------------------------------------------|
| Request body                | the EPCIS document                                                        |
| `Content-Type`              | `application/json`, `application/ld+json` or `application/xml`            |
| `GS1-EPCIS-Version` header  | `2.0.0` (default) or `1.2.0`                                              |
| `epcisDocumentSchema` query | `capture` (default) or `query`                                            |
| `Accept`                    | `application/json` for a JSON array, `application/xml` for a JAXB wrapper |

| Response | Meaning                                   |
|----------|-------------------------------------------|
| `200`    | valid, empty body                         |
| `400`    | not valid, body lists the problems        |
| `415`    | media type or EPCIS version not supported |

### An example error format

```json
[
    {
	"type": "enum",
	"line": "https://ref.gs1.org/standards/epcis/epcis-json-schema.json#/definitions/bizStep/anyOf/1/enum",
	"location": "$.epcisBody.eventList[0].bizStep",
	"message": "\"transforming\" is not a standard EPCIS business step for bizStep in event 1 (ObjectEvent). Did you mean \"transporting\"? Use one of the 41 standard steps (accepting, arriving, assembling, collecting, commissioning, consigning and 35 more), or, for a step of your own, a full web address such as https://example.com/bizStep/transforming.",
	"field": "bizStep",
	"value": "transforming",
	"eventIndex": 0,
	"eventType": "ObjectEvent",
	"suggestion": "transporting",
	"allowedValues": [
	    "accepting",
	    "arriving",
	    "assembling",
	    "collecting",
	    "commissioning",
	    "..."
	]
    }
]
```

`message` is written for a human to read. The other fields are there so a UI can do more with it:

- `location` is a JSON path, so an editor can jump straight to the line. For XML, `line` carries the
  line number instead.
- `field`, `value`, `eventIndex` and `eventType` let you say "event 6, bizStep" without parsing text.
- `suggestion` is the closest standard value, when the wrong one is a near miss.
- `allowedValues` is the full list, kept out of the message so it can sit behind a "show all" toggle.

## Use it as a library

Add the module you need. Versions come from the OpenEPCIS BOM.

```xml
<!-- validation logic only, no web layer -->
<dependency>
    <groupId>io.openepcis</groupId>
    <artifactId>openepcis-document-validation-service</artifactId>
</dependency>

        <!-- JAX-RS endpoint, reactive -->
<dependency>
<groupId>io.openepcis</groupId>
<artifactId>openepcis-document-validation-rest-api</artifactId>
</dependency>

        <!-- plain servlet endpoint, for non-reactive deployments -->
<dependency>
<groupId>io.openepcis</groupId>
<artifactId>openepcis-document-validation-servlet-api</artifactId>
</dependency>

        <!-- Quarkus extension, wires up the CDI beans for you -->
<dependency>
<groupId>io.openepcis.quarkus</groupId>
<artifactId>quarkus-document-validation-service</artifactId>
</dependency>
```

Calling the validator directly:

```java
// SchemaValidator is @RequestScoped, so inject it instead of keeping one instance around
@Inject
SchemaValidator schemaValidator;

final Multi<ValidationError> errors = schemaValidator.validate(
        document,                      // InputStream
        "application/json",            // media type decides JSON or XML
        EPCISDocumentType.CAPTURE,     // or QUERY
        EPCISVersion.VERSION_2_0_0);   // or VERSION_1_2_0
```

The stream stays empty when the document is valid. Each problem arrives as an item, so you can react to
the first one without waiting for the rest. It only fails when validation itself broke, for example on
malformed input.

## Project layout

| Module                                  | What it does                                               |
|-----------------------------------------|------------------------------------------------------------|
| `core`                                  | the validator and the bundled schemas. No web dependencies |
| `rest-api`                              | JAX-RS resource returning `Uni<RestResponse<?>>`           |
| `servlet-api`                           | the same endpoint as a plain servlet                       |
| `quarkus/runtime`, `quarkus/deployment` | Quarkus extension pair                                     |
| `quarkus/rest-app`                      | runnable demo service                                      |
| `restassured`                           | shared HTTP test suite, so both front ends behave the same |

Error wording lives in `core/src/main/resources/validation-messages/epcis-validation-messages.yaml`.
Rewording a message is a change to that file, not to Java.

## Building

Java 21 and Maven. The parent POM is the external OpenEPCIS BOM, so the first build downloads it.

```bash
mvn clean install                                    # everything
mvn -pl core test                                    # one module
mvn -pl core test -Dtest=JsonValidationMessageTest   # one test class
mvn -pl quarkus/rest-app -am verify -Pnative         # native image
```

Test documents come from
[openepcis-test-resources](https://github.com/openepcis/openepcis-test-resources), including the
deliberately broken ones used to check the error messages.

## Contributing

Bug reports, new test documents and better error wording are all welcome. If you hit a message that
left you guessing, that is worth an issue on its own, since clear messages are the whole point of this
project.

## Related

- [OpenEPCIS Tools](https://tools.openepcis.io/) - open source EPCIS 2.0 tools and services
- [OpenEPCIS](https://openepcis.io/) - Read more about OpenEPCIS
- [OpenEPCIS Test Resources](https://github.com/openepcis/openepcis-test-resources) - EPCIS documents used for testing
- [benelog GmbH & Co. KG](https://www.benelog.com/) - Company behind the OpenEPCIS
- [GS1 EPCIS Standard](https://www.gs1.org/standards/epcis) - Learn more about EPCIS

## License

Licensed under the [Apache License 2.0](LICENSE).
