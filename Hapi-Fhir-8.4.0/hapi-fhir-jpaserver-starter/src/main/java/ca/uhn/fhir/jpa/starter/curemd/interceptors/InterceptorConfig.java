package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.jpa.starter.curemd.audit.AuditService;
import ca.uhn.fhir.jpa.starter.curemd.provider.AuditOperationProvider;
import ca.uhn.fhir.jpa.starter.curemd.provider.CcdaOperationProvider;
import ca.uhn.fhir.jpa.starter.curemd.service.CcdaService;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InterceptorConfig {
	private static final Logger LGR = Logger.getLogger(InterceptorConfig.class.getName());

	@Autowired
	private IInterceptorService interceptorService;

	@Autowired
	private RestfulServer restfulServer;

	@Bean
	public CustomPartitionInterceptor partitionInterceptor(IInterceptorService interceptorService) {
		CustomPartitionInterceptor interceptor = new CustomPartitionInterceptor();
		interceptorService.registerInterceptor(interceptor);
		return interceptor;
	}

	@Bean
	@ConditionalOnProperty(
		name = "hapi.fhir.interceptors.document-content.enabled",
		havingValue = "true",
		matchIfMissing = false
	)
	public DocumentContentEmbedderInterceptor documentContentEmbedderInterceptor(IInterceptorService interceptorService) {
		DocumentContentEmbedderInterceptor interceptor = new DocumentContentEmbedderInterceptor();
		LGR.info("Creating and registering DocumentContentEmbedderInterceptor");
		restfulServer.registerInterceptor(interceptor);
		return interceptor;
	}

	@Bean
	@ConditionalOnProperty(
		name = "hapi.fhir.interceptors.audit.enabled",
		havingValue = "true",
		matchIfMissing = false
	)
	public AuditInterceptor auditInterceptor(AuditService auditService) {
		AuditInterceptor interceptor = new AuditInterceptor(auditService);
		LGR.info("Creating and registering AuditInterceptor");
		restfulServer.registerInterceptor(interceptor);
		return interceptor;
	}

	@Bean
	public AuditOperationProvider registerAuditProvider(AuditService auditService) {
		AuditOperationProvider interceptor = new AuditOperationProvider(auditService);
		LGR.info("Creating and registering AuditOperationProvider");
		restfulServer.registerProvider(interceptor);
		return interceptor;
	}

	@Bean
	public CcdaOperationProvider registerCcdaProvider(CcdaService ccdaService) {
		CcdaOperationProvider interceptor = new CcdaOperationProvider(ccdaService);
		LGR.info("Creating and registering CcdaOperationProvider");
		restfulServer.registerProvider(interceptor);
		return interceptor;
	}

}
