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
package io.openepcis.validation;

import io.openepcis.constants.EPCIS;
import io.openepcis.constants.EPCISVersion;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class that informs which EPCIS versions this service validates, and for which format.
 */
public class EPCISVersionSupport {

    // request header that this service reads
    public static final String VERSION_HEADER = "GS1-EPCIS-Version";

    // default header when no version is provided
    public static final EPCISVersion DEFAULT_VERSION = EPCISVersion.VERSION_2_0_1;

    // releases that share the 2.0 schema/xsd
    public static final Set<EPCISVersion> VERSION_2_0_FAMILY = EnumSet.of(EPCISVersion.VERSION_2_0_0, EPCISVersion.VERSION_2_0_1);

    // EPCIS JSON accepts the 2.0 only.
    private static final Set<EPCISVersion> JSON_VERSIONS = VERSION_2_0_FAMILY;

    // XML has support for both the 1.2 and the 2.0 XSD
    private static final Set<EPCISVersion> XML_VERSIONS = EnumSet.of(EPCISVersion.VERSION_1_2_0, EPCISVersion.VERSION_2_0_0, EPCISVersion.VERSION_2_0_1);

    private EPCISVersionSupport() {
    }

    // Reads the header value and applies the default if missing
    public static Optional<EPCISVersion> parse(final String headerValue) {
        if (StringUtils.isBlank(headerValue)) {
            return Optional.of(DEFAULT_VERSION);
        }
        return EPCISVersion.fromString(headerValue);
    }

    // Returns the supported EPCISVersions based on mediaType
    public static Set<EPCISVersion> supportedFor(final String mediaType) {
        final String format = StringUtils.isBlank(mediaType) ? "" : mediaType.toLowerCase(Locale.ROOT);

        if (format.contains(EPCIS.JSON_FORMAT)) return JSON_VERSIONS;
        if (format.contains(EPCIS.XML_FORMAT)) return XML_VERSIONS;
        return Set.of();
    }

    // Readable list of error messages if unsupported version/media
    public static String listFor(final String mediaType) {
        return supportedFor(mediaType).stream()
                .map(EPCISVersion::getVersion)
                .collect(Collectors.joining(", "));
    }
}
