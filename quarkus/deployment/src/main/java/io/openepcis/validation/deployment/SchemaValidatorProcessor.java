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
    return new HealthBuildItem(SchemaValidatorHealthCheck.class.getName(),
            buildTimeConfig.healthEnabled);
  }

  @BuildStep
  NativeImageResourcePatternsBuildItem addNativeImageResourceBuildItem() {
    return NativeImageResourcePatternsBuildItem.builder().includeGlobs(
            "json-schema/*",
            "xsd/**/*"
    ).build();
  }

  @BuildStep
  NativeImageSystemPropertyBuildItem addNativeImageSystemPropertyBuildItem() {
    return new NativeImageSystemPropertyBuildItem("EnableURLProtocols", "http,https");
  }

}
