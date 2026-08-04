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

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import io.openepcis.validation.model.ValidationError;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * Turns the raw networknt messages into one readable error per place in the document.
 */
public final class JsonErrorFormatter {

    //When one position produced several messages, lead with the most useful keyword.
    private static final List<String> KEYWORD_PRIORITY =
            List.of(
                    "required", "additionalProperties", "propertyNames", "enum", "type", "format", "pattern",
                    "minItems", "maxItems", "uniqueItems", "minimum", "maximum", "minLength");
    // Keeps a 41 value vocabulary out of the sentence. The full list travels in allowedValues.
    private static final int ALLOWED_SAMPLE_SIZE = 6;

    /**
     * Where a single streamed event sits in the document. Null whenever the whole document was
     * parsed, because then the paths networknt reports are already absolute.
     *
     * @param prefix the absolute path of this event, for example $.epcisBody.eventList[3]
     * @param index  its zero based position in the event list
     * @param type   its EPCIS event type, or null when the document did not declare one
     */
    private record EventContext(String prefix, int index, String type) {
    }

    /**
     * Groups and words every failure of a whole parsed document.
     */
    public List<ValidationError> format(final JsonNode document, final Collection<ValidationMessage> messages) {
        return format(document, messages, null); // no context, the reported paths are already absolute
    }

    /**
     * Groups and words the failures of one streamed event.
     *
     * <p>A streamed event is validated on its own, so networknt reports $.eventTime where the caller
     * needs $.epcisBody.eventList[3].eventTime. The context restores that, which is what keeps the
     * location usable for jumping to a line in an editor.
     *
     * @param eventListPath absolute path of the list holding the event, without the index
     * @param eventIndex    zero based position of the event in that list
     * @param eventType     EPCIS event type of the event, may be null
     */
    public List<ValidationError> formatEvent(final String eventListPath,
                                             final int eventIndex,
                                             final String eventType,
                                             final Collection<ValidationMessage> messages) {
        // the index is appended here so a caller cannot get the bracket syntax wrong
        final String prefix = eventListPath + "[" + eventIndex + "]";
        // no document tree exists while streaming, the context carries what would be read from it
        return format(null, messages, new EventContext(prefix, eventIndex, eventType));
    }

    private List<ValidationError> format(final JsonNode document,
                                         final Collection<ValidationMessage> messages,
                                         final EventContext context) {

        if (CollectionUtils.isEmpty(messages)) {
            // valid document, so no allocation at all
            return List.of();
        }

        final Map<String, List<ValidationMessage>> byLocation = new LinkedHashMap<>();
        for (final ValidationMessage message : messages) {
            // one bucket per position, which is what merges the duplicates
            byLocation.computeIfAbsent(message.getInstanceLocation().toString(), key -> new ArrayList<>()).add(message);
        }

        // where each anyOf/oneOf choice failed, so a choice can tell a missing field from a bad one
        final Map<String, Set<String>> branchLocations = branchLocations(messages);
        final List<ValidationError> results = new ArrayList<>(byLocation.size());
        byLocation.forEach((location, group) -> results.addAll(buildAll(document, location, group, branchLocations, context)));
        results.sort(Comparator.comparing(error -> error.getLocation() == null ? "" : error.getLocation()));
        return results;
    }

    /**
     * Records every position at which each anyOf/oneOf choice failed, keyed by the choice.
     *
     * <p>A branch that fails at the position of the choice itself was rejected because a field is
     * absent. A branch that fails deeper got past that presence check, so the field was supplied and
     * only its contents are wrong. Telling those two apart is what {@link
     * #choiceWasSatisfiedButInvalid} needs.
     */
    private Map<String, Set<String>> branchLocations(final Collection<ValidationMessage> messages) {
        final Map<String, Set<String>> byBranch = new HashMap<>();
        for (final ValidationMessage message : messages) {
            final String branch = branchGroupOf(message);
            if (branch != null) {
                byBranch.computeIfAbsent(branch, key -> new HashSet<>())
                        .add(message.getInstanceLocation().toString());
            }
        }
        return byBranch;
    }

    /**
     * Whether the document already satisfied this choice and only got the contents wrong.
     *
     * <p>An event carrying a quantityList with an unknown field inside it fails that branch deep in
     * the subtree, and the sibling branches then report their own fields as missing. Saying "epcList
     * or sensorElementList is missing" would be wrong, so the choice is dropped and the precise error
     * inside quantityList is kept instead.
     *
     * <p>Only failures of this same choice count. That is what stops an unrelated problem, such as a
     * malformed eventTime, from ever hiding a genuinely missing field.
     */
    private boolean choiceWasSatisfiedButInvalid(final String branch,
                                                 final String location,
                                                 final Map<String, Set<String>> branchLocations) {
        final Set<String> inGroup = branchLocations.get(branch);
        if (inGroup == null) {
            return false; // nothing recorded for this choice, so keep the error
        }
        for (final String other : inGroup) {
            if (other.length() > location.length() // strictly longer means deeper than the choice
                    && other.startsWith(location)
                    && (other.charAt(location.length()) == '.' || other.charAt(location.length()) == '[')) {
                return true; // a branch of this choice got past the presence check
            }
        }
        return false;
    }

