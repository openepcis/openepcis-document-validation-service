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

import io.openepcis.validation.message.ValueSuggester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Validate the "did you mean" suggestions. Also avoid wrong suggestions based on attributes or values.
 */
class ValueSuggesterTest {

    // The CBV business steps enum that is the largest vocabulary in the EPCIS schema.
    private static final List<String> BIZ_STEPS =
            List.of(
                    "accepting", "arriving", "assembling", "collecting", "commissioning", "consigning",
                    "creating_class_instance", "cycle_counting", "decommissioning", "departing", "destroying",
                    "disassembling", "dispensing", "encoding", "entering_exiting", "holding", "inspecting",
                    "installing", "killing", "loading", "other", "packing", "picking", "receiving", "removing",
                    "repackaging", "repairing", "replacing", "reserving", "retail_selling", "shipping",
                    "staging_outbound", "stock_taking", "stocking", "storing", "transporting", "unloading",
                    "unpacking", "void_shipping", "sensor_reporting", "sampling");

    private static final List<String> ACTIONS = List.of("OBSERVE", "ADD", "DELETE");

    // Map of user provided values and potential suggestions
    @ParameterizedTest(name = "\"{0}\" should suggest \"{1}\"")
    @CsvSource({
            "transforming, transporting", // the value from the original user report
            "shiping, shipping", // one missing letter
            "commisioning, commissioning", // one missing letter in a long word
            "recieving, receiving", // two letters transposed
            "SHIPPING, shipping", // right word, wrong case
            "Packing, packing", // capitalized first letter
            "unpackng, unpacking" // one dropped letter
    })
    @DisplayName("suggests the closest business step for a near miss")
    void suggestsClosestBizStep(final String wrongValue, final String expected) {
        assertEquals(expected, ValueSuggester.closest(wrongValue, BIZ_STEPS));
    }

    @ParameterizedTest(name = "\"{0}\" should suggest nothing")
    @CsvSource({
            "CREATE", // a plausible looking action that is nowhere near a business step
            "totally_unrelated_word",
            "xyz",
            "a"
    })
    @DisplayName("stays quiet when nothing is close enough to be useful")
    void staysQuietWhenNothingIsClose(final String wrongValue) {
        assertNull(ValueSuggester.closest(wrongValue, BIZ_STEPS));
    }

    @Test
    @DisplayName("stays quiet for a wrong action, because none of the three is close to CREATE")
    void noSuggestionForWrongAction() {
        assertNull(ValueSuggester.closest("CREATE", ACTIONS));
    }

    @Test
    @DisplayName("suggests the right action when only the case is wrong")
    void suggestsActionOnCaseMismatch() {
        assertEquals("OBSERVE", ValueSuggester.closest("observe", ACTIONS));
    }

    @Test
    @DisplayName("handles null and empty input without throwing")
    void handlesMissingInput() {
        assertNull(ValueSuggester.closest(null, BIZ_STEPS));
        assertNull(ValueSuggester.closest("", BIZ_STEPS));
        assertNull(ValueSuggester.closest("   ", BIZ_STEPS));
        assertNull(ValueSuggester.closest("shiping", null));
        assertNull(ValueSuggester.closest("shiping", List.of()));
    }

    @Test
    @DisplayName("a longer wrong value tolerates proportionally more difference")
    void toleranceScalesWithLength() {
        // the threshold is a third of the length, so a long value may differ by more characters
        assertEquals("creating_class_instance", ValueSuggester.closest("creating_class_instanc", BIZ_STEPS));
        // but a short value must be almost exact
        assertNull(ValueSuggester.closest("add", List.of("OBSERVE", "DELETE")));
    }
}
