//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.partition.IRequestPartitionHelperSvc;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.SearchParameterUtil;
import jakarta.annotation.Nonnull;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.instance.model.api.IIdType;

import java.util.*;
import java.util.stream.Collectors;

public class BulkDataExportSupportYasir {
	private final FhirContext myFhirContext;
	private final DaoRegistry myDaoRegistry;
	private final IRequestPartitionHelperSvc myRequestPartitionHelperService;
	private Set<String> myCompartmentResources;

	public BulkDataExportSupportYasir(@Nonnull FhirContext theFhirContext, @Nonnull DaoRegistry theDaoRegistry, @Nonnull IRequestPartitionHelperSvc theRequestPartitionHelperService) {
		this.myFhirContext = theFhirContext;
		this.myDaoRegistry = theDaoRegistry;
		this.myRequestPartitionHelperService = theRequestPartitionHelperService;
	}

	public void validateTargetsExists(@Nonnull RequestDetails theRequestDetails, @Nonnull String theTargetResourceName, @Nonnull Iterable<IIdType> theIdParams) {
		if (theIdParams.iterator().hasNext()) {
			RequestPartitionId partitionId = this.myRequestPartitionHelperService.determineReadPartitionForRequestForRead(theRequestDetails, theTargetResourceName, (IIdType)theIdParams.iterator().next());
			SystemRequestDetails requestDetails = (new SystemRequestDetails()).setRequestPartitionId(partitionId);
			Iterator var6 = theIdParams.iterator();

			while(var6.hasNext()) {
				IIdType nextId = (IIdType)var6.next();
				this.myDaoRegistry.getResourceDao(theTargetResourceName).read(nextId, requestDetails);
			}
		}

	}

	public void validateOrDefaultResourceTypesForGroupBulkExport(@Nonnull BulkExportJobParameters theBulkExportJobParameters) {
		if (CollectionUtils.isNotEmpty(theBulkExportJobParameters.getResourceTypes())) {
			this.validateResourceTypesAllContainPatientSearchParams(theBulkExportJobParameters.getResourceTypes());
		} else {
			Set<String> groupTypes = new HashSet(this.getPatientCompartmentResources());
			groupTypes.addAll(BulkDataExportUtilYasir.PATIENT_BULK_EXPORT_FORWARD_REFERENCE_RESOURCE_TYPES);
			groupTypes.removeIf((t) -> {
				return !this.myDaoRegistry.isResourceTypeSupported(t);
			});
			theBulkExportJobParameters.setResourceTypes(groupTypes);
		}

	}

	public void validateResourceTypesAllContainPatientSearchParams(Collection<String> theResourceTypes) {
		if (theResourceTypes != null) {
			List<String> badResourceTypes = (List)theResourceTypes.stream().filter((resourceType) -> {
				return !BulkDataExportUtilYasir.PATIENT_BULK_EXPORT_FORWARD_REFERENCE_RESOURCE_TYPES.contains(resourceType);
			}).filter((resourceType) -> {
				return !this.getPatientCompartmentResources().contains(resourceType);
			}).collect(Collectors.toList());
			if (!badResourceTypes.isEmpty()) {
				String var10002 = Msg.code(512);
				throw new InvalidRequestException(var10002 + String.format("Resource types [%s] are invalid for this type of export, as they do not contain search parameters that refer to patients.", String.join(",", badResourceTypes)));
			}
		}

	}

	public Set<String> getPatientCompartmentResources() {
		return this.getPatientCompartmentResources(this.myFhirContext);
	}

	Set<String> getPatientCompartmentResources(FhirContext theFhirContext) {
		if (this.myCompartmentResources == null) {
			this.myCompartmentResources = new HashSet(SearchParameterUtil.getAllResourceTypesThatAreInPatientCompartment(theFhirContext));
		}

		return this.myCompartmentResources;
	}
}