    // Which anyOf or oneOf produced it, so sibling branches of the same choice can be merged.
    private String branchGroupOf(final ValidationMessage message) {
        final String path = message.getEvaluationPath().toString();
        int index = path.lastIndexOf("anyOf[");
        if (index < 0) {
            index = path.lastIndexOf("oneOf[");
        }
        // the shared prefix identifies the choice
        return index < 0 ? null : path.substring(0, index);
    }

    /**
     * One position can still hold genuinely different problems. Alternative required fields coming
     * from sibling branches become one "at least one of" sentence, everything else is kept separate.
     */
    private List<ValidationError> buildAll(final JsonNode document,
                                           final String location,
                                           final List<ValidationMessage> group,
                                           final Map<String, Set<String>> branchLocations,
                                           final EventContext context) {

        // Every "required" message is grouped by the choice it came out of. The empty key collects the
        // unconditional ones, where each named field really is missing rather than being an alternative.
        final Map<String, List<ValidationMessage>> requiredByBranch = new LinkedHashMap<>();
        final List<ValidationMessage> others = new ArrayList<>();
        for (final ValidationMessage message : group) {
            if ("required".equals(message.getType())) {
                final String branch = branchGroupOf(message); // null when the rule is unconditional
                requiredByBranch
                        .computeIfAbsent(branch == null ? "" : branch, key -> new ArrayList<>())
                        .add(message);
            } else {
                others.add(message);
            }
        }

        final List<ValidationError> results = new ArrayList<>();
        // field name to the message that reported it, so nothing is lost and duplicates collapse
        final Map<String, ValidationMessage> leftoverRequired = new LinkedHashMap<>();

        requiredByBranch.forEach(
                (branch, siblings) -> {
                    final Set<String> fields = new LinkedHashSet<>(); // ordered, so wording is stable
                    for (final ValidationMessage message : siblings) {
                        if (message.getProperty() != null) {
                            fields.add(message.getProperty()); // getProperty holds the missing field name
                        }
                    }

                    // sibling branches offer a choice, so they become one "at least one of" sentence
                    if (!branch.isEmpty() && fields.size() > 1 && MessageTemplates.hasKeyword("required.alternatives")) {
                        if (choiceWasSatisfiedButInvalid(branch, location, branchLocations)) {
                            return; // the precise error inside that field is the real one
                        }

                        results.add(buildRequiredGroup(document, location, siblings, fields, "required.alternatives", context));
                        return;
                    }

                    for (final ValidationMessage message : siblings) {
                        if (message.getProperty() == null) {
                            others.add(message); // no field name to group by, report it on its own
                        } else {
                            leftoverRequired.putIfAbsent(message.getProperty(), message);
                        }
                    }
                });

        // unconditional rules mean all of these fields are missing, so every one must be named
        if (leftoverRequired.size() > 1 && MessageTemplates.hasKeyword("required.multiple")) {
            results.add(
                    buildRequiredGroup(
                            document,
                            location,
                            List.copyOf(leftoverRequired.values()),
                            leftoverRequired.keySet(),
                            "required.multiple",
                            context));
        } else {
            others.addAll(leftoverRequired.values()); // a single missing field stays an ordinary message
        }

        if (!others.isEmpty()) {
            results.add(build(document, location, others, context));
        }
        return results;
    }

    /**
     * Builds one error that names several fields at once.
     *
     * @param templateKey required.alternatives when the fields are a choice, required.multiple when
     *                    every one of them is genuinely missing
     */
    private ValidationError buildRequiredGroup(
            final JsonNode document,
            final String location,
            final List<ValidationMessage> siblings,
            final Set<String> fields,
            final String templateKey,
            final EventContext context) {
        final ValidationError error = new ValidationError();
        error.setType("required");
        error.setLine(siblings.get(0).getSchemaLocation().toString());
        error.setLocation(absolute(location, context)); // full document path even while streaming

        final String joined = String.join(", ", fields);
        error.setField(joined);
        applyEventContext(error, document, location, context);
        error.setAllowedValues(List.copyOf(fields));
        error.setMessage(MessageTemplates.render(MessageTemplates.keyword(templateKey), Map.of("where", whereLabel(error), "fields", joined)));

        return error;
    }

