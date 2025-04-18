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
package io.openepcis.validation.deployment;

import io.openepcis.quarkus.deployment.model.OpenEPCISBuildTimeConfig;
import io.openepcis.validation.runtime.SchemaValidatorHealthCheck;
import io.openepcis.validation.runtime.SchemaValidatorProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class SchemaValidatorProcessor {
  private static final String FEATURE = "openepcis-schema-validator";

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE);
  }

  @BuildStep()
  AdditionalBeanBuildItem buildOpenEPCISJAXBContext() {
    return AdditionalBeanBuildItem.unremovableOf(SchemaValidatorProducer.class);
  }

  @BuildStep
  HealthBuildItem addHealthCheck(OpenEPCISBuildTimeConfig buildTimeConfig) {
    return new HealthBuildItem(
        SchemaValidatorHealthCheck.class.getName(), buildTimeConfig.healthEnabled());
  }

  @BuildStep
  NativeImageResourcePatternsBuildItem addNativeImageResourceBuildItem() {
    return NativeImageResourcePatternsBuildItem.builder()
        .includeGlobs("json-schema/*", "xsd/**/*")
        .build();
  }

  @BuildStep
  NativeImageSystemPropertyBuildItem addNativeImageSystemPropertyBuildItem() {
    return new NativeImageSystemPropertyBuildItem("EnableURLProtocols", "http,https");
  }
}
