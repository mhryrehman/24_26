//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.batch2.api.*;
import ca.uhn.fhir.batch2.jobs.chunk.TypedPidJson;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.executor.InterceptorService;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.PersistentIdToForcedIdMap;
import ca.uhn.fhir.jpa.api.svc.IIdHelperService;
import ca.uhn.fhir.jpa.bulk.export.api.IBulkExportProcessor;
import ca.uhn.fhir.jpa.dao.tx.IHapiTransactionService;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.searchparam.extractor.ResourceIndexedSearchParams;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryMatchResult;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryResourceMatcher;
import ca.uhn.fhir.jpa.starter.curemd.export.models.ExpandedResourcesListYasir;
import ca.uhn.fhir.jpa.starter.curemd.export.models.ResourceIdListYasir;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import ca.uhn.fhir.rest.api.server.storage.IResourcePersistentId;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.server.interceptor.ResponseTerminologyTranslationSvc;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import jakarta.annotation.Nonnull;
import org.apache.commons.collections4.ListUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

public class ExpandResourcesStepYasir implements IJobStepWorker<BulkExportJobParameters, ResourceIdListYasir, ExpandedResourcesListYasir> {
	private static final Logger ourLog = LoggerFactory.getLogger(ExpandResourcesStepYasir.class);
	@Autowired
	private DaoRegistry myDaoRegistry;
	@Autowired
	private FhirContext myFhirContext;
	@Autowired
	private IBulkExportProcessor<?> myBulkExportProcessor;
	@Autowired
	private ApplicationContext myApplicationContext;
	@Autowired
	private JpaStorageSettings myStorageSettings;
	@Autowired
	private IIdHelperService myIdHelperService;
	@Autowired
	private IHapiTransactionService myTransactionService;
	@Autowired
	private InMemoryResourceMatcher myInMemoryResourceMatcher;
	@Autowired
	private InterceptorService myInterceptorService;
	private volatile ResponseTerminologyTranslationSvc myResponseTerminologyTranslationSvc;

	public ExpandResourcesStepYasir() {
	}

