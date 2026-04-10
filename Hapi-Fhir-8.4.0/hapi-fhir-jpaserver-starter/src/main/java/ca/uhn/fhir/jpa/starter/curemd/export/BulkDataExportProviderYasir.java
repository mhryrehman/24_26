//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.batch2.api.IJobCoordinator;
import ca.uhn.fhir.batch2.api.JobOperationResultJson;
import ca.uhn.fhir.batch2.model.JobInstance;
import ca.uhn.fhir.batch2.model.StatusEnum;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.model.BulkExportJobResults;
import ca.uhn.fhir.jpa.bulk.export.model.BulkExportResponseJson;
import ca.uhn.fhir.jpa.partition.IRequestPartitionHelperSvc;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters.ExportStyle;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.JsonUtil;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import com.google.common.annotations.VisibleForTesting;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BulkDataExportProviderYasir {
	private static final Logger ourLog = LoggerFactory.getLogger(BulkDataExportProviderYasir.class);
	@Autowired
	private IInterceptorBroadcaster myInterceptorBroadcaster;
	@Autowired
	private FhirContext myFhirContext;
	@Autowired
	private IJobCoordinator myJobCoordinator;
	@Autowired
	private JpaStorageSettings myStorageSettings;
	@Autowired
	private DaoRegistry myDaoRegistry;
	@Autowired
	private IRequestPartitionHelperSvc myRequestPartitionHelperService;
	private BulkExportJobServiceYasir myBulkExportJobService;
	private BulkDataExportSupportYasir myBulkDataExportSupport;

	public BulkDataExportProviderYasir() {
	}

	@Operation(
			name = "$export",
			global = false,
			manualResponse = true,
			idempotent = true,
			canonicalUrl = "http://hl7.org/fhir/uv/bulkdata/OperationDefinition/export"
	)
	public void export(@OperationParam(name = "_outputFormat",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theOutputFormat, @OperationParam(name = "_type",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theType, @OperationParam(name = "_since",min = 0,max = 1,typeName = "instant") IPrimitiveType<Date> theSince, @OperationParam(name = "_typeFilter",min = 0,max = -1,typeName = "string") List<IPrimitiveType<String>> theTypeFilter, @OperationParam(name = "_typePostFetchFilterUrl",min = 0,max = -1,typeName = "string") List<IPrimitiveType<String>> theTypePostFetchFilterUrl, @OperationParam(name = "_exportId",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theExportId, ServletRequestDetails theRequestDetails) {
		ourLog.info("Received Custom Bulk Export Request");
		BulkDataExportUtilYasir.validatePreferAsyncHeader(theRequestDetails, "$export");
		BulkExportJobParameters bulkExportJobParameters = (new BulkExportJobParametersBuilderYasir()).outputFormat(theOutputFormat).resourceTypes(theType).since(theSince).filters(theTypeFilter).exportIdentifier(theExportId).exportStyle(ExportStyle.SYSTEM).postFetchFilterUrl(theTypePostFetchFilterUrl).build();
		this.getBulkDataExportJobService().startJob(theRequestDetails, bulkExportJobParameters);
	}

	@Operation(
			name = "$export",
			manualResponse = true,
			idempotent = true,
			typeName = "Group",
			canonicalUrl = "http://hl7.org/fhir/uv/bulkdata/OperationDefinition/group-export"
	)
	public void groupExport(@IdParam IIdType theIdParam, @OperationParam(name = "_outputFormat",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theOutputFormat, @OperationParam(name = "_type",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theType, @OperationParam(name = "_since",min = 0,max = 1,typeName = "instant") IPrimitiveType<Date> theSince, @OperationParam(name = "_typeFilter",min = 0,max = -1,typeName = "string") List<IPrimitiveType<String>> theTypeFilter, @OperationParam(name = "_typePostFetchFilterUrl",min = 0,max = -1,typeName = "string") List<IPrimitiveType<String>> theTypePostFetchFilterUrl, @OperationParam(name = "_mdm",min = 0,max = 1,typeName = "boolean") IPrimitiveType<Boolean> theMdm, @OperationParam(name = "_exportId",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theExportIdentifier, ServletRequestDetails theRequestDetails) {
		ourLog.debug("Received Group Bulk Export Request for Group {}", theIdParam);
		ourLog.debug("_type={}", theType);
		ourLog.debug("_since={}", theSince);
		ourLog.debug("_typeFilter={}", theTypeFilter);
		ourLog.debug("_mdm={}", theMdm);
		BulkDataExportUtilYasir.validatePreferAsyncHeader(theRequestDetails, "$export");
		this.getBulkDataExportSupport().validateTargetsExists(theRequestDetails, "Group", List.of(theIdParam));
		BulkExportJobParameters bulkExportJobParameters = (new BulkExportJobParametersBuilderYasir()).outputFormat(theOutputFormat).resourceTypes(theType).since(theSince).filters(theTypeFilter).exportIdentifier(theExportIdentifier).exportStyle(ExportStyle.GROUP).postFetchFilterUrl(theTypePostFetchFilterUrl).groupId(theIdParam).expandMdm(theMdm).build();
		this.getBulkDataExportSupport().validateOrDefaultResourceTypesForGroupBulkExport(bulkExportJobParameters);
		this.getBulkDataExportJobService().startJob(theRequestDetails, bulkExportJobParameters);
	}

	@VisibleForTesting
	Set<String> getPatientCompartmentResources(FhirContext theFhirContext) {
		return this.getBulkDataExportSupport().getPatientCompartmentResources(theFhirContext);
	}

	@Operation(
			name = "$export",
			manualResponse = true,
			idempotent = true,
			typeName = "Patient",
			canonicalUrl = "http://hl7.org/fhir/uv/bulkdata/OperationDefinition/patient-export"
	)
	public void patientExport(@OperationParam(name = "_outputFormat",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theOutputFormat, @OperationParam(name = "_type",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theType, @OperationParam(name = "_since",min = 0,max = 1,typeName = "instant") IPrimitiveType<Date> theSince, @OperationParam(name = "_typeFilter",min = 0,max = -1,typeName = "string") List<IPrimitiveType<String>> theTypeFilter, @OperationParam(name = "_typePostFetchFilterUrl",min = 0,max = -1,typeName = "string") List<IPrimitiveType<String>> theTypePostFetchFilterUrl, @OperationParam(name = "patient",min = 0,max = -1,typeName = "string") List<IPrimitiveType<String>> thePatient, @OperationParam(name = "_exportId",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theExportIdentifier, ServletRequestDetails theRequestDetails) {
		ourLog.info("Received Patient Bulk Export Request");
		List<IPrimitiveType<String>> patientIds = thePatient != null ? thePatient : new ArrayList();
		this.doPatientExport(theRequestDetails, theOutputFormat, theType, theSince, theExportIdentifier, theTypeFilter, theTypePostFetchFilterUrl, (List)patientIds);
	}

	@Operation(
			name = "$export",
			manualResponse = true,
			idempotent = true,
			typeName = "Patient"
	)
	public void patientInstanceExport(@IdParam IIdType theIdParam, @OperationParam(name = "_outputFormat",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theOutputFormat, @OperationParam(name = "_type",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theType, @OperationParam(name = "_since",min = 0,max = 1,typeName = "instant") IPrimitiveType<Date> theSince, @OperationParam(name = "_typeFilter",min = 0,max = -1,typeName = "string") List<IPrimitiveType<String>> theTypeFilter, @OperationParam(name = "_typePostFetchFilterUrl",min = 0,max = -1,typeName = "string") List<IPrimitiveType<String>> theTypePostFetchFilterUrl, @OperationParam(name = "_exportId",min = 0,max = 1,typeName = "string") IPrimitiveType<String> theExportIdentifier, ServletRequestDetails theRequestDetails) {
		this.patientExport(theOutputFormat, theType, theSince, theTypeFilter, theTypePostFetchFilterUrl, List.of(theIdParam), theExportIdentifier, theRequestDetails);
	}

	private void doPatientExport(ServletRequestDetails theRequestDetails, IPrimitiveType<String> theOutputFormat, IPrimitiveType<String> theType, IPrimitiveType<Date> theSince, IPrimitiveType<String> theExportIdentifier, List<IPrimitiveType<String>> theTypeFilter, List<IPrimitiveType<String>> theTypePostFetchFilterUrl, List<IPrimitiveType<String>> thePatientIds) {
		BulkDataExportUtilYasir.validatePreferAsyncHeader(theRequestDetails, "$export");
		this.getBulkDataExportSupport().validateTargetsExists(theRequestDetails, "Patient", (Iterable)thePatientIds.stream().map((c) -> {
			return new IdType((String)c.getValue());
		}).collect(Collectors.toList()));
		IPrimitiveType<String> resourceTypes = theType == null ? new StringDt(String.join(",", this.getBulkDataExportSupport().getPatientCompartmentResources())) : theType;
		BulkExportJobParameters bulkExportJobParameters = (new BulkExportJobParametersBuilderYasir()).outputFormat(theOutputFormat).resourceTypes((IPrimitiveType)resourceTypes).since(theSince).filters(theTypeFilter).exportIdentifier(theExportIdentifier).exportStyle(ExportStyle.PATIENT).postFetchFilterUrl(theTypePostFetchFilterUrl).patientIds(thePatientIds).build();
		this.getBulkDataExportSupport().validateResourceTypesAllContainPatientSearchParams(bulkExportJobParameters.getResourceTypes());
		this.getBulkDataExportJobService().startJob(theRequestDetails, bulkExportJobParameters);
	}

	@Operation(
			name = "$export-poll-status",
			manualResponse = true,
			idempotent = true,
			deleteEnabled = true
	)
	public void exportPollStatus(@OperationParam(name = "_jobId",typeName = "string",min = 0,max = 1) IPrimitiveType<String> theJobId, ServletRequestDetails theRequestDetails) throws IOException {
		ourLog.info("Received request for export poll status for Job ID: {}", theJobId != null ? theJobId.getValue() : "null");
		HttpServletResponse response = theRequestDetails.getServletResponse();
		theRequestDetails.getServer().addHeadersToResponse(response);
		if (theJobId == null) {
			Parameters parameters = (Parameters)theRequestDetails.getResource();
			Parameters.ParametersParameterComponent parameter = (Parameters.ParametersParameterComponent)parameters.getParameter().stream().filter((param) -> {
				return param.getName().equals("_jobId");
			}).findFirst().orElseThrow(() -> {
				return new InvalidRequestException(Msg.code(2227) + "$export-poll-status requires a job ID, please provide the value of target jobId.");
			});
			theJobId = (IPrimitiveType)parameter.getValue();
		}

		JobInstance info = this.myJobCoordinator.getInstance(theJobId.getValueAsString());
		BulkExportJobParameters parameters = (BulkExportJobParameters)info.getParameters(BulkExportJobParameters.class);
		String var10002;
		if (parameters.getPartitionId() != null) {
			RequestPartitionId partitionId = this.myRequestPartitionHelperService.determineReadPartitionForRequestForServerOperation(theRequestDetails, "$export-poll-status");
			this.myRequestPartitionHelperService.validateHasPartitionPermissions(theRequestDetails, "Binary", partitionId);
			if (!parameters.getPartitionId().equals(partitionId)) {
				var10002 = Msg.code(2304);
				throw new InvalidRequestException(var10002 + "Invalid partition in request for Job ID " + String.valueOf(theJobId));
			}
		}

		String report;
		switch (info.getStatus()) {
			case COMPLETED:
				if (theRequestDetails.getRequestType() == RequestTypeEnum.DELETE) {
					this.handleDeleteRequest(theJobId, response, info.getStatus());
				} else {
					response.setStatus(200);
					response.setContentType("application/json");
					BulkExportResponseJson bulkResponseDocument = new BulkExportResponseJson();
					bulkResponseDocument.setTransactionTime(info.getEndTime());
					bulkResponseDocument.setRequiresAccessToken(true);
					report = info.getReport();
					if (StringUtils.isEmpty(report)) {
						ourLog.error("No report for completed bulk export job.");
						response.getWriter().close();
					} else {
						BulkExportJobResults results = (BulkExportJobResults)JsonUtil.deserialize(report, BulkExportJobResults.class);
						bulkResponseDocument.setMsg(results.getReportMsg());
						bulkResponseDocument.setRequest(results.getOriginalRequestUrl());
						String serverBase = BulkDataExportUtilYasir.getServerBase(theRequestDetails);
						bulkResponseDocument.getOutput();
						Iterator var10 = results.getResourceTypeToBinaryIds().entrySet().iterator();

						while(var10.hasNext()) {
							Map.Entry<String, List<String>> entrySet = (Map.Entry)var10.next();
							String resourceType = (String)entrySet.getKey();
							List<String> binaryIds = (List)entrySet.getValue();
							Iterator var14 = binaryIds.iterator();

							while(var14.hasNext()) {
								String binaryId = (String)var14.next();
								IIdType iId = new IdType(binaryId);
								String nextUrl = serverBase + "/" + iId.toUnqualifiedVersionless().getValue();
								bulkResponseDocument.addOutput().setType(resourceType).setUrl(nextUrl);
							}
						}

						JsonUtil.serialize(bulkResponseDocument, response.getWriter());
						response.getWriter().close();
					}
				}
				break;
			case FAILED:
				response.setStatus(500);
				response.setContentType("application/json+fhir");
				IBaseOperationOutcome oo = OperationOutcomeUtil.newInstance(this.myFhirContext);
				OperationOutcomeUtil.addIssue(this.myFhirContext, oo, "error", info.getErrorMessage(), (String)null, (String)null);
				this.myFhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToWriter(oo, response.getWriter());
				response.getWriter().close();
				break;
			default:
				ourLog.warn("Unrecognized status encountered: {}. Treating as BUILDING/SUBMITTED", info.getStatus().name());
			case FINALIZE:
			case QUEUED:
			case IN_PROGRESS:
			case CANCELLED:
			case ERRORED:
				if (theRequestDetails.getRequestType() == RequestTypeEnum.DELETE) {
					this.handleDeleteRequest(theJobId, response, info.getStatus());
				} else {
					response.setStatus(202);
					report = this.getTransitionTimeOfJobInfo(info);
					var10002 = String.valueOf(info.getStatus());
					response.addHeader("X-Progress", "Build in progress - Status set to " + var10002 + " at " + report);
					response.addHeader("Retry-After", "120");
				}
		}

	}

	private void handleDeleteRequest(IPrimitiveType<String> theJobId, HttpServletResponse response, StatusEnum theOrigStatus) throws IOException {
		IBaseOperationOutcome outcome = OperationOutcomeUtil.newInstance(this.myFhirContext);
		JobOperationResultJson resultMessage = this.myJobCoordinator.cancelInstance(theJobId.getValueAsString());
		if (theOrigStatus.equals(StatusEnum.COMPLETED)) {
			response.setStatus(404);
			OperationOutcomeUtil.addIssue(this.myFhirContext, outcome, "error", "Job instance <" + theJobId.getValueAsString() + "> was already cancelled or has completed.  Nothing to do.", (String)null, (String)null);
		} else {
			response.setStatus(202);
			OperationOutcomeUtil.addIssue(this.myFhirContext, outcome, "information", resultMessage.getMessage(), (String)null, "informational");
		}

		this.myFhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToWriter(outcome, response.getWriter());
		response.getWriter().close();
	}

	private String getTransitionTimeOfJobInfo(JobInstance theInfo) {
		if (theInfo.getEndTime() != null) {
			return (new InstantType(theInfo.getEndTime())).getValueAsString();
		} else {
			return theInfo.getStartTime() != null ? (new InstantType(theInfo.getStartTime())).getValueAsString() : "";
		}
	}

	@VisibleForTesting
	public void setStorageSettings(JpaStorageSettings theStorageSettings) {
		this.myStorageSettings = theStorageSettings;
	}

	@VisibleForTesting
	public void setDaoRegistry(DaoRegistry theDaoRegistry) {
		this.myDaoRegistry = theDaoRegistry;
	}

	private BulkExportJobServiceYasir getBulkDataExportJobService() {
		if (this.myBulkExportJobService == null) {
			this.myBulkExportJobService = new BulkExportJobServiceYasir(this.myInterceptorBroadcaster, this.myJobCoordinator, this.myDaoRegistry, this.myRequestPartitionHelperService, this.myStorageSettings);
		}

		return this.myBulkExportJobService;
	}

	private BulkDataExportSupportYasir getBulkDataExportSupport() {
		if (this.myBulkDataExportSupport == null) {
			this.myBulkDataExportSupport = new BulkDataExportSupportYasir(this.myFhirContext, this.myDaoRegistry, this.myRequestPartitionHelperService);
		}

		return this.myBulkDataExportSupport;
	}
}
