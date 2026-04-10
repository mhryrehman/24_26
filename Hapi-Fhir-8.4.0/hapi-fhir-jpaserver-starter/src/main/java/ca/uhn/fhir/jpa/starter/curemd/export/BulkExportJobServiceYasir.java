//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.batch2.api.IJobCoordinator;
import ca.uhn.fhir.batch2.model.JobInstanceStartRequest;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.batch.models.Batch2JobStartResponse;
import ca.uhn.fhir.jpa.partition.IRequestPartitionHelperSvc;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.rest.server.util.CompositeInterceptorBroadcaster;
import ca.uhn.fhir.util.UrlUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class BulkExportJobServiceYasir {
	private final IInterceptorBroadcaster myInterceptorBroadcaster;
	private final IJobCoordinator myJobCoordinator;
	private final DaoRegistry myDaoRegistry;
	private final IRequestPartitionHelperSvc myRequestPartitionHelperService;
	private final JpaStorageSettings myStorageSettings;

	@Autowired
	private ca.uhn.fhir.batch2.coordinator.JobDefinitionRegistry myJobDefinitionRegistry;

	public BulkExportJobServiceYasir(@Nonnull IInterceptorBroadcaster theInterceptorBroadcaster, @Nonnull IJobCoordinator theJobCoordinator, @Nonnull DaoRegistry theDaoRegistry, @Nonnull IRequestPartitionHelperSvc theRequestPartitionHelperService, @Nonnull JpaStorageSettings theStorageSettings) {
		this.myInterceptorBroadcaster = theInterceptorBroadcaster;
		this.myJobCoordinator = theJobCoordinator;
		this.myDaoRegistry = theDaoRegistry;
		this.myRequestPartitionHelperService = theRequestPartitionHelperService;
		this.myStorageSettings = theStorageSettings;
	}

	public void startJob(@Nonnull ServletRequestDetails theRequestDetails, @Nonnull BulkExportJobParameters theBulkExportJobParameters) {
		this.expandParameters(theRequestDetails, theBulkExportJobParameters);
		this.callBulkExportHooks(theRequestDetails, theBulkExportJobParameters);

		boolean useCache = this.shouldUseCache(theRequestDetails);
		JobInstanceStartRequest startRequest = new JobInstanceStartRequest();
		startRequest.setParameters(theBulkExportJobParameters);
		startRequest.setUseCache(useCache);
		startRequest.setJobDefinitionId("BULK_EXPORT_YASIR");
		Batch2JobStartResponse response = this.myJobCoordinator.startInstance(theRequestDetails, startRequest);
		this.writePollingLocationToResponseHeaders(theRequestDetails, response.getInstanceId());
	}

	private void expandParameters(@Nonnull ServletRequestDetails theRequestDetails, @Nonnull BulkExportJobParameters theBulkExportJobParameters) {
		theBulkExportJobParameters.setOriginalRequestUrl(theRequestDetails.getCompleteUrl());
		if (theBulkExportJobParameters.getResourceTypes().isEmpty()) {
			List<String> resourceTypes = new ArrayList(this.myDaoRegistry.getRegisteredDaoTypes());
			resourceTypes.remove("Binary");
			theBulkExportJobParameters.setResourceTypes(resourceTypes);
		}

		RequestPartitionId partitionId = this.myRequestPartitionHelperService.determineReadPartitionForRequestForServerOperation(theRequestDetails, "$export");
		this.myRequestPartitionHelperService.validateHasPartitionPermissions(theRequestDetails, "Binary", partitionId);
		theBulkExportJobParameters.setPartitionId(partitionId);
	}

	private void callBulkExportHooks(@Nonnull ServletRequestDetails theRequestDetails, @Nonnull BulkExportJobParameters theBulkExportJobParameters) {
		IInterceptorBroadcaster compositeBroadcaster = CompositeInterceptorBroadcaster.newCompositeBroadcaster(this.myInterceptorBroadcaster, theRequestDetails);
		HookParams initiateBulkExportHookParams;
		if (compositeBroadcaster.hasHooks(Pointcut.STORAGE_PRE_INITIATE_BULK_EXPORT)) {
			initiateBulkExportHookParams = (new HookParams()).add(BulkExportJobParameters.class, theBulkExportJobParameters).add(RequestDetails.class, theRequestDetails).addIfMatchesType(ServletRequestDetails.class, theRequestDetails);
			compositeBroadcaster.callHooks(Pointcut.STORAGE_PRE_INITIATE_BULK_EXPORT, initiateBulkExportHookParams);
		}

		if (compositeBroadcaster.hasHooks(Pointcut.STORAGE_INITIATE_BULK_EXPORT)) {
			initiateBulkExportHookParams = (new HookParams()).add(BulkExportJobParameters.class, theBulkExportJobParameters).add(RequestDetails.class, theRequestDetails).addIfMatchesType(ServletRequestDetails.class, theRequestDetails);
			compositeBroadcaster.callHooks(Pointcut.STORAGE_INITIATE_BULK_EXPORT, initiateBulkExportHookParams);
		}

	}

	private boolean shouldUseCache(@Nonnull ServletRequestDetails theRequestDetails) {
		CacheControlDirective cacheControlDirective = (new CacheControlDirective()).parse(theRequestDetails.getHeaders("Cache-Control"));
		return this.myStorageSettings.getEnableBulkExportJobReuse() && !cacheControlDirective.isNoCache();
	}

	private void writePollingLocationToResponseHeaders(@Nonnull ServletRequestDetails theRequestDetails, @Nonnull String theInstanceId) {
		String serverBase = BulkDataExportUtilYasir.getServerBase(theRequestDetails);
		if (serverBase == null) {
			throw new InternalErrorException(Msg.code(2136) + "Unable to get the server base.");
		} else {
			String pollLocation = serverBase + "/$export-poll-status?_jobId=" + theInstanceId;
			pollLocation = UrlUtil.sanitizeHeaderValue(pollLocation);
			HttpServletResponse response = theRequestDetails.getServletResponse();
			theRequestDetails.getServer().addHeadersToResponse(response);
			response.addHeader("Content-Location", pollLocation);
			response.setStatus(202);
		}
	}
}
