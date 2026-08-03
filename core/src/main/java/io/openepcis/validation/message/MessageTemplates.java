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
package io.openepcis.validation.message;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.ValidationMessage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Reads the message catalogue and decides which sentence describes a validation failure.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageTemplates {

    // Read the catalog of error validation messages
    private static final String LOCATION = "/validation-messages/epcis-validation-messages.yaml";

    // Loaded exactly once, when this class is first touched.
    private static final Catalogue CATALOGUE = load();

    // Hold the 4 types of validation message from epcis-validation-messages.yaml
    private record Catalogue(
            Map<String, String> schemaLocations,
            Map<String, String> keywords,
            Map<String, String> fragments,
            Map<String, String> xsdTypes
    ) {
    }

    private static Catalogue load() {
        try (InputStream stream = MessageTemplates.class.getResourceAsStream(LOCATION)) {
            if (stream == null) {
                throw new IllegalStateException("Validation message catalogue not found at " + LOCATION);
            }

            final Catalogue parsed = new ObjectMapper(new YAMLFactory())
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(stream, Catalogue.class);

            // rebuild with truly immutable maps, no wrapper indirection
            return new Catalogue(
                    immutable(parsed.schemaLocations()),
                    immutable(parsed.keywords()),
                    immutable(parsed.fragments()),
                    immutable(parsed.xsdTypes()));

        } catch (IOException e) {
            throw new UncheckedIOException("Could not read validation message catalogue", e);
        }
    }


    // Map.copyOf gives a real immutable map. A missing YAML section becomes empty, never null
    private static Map<String, String> immutable(final Map<String, String> source) {
        return MapUtils.isEmpty(source) ? Map.of() : Map.copyOf(source);
    }

    /**
     * The template for this message, or null when the caller should fall back to getError().
     */
    public static String templateFor(final ValidationMessage message) {
        final String location = message.getSchemaLocation().toString();
        final int hash = location.indexOf('#');
        String pointer = hash < 0 ? "#" : location.substring(hash);
        final String keyword = message.getType();

        while (true) {
            String template = CATALOGUE.schemaLocations().get(pointer + "." + keyword);
            if (template != null) {
                return template;
            }

            template = CATALOGUE.schemaLocations().get(pointer);
            if (template != null) {
                return template;
            }

            final int slash = pointer.lastIndexOf('/');
            if (slash <= 0) {
                break;
            }
            pointer = pointer.substring(0, slash);
        }

        final String byMessageKey = CATALOGUE.keywords().get(message.getMessageKey());
        return byMessageKey != null ? byMessageKey : CATALOGUE.keywords().get(keyword);
    }


    /**
     * A keyword template by name, for synthetic messages such as required.alternatives.
     */
    public static String keyword(final String name) {
        return CATALOGUE.keywords().get(name);
    }

    /**
     * Whether a keyword template exists, so callers can choose a different strategy if not.
     */
    public static boolean hasKeyword(final String name) {
        return CATALOGUE.keywords().containsKey(name);
    }

    /**
     * A reusable sentence piece, with a hard coded fallback so wording is never missing entirely.
     */
    public static String fragment(final String name, final String fallback) {
        final String value = CATALOGUE.fragments().get(name);
        return value != null ? value : fallback;
    }

    /**
     * Maps an XSD rule code such as cvc-enumeration-valid to the type field of the response.
     */
    public static String xsdType(final String code) {
        return CATALOGUE.xsdTypes().get(code);
    }

    /**
     * Fills a template and tidies the finished sentence. Null template gives null, not a crash.
     */
    public static String render(final String template, final Map<String, String> values) {
        return template == null ? null : tidy(fill(template, values));
    }

    /**
     * Collapses runs of spaces left behind by empty placeholders and trims the ends.
     */
    public static String tidy(final String message) {
        if (message == null) {
            return null;
        }

        final StringBuilder out = new StringBuilder(message.length());
        boolean previousWasSpace = false;

        for (int i = 0; i < message.length(); i++) {
            final char c = message.charAt(i);
            final boolean isSpace = c == ' ' || c == '\t';

            if (isSpace) {
                if (!previousWasSpace) {
                    out.append(' '); // keep the first space of a run, drop the rest
                }
            } else {
                out.append(c);
            }
            previousWasSpace = isSpace;
        }

        int start = 0; // trim leading spaces without allocating another String
        int end = out.length();
        while (start < end && out.charAt(start) == ' ') {
            start++;
        }
        while (end > start && out.charAt(end - 1) == ' ') {
            end--;
        }
        return out.substring(start, end);
    }


    /**
     * Replaces every {name} with its value. An unknown placeholder becomes empty.
     */
    public static String fill(final String template, final Map<String, String> values) {
        if (template.indexOf('{') < 0) {
            // nothing to substitute, so allocate nothing at all
            return template;
        }

        final StringBuilder out = new StringBuilder(template.length() + 64);
        int i = 0;

        while (i < template.length()) {
            final int open = template.indexOf('{', i);
            if (open < 0) {
                // no placeholder left, copy the tail
                out.append(template, i, template.length());
                break;
            }

            final int close = template.indexOf('}', open);
            if (close < 0) {
                // unbalanced brace, copy verbatim, never throw
                out.append(template, i, template.length());
                break;
            }

            // the literal text before the placeholder
            out.append(template, i, open);
            final String value = values.get(template.substring(open + 1, close));
            if (value != null) {
                // a missing value contributes nothing, tidy() closes the gap
                out.append(value);
            }

            i = close + 1;
        }
        return out.toString();
    }
}