	@Nonnull
	public RunOutcome run(@Nonnull StepExecutionDetails<BulkExportJobParameters, ResourceIdListYasir> theStepExecutionDetails, @Nonnull IJobDataSink<ExpandedResourcesListYasir> theDataSink) throws JobExecutionFailedException {
		String instanceId = theStepExecutionDetails.getInstance().getInstanceId();
		String chunkId = theStepExecutionDetails.getChunkId();
		ResourceIdListYasir data = (ResourceIdListYasir)theStepExecutionDetails.getData();
		BulkExportJobParameters parameters = (BulkExportJobParameters)theStepExecutionDetails.getParameters();
		ourLog.info("Bulk export instance[{}] chunk[{}] - About to expand {} resource IDs into their full resource bodies.", new Object[]{instanceId, chunkId, data.getIds().size()});
		List<List<TypedPidJson>> idLists = ListUtils.partition(data.getIds(), 100);
		Iterator var8 = idLists.iterator();

		while(var8.hasNext()) {
			List<TypedPidJson> idList = (List)var8.next();
			List<IBaseResource> allResources = this.fetchAllResources(idList, parameters.getPartitionId());
//			List<IBaseResource> allResources = this.fetchFrom10gApi(idList, "Patient");
			String resourceType = data.getResourceType();
			List<String> postFetchFilterUrls = (List)parameters.getPostFetchFilterUrls().stream().filter((t) -> {
				return t.substring(0, t.indexOf(63)).equals(resourceType);
			}).collect(Collectors.toList());
			if (!postFetchFilterUrls.isEmpty()) {
				this.applyPostFetchFiltering(allResources, postFetchFilterUrls, instanceId, chunkId);
			}

			if (parameters.isExpandMdm()) {
				this.myBulkExportProcessor.expandMdmResources(allResources);
			}

			if (this.myStorageSettings.isNormalizeTerminologyForBulkExportJobs()) {
				ResponseTerminologyTranslationSvc terminologyTranslationSvc = this.myResponseTerminologyTranslationSvc;
				if (terminologyTranslationSvc == null) {
					terminologyTranslationSvc = (ResponseTerminologyTranslationSvc)this.myApplicationContext.getBean(ResponseTerminologyTranslationSvc.class);
					this.myResponseTerminologyTranslationSvc = terminologyTranslationSvc;
				}

				terminologyTranslationSvc.processResourcesForTerminologyTranslation(allResources);
			}

			if (this.myInterceptorService.hasHooks(Pointcut.STORAGE_BULK_EXPORT_RESOURCE_INCLUSION)) {
				Iterator<IBaseResource> iter = allResources.iterator();

				while(iter.hasNext()) {
					HookParams params = (new HookParams()).add(BulkExportJobParameters.class, (BulkExportJobParameters)theStepExecutionDetails.getParameters()).add(IBaseResource.class, (IBaseResource)iter.next());
					boolean outcome = this.myInterceptorService.callHooks(Pointcut.STORAGE_BULK_EXPORT_RESOURCE_INCLUSION, params);
					if (!outcome) {
						iter.remove();
					}
				}
			}

			ListMultimap<String, String> resources = this.encodeToString(allResources, parameters);
			long maxFileSize = this.myStorageSettings.getBulkExportFileMaximumSize();
			long currentFileSize = 0L;

			for(Iterator var18 = resources.keySet().iterator(); var18.hasNext(); ourLog.info("Expanding of {} resources of type {} completed", idList.size(), data.getResourceType())) {
				String nextResourceType = (String)var18.next();
				List<String> stringifiedResources = resources.get(nextResourceType);
				List<String> currentFileStringifiedResources = new ArrayList();

				String nextStringifiedResource;
				for(Iterator var22 = stringifiedResources.iterator(); var22.hasNext(); currentFileSize += (long)nextStringifiedResource.length()) {
					nextStringifiedResource = (String)var22.next();
					if (currentFileSize + (long)nextStringifiedResource.length() > maxFileSize && !currentFileStringifiedResources.isEmpty()) {
						ExpandedResourcesListYasir output = new ExpandedResourcesListYasir();
						output.setStringifiedResources(currentFileStringifiedResources);
						output.setResourceType(nextResourceType);
						theDataSink.accept(output);
						currentFileStringifiedResources = new ArrayList();
						currentFileSize = 0L;
					}

					currentFileStringifiedResources.add(nextStringifiedResource);
				}

				if (!currentFileStringifiedResources.isEmpty()) {
					ExpandedResourcesListYasir output = new ExpandedResourcesListYasir();
					output.setStringifiedResources(currentFileStringifiedResources);
					output.setResourceType(nextResourceType);
					theDataSink.accept(output);
				}
			}
		}

		return RunOutcome.SUCCESS;
	}

	private List<IBaseResource> fetchFrom10gApi(List<TypedPidJson> idList, String resourceType) {
		List<IBaseResource> fhirResources = new ArrayList<>();

		if (!"Patient".equals(resourceType)) {
			ourLog.warn("10g API mock only supports 'Patient' resource for now.");
			return fhirResources;
		}

		// Patient 1
		Patient patient1 = new Patient();
		patient1.setId("Patient/99502");
		patient1.addIdentifier()
			.setSystem("http://hospital.smarthealth.org/mrn")
			.setValue("99502");
		patient1.addName()
			.setFamily("Smith")
			.addGiven("John");
		patient1.setGender(Enumerations.AdministrativeGender.MALE);
		patient1.setBirthDateElement(new DateType("1980-01-01"));

		// Patient 2
		Patient patient2 = new Patient();
		patient2.setId("Patient/99503");
		patient2.addIdentifier()
			.setSystem("http://hospital.smarthealth.org/mrn")
			.setValue("99503");
		patient2.addName()
			.setFamily("Doe")
			.addGiven("Jane");
		patient2.setGender(Enumerations.AdministrativeGender.FEMALE);
		patient2.setBirthDateElement(new DateType("1990-05-10"));

		// Patient 3
		Patient patient3 = new Patient();
		patient3.setId("Patient/99504");
		patient3.addIdentifier()
			.setSystem("http://hospital.smarthealth.org/mrn")
			.setValue("99504");
		patient3.addName()
			.setFamily("Brown")
			.addGiven("Emily");
		patient3.setGender(Enumerations.AdministrativeGender.FEMALE);
		patient3.setBirthDateElement(new DateType("2000-12-25"));

		// Add to list
		fhirResources.add(patient1);
		fhirResources.add(patient2);
		fhirResources.add(patient3);

		return fhirResources;
	}


