/*
 * Copyright 2022-2026 benelog GmbH & Co. KG
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
package io.openepcis.validation.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.serialization.JsonNodeReader;
import io.openepcis.validation.message.MessageTemplates;
import io.openepcis.validation.model.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the message catalogue itself. The error message itself can contain wrong information or missing information.
 * These tests are present to prevent any missing/wrong information within the error message itself.
 */
class MessageCatalogueTest {

    private static final String CATALOGUE = "/validation-messages/epcis-validation-messages.yaml";

    // Every placeholder the formatter is able to supply. Anything else is a typo.
    private static final Set<String> KNOWN_PLACEHOLDERS =
            Set.of("field", "value", "where", "suggestion", "allowedCount", "allowedSample",
                    "expected", "actual", "limit", "fields", "best", "reasons", "number", "type");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]*)}");

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> readCatalogue() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(CATALOGUE)) {
            assertNotNull(stream, "catalogue not found on the classpath at " + CATALOGUE);
            return new ObjectMapper(new YAMLFactory()).readValue(stream, Map.class);
        }
    }

    // ---------------------------------------------------------------------------
    //  Content integrity
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("catalogue: parses and contains all four sections")
    void catalogueParses() throws Exception {
        final Map<String, Map<String, String>> catalogue = readCatalogue();
        assertTrue(catalogue.containsKey("schemaLocations"));
        assertTrue(catalogue.containsKey("keywords"));
        assertTrue(catalogue.containsKey("fragments"));
        assertTrue(catalogue.containsKey("xsdTypes"));
        assertFalse(catalogue.get("schemaLocations").isEmpty());
        assertFalse(catalogue.get("keywords").isEmpty());
    }

    @Test
    @DisplayName("catalogue: schema pointer keys survived YAML parsing")
    void schemaPointerKeysAreNotSwallowed() throws Exception {
        // a JSON Pointer starts with '#', which YAML reads as a comment unless the key is quoted.
        final Map<String, String> locations = readCatalogue().get("schemaLocations");
        assertTrue(locations.containsKey("#/definitions/bizStep"), "bizStep pointer key missing, is it quoted in the YAML? keys=" + locations.keySet());
        locations.keySet()
                .forEach(key -> assertTrue(key.startsWith("#/"), "not a JSON Pointer key: " + key));
    }

    @Test
    @DisplayName("catalogue: every placeholder is one the formatter can fill")
    void noUnknownPlaceholders() throws Exception {
        final Map<String, String> offenders = new LinkedHashMap<>();
        readCatalogue()
                .forEach(
                        (section, entries) ->
                                entries.forEach(
                                        (key, template) -> {
                                            final Matcher matcher = PLACEHOLDER.matcher(template);
                                            while (matcher.find()) {
                                                if (!KNOWN_PLACEHOLDERS.contains(matcher.group(1))) {
                                                    offenders.put(section + " -> " + key, matcher.group(0));
                                                }
                                            }
                                        }));
        assertTrue(offenders.isEmpty(), "unknown placeholders: " + offenders);
    }

    @Test
    @DisplayName("catalogue: braces are balanced in every template")
    void bracesAreBalanced() throws Exception {
        readCatalogue()
                .forEach(
                        (section, entries) ->
                                entries.forEach(
                                        (key, template) ->
                                                assertEquals(
                                                        template.chars().filter(c -> c == '{').count(),
                                                        template.chars().filter(c -> c == '}').count(),
                                                        "unbalanced braces in " + section + " -> " + key)));
    }

    @Test
    @DisplayName("catalogue: the fragments that need a leading space still have one")
    void leadingSpacesArePreserved() throws Exception {
        // these are appended straight onto a sentence, so losing the space runs words together.
        final Map<String, String> fragments = readCatalogue().get("fragments");
        assertTrue(fragments.get("suggestionHint").startsWith(" "), "suggestionHint lost its space");
        assertTrue(fragments.get("reasonMore").startsWith(" "), "reasonMore lost its space");
    }

    // ---------------------------------------------------------------------------
    //  Lookup behaviour
    // ---------------------------------------------------------------------------

    /**
     * Produces the real messages for one broken document, so lookup is tested against real input.
     */
    private Set<ValidationMessage> messagesFor(final String document) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                        .jsonNodeReader(JsonNodeReader.builder().jsonMapper(mapper).build())
                        .build();
        try (InputStream schema = getClass().getResourceAsStream("/json-schema/modular-EPCIS-JSON-schema.json")) {
            final JsonSchema captureSchema = factory.getSchema(schema);
            final JsonNode node = mapper.readTree(document);
            return captureSchema.validate(node);
        }
    }

    private static String captureWith(final String extraField) {
        return "{\"@context\":[\"https://ref.gs1.org/standards/epcis/epcis-context.jsonld\"],"
                + "\"type\":\"EPCISDocument\",\"schemaVersion\":\"2.0\","
                + "\"creationDate\":\"2026-07-22T10:30:00Z\",\"epcisBody\":{\"eventList\":["
                + "{\"type\":\"ObjectEvent\",\"eventTime\":\"2026-07-22T06:45:00+02:00\","
                + "\"eventTimeZoneOffset\":\"+02:00\",\"action\":\"ADD\","
                + "\"epcList\":[\"https://id.gs1.org/00/038123450000000129\"],"
                + extraField
                + "}]}}";
    }

    @Test
    @DisplayName("lookup: a bizStep failure resolves the layer 1 entry by walking the schema pointer")
    void schemaPointerWalkResolvesVocabulary() throws Exception {
        // the engine reports #/definitions/bizStep/anyOf/1/enum, the catalogue holds #/definitions/bizStep
        final ValidationMessage enumMessage =
                messagesFor(captureWith("\"bizStep\":\"transforming\"")).stream()
                        .filter(message -> "enum".equals(message.getType()))
                        .findFirst()
                        .orElseThrow();
        final String template = MessageTemplates.templateFor(enumMessage);
        assertNotNull(template);
        assertTrue(
                template.contains("business step"),
                "expected the bizStep wording, got: " + template);
    }

    @Test
    @DisplayName("lookup: a keyword with no schema entry falls back to the layer 2 wording")
    void keywordFallbackIsUsed() throws Exception {
        final ValidationMessage required =
                messagesFor(
                        "{\"@context\":[\"https://ref.gs1.org/standards/epcis/epcis-context.jsonld\"],"
                                + "\"type\":\"EPCISDocument\",\"schemaVersion\":\"2.0\","
                                + "\"creationDate\":\"2026-07-22T10:30:00Z\",\"epcisBody\":{\"eventList\":["
                                + "{\"type\":\"ObjectEvent\",\"eventTimeZoneOffset\":\"+02:00\",\"action\":\"ADD\","
                                + "\"epcList\":[\"https://id.gs1.org/00/038123450000000129\"]}]}}")
                        .stream()
                        .filter(message -> "required".equals(message.getType()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(MessageTemplates.keyword("required"), MessageTemplates.templateFor(required));
    }

    @Test
    @DisplayName("lookup: an unknown keyword returns null so the library wording is used instead")
    void unknownKeywordFallsThrough() {
        assertNull(MessageTemplates.keyword("thisKeywordDoesNotExist"));
        assertFalse(MessageTemplates.hasKeyword("thisKeywordDoesNotExist"));
    }

    @Test
    @DisplayName("lookup: both synthetic required templates exist")
    void syntheticRequiredTemplatesExist() {
        // one for a choice between fields, one for several fields that are all missing
        assertTrue(MessageTemplates.hasKeyword("required.alternatives"));
        assertTrue(MessageTemplates.hasKeyword("required.multiple"));
    }

    // ---------------------------------------------------------------------------
    //  Rendering
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("render: placeholders are replaced and unknown ones leave no gap")
    void fillAndTidy() {
        assertEquals("bizStep is wrong", MessageTemplates.render("{field} is wrong{suggestion}", Map.of("field", "bizStep")));
        assertEquals("bizStep is wrong. Did you mean \"shipping\"?",
                MessageTemplates.render(
                        "{field} is wrong.{suggestion}",
                        Map.of("field", "bizStep", "suggestion", " Did you mean \"shipping\"?")));
    }

    @Test
    @DisplayName("render: runs of spaces collapse and the ends are trimmed")
    void tidyCollapsesWhitespace() {
        assertEquals("a b c", MessageTemplates.tidy("  a   b \t c  "));
    }

    @Test
    @DisplayName("render: a template with no placeholder is returned untouched")
    void fillShortCircuits() {
        final String plain = "nothing to substitute here";
        assertEquals(plain, MessageTemplates.fill(plain, Map.of()));
    }

    @Test
    @DisplayName("render: an unbalanced brace is copied verbatim rather than throwing")
    void unbalancedBraceIsSafe() {
        assertEquals("a {field", MessageTemplates.fill("a {field", Map.of("field", "x")));
    }

    // ---------------------------------------------------------------------------
    //  XML rule code mapping
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("xsd: rule codes map to a meaningful type")
    void xsdCodesAreMapped() {
        assertEquals("enum", MessageTemplates.xsdType("cvc-enumeration-valid"));
        assertEquals("pattern", MessageTemplates.xsdType("cvc-pattern-valid"));
        assertEquals("unexpectedElement", MessageTemplates.xsdType("cvc-complex-type.2.4"));
        assertEquals("invalidContent", MessageTemplates.xsdType("cvc-complex-type"));
        assertNull(MessageTemplates.xsdType("cvc-does-not-exist"));
    }

    // ---------------------------------------------------------------------------
    //  The message trimming that used to corrupt XML text
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("trim: an XSD rule code prefix is removed")
    void ruleCodePrefixIsStripped() {
        final ValidationError error = new ValidationError();
        assertEquals(
                "Invalid content was found starting with element 'action'.",
                error.filterErrorMessage("cvc-complex-type.2.4.a: Invalid content was found starting with element 'action'."));
    }

    @Test
    @DisplayName("trim: a colon inside the text is left alone")
    void colonInsideTextIsNotCut() {
        final ValidationError error = new ValidationError();
        final String message = "The prefix \"epcis\" for element \"epcis:EPCISDocument\" is not bound.";
        assertEquals(message, error.filterErrorMessage(message));
    }

    @Test
    @DisplayName("trim: a null or blank message is handled without throwing")
    void blankMessageIsSafe() {
        final ValidationError error = new ValidationError();
        assertNull(error.filterErrorMessage(null));
        assertEquals("", error.filterErrorMessage(""));
    }
}