    /**
     * Builds one error from a group of messages that all describe the same position.
     */
    private ValidationError build(final JsonNode document,
                                  final String location,
                                  final List<ValidationMessage> group,
                                  final EventContext context) {

        final ValidationMessage primary = pickPrimary(group); // the most informative of the group
        final ValidationError error = new ValidationError();

        error.setType(primary.getType()); // unchanged, the front end may switch on it
        error.setLine(primary.getSchemaLocation().toString()); // unchanged
        error.setLocation(absolute(location, context)); // full document path even while streaming

        // the field name is the last path segment, except for required, additionalProperties and
        // propertyNames, where the path points at the parent and the name is in getProperty
        error.setField(primary.getProperty() != null ? primary.getProperty() : lastSegment(location));
        error.setValue(plainValue(primary.getInstanceNode())); // the value sent, previously discarded
        applyEventContext(error, document, location, context);

        final List<String> allowed = allowedValuesIn(group); // read from the schema, never hard coded
        if (!allowed.isEmpty()) {
            error.setAllowedValues(allowed); // full list as data so the UI can reveal it on demand
            error.setSuggestion(ValueSuggester.closest(error.getValue(), allowed)); // commons-text
        }

        error.setMessage(renderMessage(primary, group, error, allowed));
        return error;
    }

    /**
     * Chooses the wording and fills in every placeholder.
     */
    private String renderMessage(
            final ValidationMessage primary,
            final List<ValidationMessage> group,
            final ValidationError error,
            final List<String> allowed) {

        final Map<String, String> values = new HashMap<>(); // one small map per reported error
        values.put("field", error.getField() == null ? "this field" : error.getField());
        values.put("value", error.getValue() == null ? "" : error.getValue());
        values.put("where", whereLabel(error));
        values.put("allowedCount", String.valueOf(allowed.size()));
        values.put("allowedSample", sample(allowed));
        values.put(
                "suggestion",
                error.getSuggestion() == null
                        ? "" // no close match, so the sentence simply omits the hint
                        : MessageTemplates.fill(
                        MessageTemplates.fragment("suggestionHint", " Did you mean \"{best}\"?"),
                        Map.of("best", error.getSuggestion())));

        // keyword specific extras, reusing the arguments networknt already computed for us
        final Object[] arguments = primary.getArguments();
        if (arguments != null && arguments.length > 0) {
            if ("type".equals(primary.getType()) && arguments.length >= 2) {
                values.put("actual", String.valueOf(arguments[0])); // for type the order is found, expected
                values.put("expected", String.valueOf(arguments[1]));
            } else {
                values.put("limit", String.valueOf(arguments[0])); // min/max style keywords
                if (arguments.length >= 2) {
                    values.put("actual", String.valueOf(arguments[1]));
                }
            }
        }

        final String rendered = MessageTemplates.render(MessageTemplates.templateFor(primary), values);
        final String base = rendered != null ? rendered : primary.getError(); // layer 3 safety net

        final String extras = otherReasons(primary, group);
        if (extras.isEmpty()) {
            return MessageTemplates.tidy(base); // the common case
        }
        return MessageTemplates.tidy(
                base
                        + MessageTemplates.fill(
                        MessageTemplates.fragment("reasonMore", " Also: {reasons}"),
                        Map.of("reasons", extras)));
    }

    /**
     * Any genuinely different reason at the same position, so nothing is silently dropped.
     */
    private String otherReasons(final ValidationMessage primary, final List<ValidationMessage> group) {
        if (group.size() < 2) {
            return ""; // nothing else to report
        }
        final Set<String> keywords = new HashSet<>();
        for (final ValidationMessage message : group) {
            keywords.add(message.getType());
        }
        // repeated branches of one keyword, and the vocabulary pair of enum plus uri, are one mistake
        if (keywords.size() <= 1
                || (keywords.size() == 2
                && keywords.contains("enum")
                && (keywords.contains("format") || keywords.contains("pattern")))) {
            return "";
        }
        final List<String> extras = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        seen.add(primary.getType()); // the primary reason is already in the sentence
        for (final ValidationMessage message : group) {
            if (seen.add(message.getType())) {
                extras.add(message.getError()); // one entry per distinct remaining keyword
            }
        }
        return String.join("; ", extras);
    }

    /**
     * Picks the message whose keyword explains the mistake best.
     */
    private ValidationMessage pickPrimary(final List<ValidationMessage> group) {
        ValidationMessage best = group.get(0);
        int bestRank = rank(best.getType());
        for (final ValidationMessage message : group) {
            final int rank = rank(message.getType());
            if (rank < bestRank) {
                bestRank = rank;
                best = message;
            }
        }
        return best;
    }

