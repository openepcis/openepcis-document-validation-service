package io.openepcis.document.validation.resource;

public interface ResponseBodyExamples {
    String RESPONSE_200_TOP_LEVEL_OR_EVENT_TYPE_SUB_RESOURCE =
            "{ \n"
                    + "  \"@context\": \"https://ref.gs1.org/standards/epcis/2.0.0/epcis-context.jsonld\",\n"
                    + "  \"type\": \"Collection\",\n"
                    + "  \"member\": [\"events\"]\n"
                    + "}";
    String RESPONSE_200_EPCIS_QUERY_DOCUMENT =
            "{\n"
                    + "  \"$ref\": \"https://ref.gs1.org/docs/epcis/examples/epcis_query_document.jsonld\"\n"
                    + "}";
    String RESPONSE_200_EPCIS_QUERY_DOCUMENT_SINGLE_PAGE =
            "{\n"
                    + "  \"$ref\": \"https://ref.gs1.org/docs/epcis/examples/epcis_query_document.jsonld\"\n"
                    + "}";
    String RESPONSE_201_EPCIS_BARE_EVENT =
            "{\n"
                    + "  \"$ref\": \"https://ref.gs1.org/docs/epcis/examples/example_9.6.2-object_event.jsonld\"\n"
                    + "}";
    String RESPONSE_200_SUPPORTED_TOP_LEVEL =
            "{\n"
                    + "  \"@context\": \"https://ref.gs1.org/standards/epcis/2.0.0/epcis-context.jsonld\",\n"
                    + "  \"type\": \"Collection\",\n"
                    + "  \"member\": [\n"
                    + "    \"queries\",\n"
                    + "    \"capture\",\n"
                    + "    \"events\",\n"
                    + "    \"eventTypes\",\n"
                    + "    \"epcs\", \n"
                    + "    \"readPoints\",\n"
                    + "    \"bizLocations\",\n"
                    + "    \"dispositions\",\n"
                    + "    \"bizSteps\"\n"
                    + "  ]\n"
                    + "}";
    String RESPONSE_200_SUPPORTED_EVENT_TYPES =
            "{\n"
                    + "  \"@context\": [\n"
                    + "    \"https://ref.gs1.org/standards/epcis/2.0.0/epcis-context.jsonld\",\n"
                    + "    {\n"
                    + "      \"ex\": \"https://example.org/myCustomEventTypes/\"\n"
                    + "    }\n"
                    + "  ],\n"
                    + "  \"type\": \"Collection\",\n"
                    + "  \"member\": [\n"
                    + "    \"AggregationEvent\",\n"
                    + "    \"AssociationEvent\",\n"
                    + "    \"ObjectEvent\",\n"
                    + "    \"TransactionEvent\",\n"
                    + "    \"TransformationEvent\",\n"
                    + "    \"ex:aCustomEventType\"\n"
                    + "  ]\n"
                    + "}";
    String RESPONSE_400_SUBSCRIPTION_ISSUES =
            "{\n"
                    + "  \"type\": \"epcisException:SubscriptionControlsException\",\n"
                    + "  \"title\": \"Subscription error\",\n"
                    + "  \"status\": 400\n"
                    + "}";
    String RESPONSE_400_QUERY_ISSUES =
            "{\n"
                    + "  \"type\": \"epcisException:QueryValidationException\",\n"
                    + "  \"title\": \"EPCIS query exception\",\n"
                    + "  \"status\": 400\n"
                    + "}";
    String RESPONSE_400_VALIDATION_EXCEPTION =
            "{\n"
                    + "  \"status\": 400,\n"
                    + "  \"type\": \"epcisException:ValidationException\",\n"
                    + "  \"title\": \"string\",\n"
                    + "  \"detail\": \"string\",\n"
                    + "  \"instance\": \"string\"\n"
                    + "}";
    String RESPONSE_401_UNAUTHORIZED_REQUEST =
            "{\n"
                    + "  \"type\": \"epcisException:SecurityException\",\n"
                    + "  \"title\": \"Unauthorised request\",\n"
                    + "  \"status\": 401\n"
                    + "}";
    String RESPONSE_403_CLIENT_UNAUTHORIZED =
            "{\n"
                    + "  \"type\": \"epcisException:SecurityException\",\n"
                    + "  \"title\": \"Access to resource forbidden\",\n"
                    + "  \"status\": 403\n"
                    + "}";
    String RESPONSE_404_RESOURCE_NOT_FOUND =
            "{\n"
                    + "  \"type\": \"epcisException:NoSuchResourceException\",\n"
                    + "  \"title\": \"Resource not found\",\n"
                    + "  \"status\": 404\n"
                    + "}";
    String RESPONSE_413_CAPTURE_PAYLOAD_TOO_LARGE =
            "{\n"
                    + "  \"type\": \"epcisException:CaptureLimitExceededException\",\n"
                    + "  \"title\": \"Capture Payload too large\",\n"
                    + "  \"status\": 413\n"
                    + "}";
    String RESPONSE_413_QUERY_SCOPE_OR_SIZE =
            "{\n"
                    + "  \"type\": \"epcisException:QueryTooComplexException\",\n"
                    + "  \"title\": \"Capture Payload too large\",\n"
                    + "  \"status\": 413\n"
                    + "}";
    String RESPONSE_414_URL_TOO_LONG =
            "{\n"
                    + "  \"type\": \"epcisException:URITooLongException\",\n"
                    + "  \"title\": \"URI Too Long\",\n"
                    + "  \"status\": 414\n"
                    + "}";
    String RESPONSE_406_NOT_ACCEPTABLE =
            "{\n"
                    + "  \"type\": \"epcisException:NotAcceptableException\",\n"
                    + "  \"title\": \"Conflicting request and response headers\",\n"
                    + "  \"status\": 406\n"
                    + "}";
    String RESPONSE_409_RESOURCE_ALREADY_EXISTS_EXCEPTION =
            "{\n"
                    + "  \"type\": \"epcisException:ResourceAlreadyExistsException\",\n"
                    + "  \"title\": \"A resource with the provided identifier already exists.\",\n"
                    + "  \"status\": 409\n"
                    + "}";
    String RESPONSE_415_UNSUPPORTED_MEDIA_TYPE =
            "{\n"
                    + "  \"type\": \"epcisException:UnsupportedMediaTypeException\",\n"
                    + "  \"title\": \"Unsupported Media Type\",\n"
                    + "  \"status\": 415\n"
                    + "}";
    String RESPONSE_500_IMPLEMENTATION_EXCEPTION =
            "{\n"
                    + "  \"type\": \"epcisException:ImplementationException\",\n"
                    + "  \"title\": \"A server-side error occurred\",\n"
                    + "  \"status\": 500\n"
                    + "}";
    String RESPONSE_501_NOT_IMPLEMENTED =
            "{\n"
                    + "  \"type\": \"epcisException:ImplementationException\",\n"
                    + "  \"title\": \"Functionality not supported by server\",\n"
                    + "  \"status\": 501\n"
                    + "}";
}
