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
package io.openepcis.validation.exception;

/**
 * Thrown when the caller asks for an EPCIS version this service cannot validate against.
 * for example EPCIS 1.2 as JSON, or 2.1.0 before its schemas are bundled.
 */
public class UnsupportedEPCISVersionException extends UnsupportedOperationException {

    public UnsupportedEPCISVersionException(final String requested, final String mediaType, final String supported) {
        super("EPCIS version '" + requested + "' is not supported for " + mediaType + ". Supported versions for this format: " + supported + ".");
    }
}
