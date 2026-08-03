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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.List;
import java.util.Locale;

/**
 * Finds the closest allowed value to what the user typed, so we can offer a "did you mean" option in the validation error message.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValueSuggester {
    private static final LevenshteinDistance LEVENSHTEIN = LevenshteinDistance.getDefaultInstance();

    /**
     * Returns the closest candidate or null when nothing is close enough. Uses distance and LEVENSHTEIN.
     */
    public static String closest(final String wrongValue, final List<String> candidates) {
        if (StringUtils.isBlank(wrongValue) || CollectionUtils.isEmpty(candidates)) {
            return null;
        }

        final String needle = wrongValue.toLowerCase(Locale.ROOT);
        final int needleLength = needle.length();
        final int maxDistance = Math.max(1, needleLength / 3);

        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (final String candidate : candidates) {
            // Combined guard checks using short-circuit logic instead of separate continue statements
            if (StringUtils.isNotBlank(candidate) && Math.abs(needleLength - candidate.length()) <= maxDistance) {

                // Execution: Reuses the stateless global engine
                final int distance = LEVENSHTEIN.apply(needle, candidate.toLowerCase(Locale.ROOT));

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;

                    // Performance optimization: Stop searching immediately upon an exact match
                    if (bestDistance == 0) {
                        return best;
                    }
                }
            }
        }

        return bestDistance <= maxDistance ? best : null;
    }
}