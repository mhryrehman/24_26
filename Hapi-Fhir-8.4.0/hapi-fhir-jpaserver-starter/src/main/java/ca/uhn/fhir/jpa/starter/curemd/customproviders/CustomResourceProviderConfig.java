package ca.uhn.fhir.jpa.starter.curemd.customproviders;

import ca.uhn.fhir.rest.server.IResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CustomResourceProviderConfig {

  private static final Logger ourLog = LoggerFactory.getLogger(CustomResourceProviderConfig.class);

  @Bean
  @Primary
  public IResourceProvider customPatientProvider() {
	  ourLog.info("Registering CustomPatientResourceProvider as the primary resource provider for Patient resources");
    return new ca.uhn.fhir.jpa.starter.curemd.customproviders.CustomPatientResourceProvider();
  }
}
