/*
 * Copyright 2022-2024 benelog GmbH & Co. KG
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
package io.openepcis.validation.rest.resource;

public interface ResponseBodyExamples {

  String RESPONSE_400_VALIDATION_ERRORS =
      """
            [
              {
                "type": "required",
                "line": "#/definitions/Event/required",
                "column": "$.epcisBody.eventList[0]",
                "message": "$.epcisBody.eventList[0].eventTimeZoneOffset: is missing but it is required"
              },
              {
                "type": "required",
                "line": "#/definitions/ObjectEvent/allOf/required",
                "column": "$.epcisBody.eventList[0]",
                "message": "$.epcisBody.eventList[0].action: is missing but it is required"
              }
            ]""";

  String RESPONSE_400_VALIDATION_ERRORS_XML =
      """
            <ValidationResult>
            	<errors>
            		<ValidationError type="required">
            			<line>#/definitions/Event/required</line>
            			<column>$.epcisBody.eventList[0]</column>
            			<message>$.epcisBody.eventList[0].eventTimeZoneOffset: is missing but it is required</message>
            		</ValidationError>
            		<ValidationError type="required">
            			<line>#/definitions/ObjectEvent/allOf/required</line>
            			<column>$.epcisBody.eventList[0]</column>
            			<message>$.epcisBody.eventList[0].action: is missing but it is required</message>
            		</ValidationError>
            	</errors>
            </ValidationResult>""";
  String RESPONSE_401_UNAUTHORIZED_REQUEST =
      """
                    {
                      "type": "epcisException:SecurityException",
                      "title": "Unauthorised request",
                      "status": 401
                    }""";
  String RESPONSE_403_CLIENT_UNAUTHORIZED =
      """
                    {
                      "type": "epcisException:SecurityException",
                      "title": "Access to resource forbidden",
                      "status": 403
                    }""";
  String RESPONSE_406_NOT_ACCEPTABLE =
      """
                    {
                      "type": "epcisException:NotAcceptableException",
                      "title": "Conflicting request and response headers",
                      "status": 406
                    }""";
  String RESPONSE_415_UNSUPPORTED_MEDIA_TYPE =
      """
                    {
                      "type": "epcisException:UnsupportedMediaTypeException",
                      "title": "Unsupported Media Type",
                      "status": 415
                    }""";
  String RESPONSE_500_IMPLEMENTATION_EXCEPTION =
      """
                    {
                      "type": "epcisException:ImplementationException",
                      "title": "A server-side error occurred",
                      "status": 500
                    }""";
}