    /**
     * Lower is better. Unknown keywords sort last rather than being dropped.
     */
    private int rank(final String keyword) {
        final int index = KEYWORD_PRIORITY.indexOf(keyword);
        return index < 0 ? KEYWORD_PRIORITY.size() : index;
    }

    /**
     * Reads the allowed values from the schema node itself. This is why no EPCIS vocabulary list is
     * ever copied into Java or into the catalogue, and why a GS1 schema update needs no code change.
     */
    private List<String> allowedValuesIn(final List<ValidationMessage> group) {
        for (final ValidationMessage message : group) {
            if ("enum".equals(message.getType())) {
                final JsonNode schemaNode = message.getSchemaNode(); // the enum array from the schema
                if (schemaNode != null && schemaNode.isArray()) {
                    final List<String> values = new ArrayList<>(schemaNode.size()); // exact size
                    schemaNode.forEach(node -> values.add(node.asText()));
                    return values;
                }
            }
        }
        return List.of(); // shared empty list, no allocation
    }

    /**
     * A short sample, so a 41 value vocabulary never floods the sentence.
     */
    private String sample(final List<String> allowed) {
        if (allowed.isEmpty()) {
            return "";
        }
        if (allowed.size() <= ALLOWED_SAMPLE_SIZE) {
            return String.join(", ", allowed);
        }
        return String.join(", ", allowed.subList(0, ALLOWED_SAMPLE_SIZE)) // subList is a view, no copy
                + " and "
                + (allowed.size() - ALLOWED_SAMPLE_SIZE)
                + " more";
    }

    /**
     * Records which event holds the problem, so a message can say "event 6 (TransformationEvent)".
     */
    private void applyEventContext(final ValidationError error,
                                   final JsonNode document,
                                   final String location,
                                   final EventContext context) {
        if (context != null) {
            // streaming already knows which event this is, so no path parsing and no tree lookup
            error.setEventIndex(context.index());
            error.setEventType(context.type());
            return;
        }
        final Integer index = eventIndexOf(location);
        if (index == null) {
            return; // the problem is outside eventList, for example at the document root
        }
        error.setEventIndex(index);
        if (document == null) {
            return; // still useful without the type
        }
        final JsonNode events = document.path("epcisBody").path("eventList"); // path never throws
        if (events.isArray() && index < events.size()) {
            final JsonNode type = events.get(index).path("type");
            if (type.isTextual()) {
                error.setEventType(type.asText()); // read from the parsed tree, no second parse
            }
        }
    }

    /**
     * Rewrites an event relative path into a full document path, so a caller of {@link #formatEvent}
     * gets the same location it would have got from validating the whole document.
     */
    private String absolute(final String location, final EventContext context) {
        if (context == null || location == null) {
            return location; // whole document mode, the path is already absolute
        }
        // $ alone means the event itself, anything else keeps its tail after the leading $
        return "$".equals(location) ? context.prefix() : context.prefix() + location.substring(1);
    }

    /**
     * Renders {where}: the document, or an event number with its type when known.
     */
    private String whereLabel(final ValidationError error) {
        if (error.getEventIndex() == null) {
            return MessageTemplates.fragment("whereDocument", "the document");
        }
        final String number = String.valueOf(error.getEventIndex() + 1); // humans count from 1
        if (error.getEventType() != null) {
            return MessageTemplates.fill(
                    MessageTemplates.fragment("whereEventTyped", "event {number} ({type})"),
                    Map.of("number", number, "type", error.getEventType()));
        }
        return MessageTemplates.fill(
                MessageTemplates.fragment("whereEvent", "event {number}"), Map.of("number", number));
    }

    /**
     * Reads the zero based index out of a path segment such as eventList[5].
     */
    private Integer eventIndexOf(final String location) {
        final int marker = location.indexOf("eventList[");
        if (marker < 0) {
            return null;
        }
        final int start = marker + "eventList[".length();
        final int end = location.indexOf(']', start);
        if (end < 0) {
            return null;
        }
        try {
            return Integer.valueOf(location.substring(start, end));
        } catch (NumberFormatException e) {
            return null; // an unexpected path must never break a validation response
        }
    }

    /**
     * The field name at the end of a path, ignoring any array indexes.
     */
    private String lastSegment(final String location) {
        if (location == null || location.isEmpty() || "$".equals(location)) {
            return null; // the document root has no field name
        }
        String path = location;
        while (path.endsWith("]")) { // strip trailing indexes, e.g. epcList[0] gives epcList
            final int open = path.lastIndexOf('[');
            if (open < 0) {
                break;
            }
            path = path.substring(0, open);
        }
        final int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }

    /**
     * The value without JSON quoting. Objects and arrays are skipped so nothing floods a message.
     */
    private String plainValue(final JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isValueNode()) {
            return null;
        }
        return node.asText();
    }
}