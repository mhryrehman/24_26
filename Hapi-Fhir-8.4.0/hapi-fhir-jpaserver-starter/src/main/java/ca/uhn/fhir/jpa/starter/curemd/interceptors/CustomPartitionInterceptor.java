package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import org.springframework.util.StringUtils;

import java.util.logging.Logger;

@Interceptor
public class CustomPartitionInterceptor {
	private static final Logger logger = Logger.getLogger(CustomPartitionInterceptor.class.getName());
	private static final String DEFAULT_PARTITION = "DEFAULT";

	@Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_CREATE)
	public RequestPartitionId identifyPartitionOnCreate(RequestDetails requestDetails) {
		logger.info("STORAGE_PARTITION_IDENTIFY_CREATE triggered.");
		return extractPartitionIdFromRequest(requestDetails);
	}

	@Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
	public RequestPartitionId identifyPartitionOnRead(RequestDetails requestDetails) {
		logger.info("STORAGE_PARTITION_IDENTIFY_READ triggered.");
		return extractPartitionIdFromRequest(requestDetails);
	}

	private RequestPartitionId extractPartitionIdFromRequest(RequestDetails requestDetails) {
		if (requestDetails == null) {
			logger.info("RequestDetails is null. Assigning to default partition.");
			return RequestPartitionId.fromPartitionName(DEFAULT_PARTITION);
		}

		String tenantId = requestDetails.getTenantId();
		if (StringUtils.hasText(tenantId)) {
			logger.info("Using tenant ID from request: " +tenantId);
			return RequestPartitionId.fromPartitionName(tenantId);
		}

		if (requestDetails instanceof SystemRequestDetails) {
			logger.info("Request originated from SystemRequestDetails (internal job or batch export). Assigning to default partition.");
		}


		return RequestPartitionId.fromPartitionName(DEFAULT_PARTITION);
	}
}