	private void applyPostFetchFiltering(List<IBaseResource> theResources, List<String> thePostFetchFilterUrls, String theInstanceId, String theChunkId) {
		int numRemoved = 0;
		Iterator<IBaseResource> iter = theResources.iterator();

		while(iter.hasNext()) {
			boolean matched = this.applyPostFetchFilteringForSingleResource(thePostFetchFilterUrls, iter);
			if (!matched) {
				iter.remove();
				++numRemoved;
			}
		}

		if (numRemoved > 0) {
			ourLog.info("Bulk export instance[{}] chunk[{}] - {} resources were filtered out because of post-fetch filter URLs", new Object[]{theInstanceId, theChunkId, numRemoved});
		}

	}

	private boolean applyPostFetchFilteringForSingleResource(List<String> thePostFetchFilterUrls, Iterator<IBaseResource> iter) {
		IBaseResource nextResource = (IBaseResource)iter.next();
		String nextResourceType = this.myFhirContext.getResourceType(nextResource);
		Iterator var5 = thePostFetchFilterUrls.iterator();

		while(var5.hasNext()) {
			String nextPostFetchFilterUrl = (String)var5.next();
			if (nextPostFetchFilterUrl.contains("?")) {
				String resourceType = nextPostFetchFilterUrl.substring(0, nextPostFetchFilterUrl.indexOf(63));
				if (nextResourceType.equals(resourceType)) {
					InMemoryMatchResult matchResult = this.myInMemoryResourceMatcher.match(nextPostFetchFilterUrl, nextResource, (ResourceIndexedSearchParams)null, new SystemRequestDetails());
					if (matchResult.matched()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private List<IBaseResource> fetchAllResources(List<TypedPidJson> theIds, RequestPartitionId theRequestPartitionId) {
		ArrayListMultimap<String, TypedPidJson> typeToIds = ArrayListMultimap.create();
		theIds.forEach((t) -> {
			typeToIds.put(t.getResourceType(), t);
		});
		List<IBaseResource> resources = new ArrayList(theIds.size());
		Iterator var5 = typeToIds.keySet().iterator();

		while(var5.hasNext()) {
			String resourceType = (String)var5.next();
			IFhirResourceDao<?> dao = this.myDaoRegistry.getResourceDao(resourceType);
			List<TypedPidJson> allIds = typeToIds.get(resourceType);
			Set<IResourcePersistentId> nextBatchOfPids = (Set)allIds.stream().map((t) -> {
				return this.myIdHelperService.newPidFromStringIdAndResourceName(t.getPartitionId(), t.getPid(), resourceType);
			}).collect(Collectors.toSet());
			PersistentIdToForcedIdMap nextBatchOfResourceIds = (PersistentIdToForcedIdMap)this.myTransactionService.withRequest((RequestDetails)null).execute(() -> {
				return this.myIdHelperService.translatePidsToForcedIds(nextBatchOfPids);
			});
			TokenOrListParam idListParam = new TokenOrListParam();
			Iterator var12 = nextBatchOfPids.iterator();

			while(var12.hasNext()) {
				IResourcePersistentId<?> nextPid = (IResourcePersistentId)var12.next();
				Optional<String> resourceId = nextBatchOfResourceIds.get(nextPid);
				idListParam.add((String)resourceId.orElse(nextPid.getId().toString()));
			}

			SearchParameterMap spMap = SearchParameterMap.newSynchronous().add("_id", idListParam);
			IBundleProvider outcome = dao.search(spMap, (new SystemRequestDetails()).setRequestPartitionId(theRequestPartitionId));
			resources.addAll(outcome.getAllResources());
		}

		return resources;
	}

	private ListMultimap<String, String> encodeToString(List<IBaseResource> theResources, BulkExportJobParameters theParameters) {
		IParser parser = this.getParser(theParameters);
		ListMultimap<String, String> retVal = ArrayListMultimap.create();
		Iterator var5 = theResources.iterator();

		while(var5.hasNext()) {
			IBaseResource resource = (IBaseResource)var5.next();
			String type = this.myFhirContext.getResourceType(resource);
			String jsonResource = parser.encodeResourceToString(resource);
			retVal.put(type, jsonResource);
		}

		return retVal;
	}

	private IParser getParser(BulkExportJobParameters theParameters) {
		return this.myFhirContext.newJsonParser().setPrettyPrint(false);
	}

	@VisibleForTesting
	public void setIdHelperServiceForUnitTest(IIdHelperService theIdHelperService) {
		this.myIdHelperService = theIdHelperService;
	}
}
