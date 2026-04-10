//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.batch2.api.*;
import ca.uhn.fhir.batch2.jobs.chunk.TypedPidJson;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.executor.InterceptorService;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.api.model.PersistentIdToForcedIdMap;
import ca.uhn.fhir.jpa.api.svc.IIdHelperService;
import ca.uhn.fhir.jpa.bulk.export.api.IBulkExportProcessor;
import ca.uhn.fhir.jpa.dao.tx.IHapiTransactionService;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.searchparam.extractor.ResourceIndexedSearchParams;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryMatchResult;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryResourceMatcher;
import ca.uhn.fhir.jpa.starter.curemd.export.models.BulkExportBinaryFileIdYasir;
import ca.uhn.fhir.jpa.starter.curemd.export.models.ExpandedResourcesListYasir;
import ca.uhn.fhir.jpa.starter.curemd.export.models.ResourceIdListYasir;
import ca.uhn.fhir.jpa.util.RandomTextUtils;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import ca.uhn.fhir.rest.api.server.storage.IResourcePersistentId;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.interceptor.ResponseTerminologyTranslationSvc;
import ca.uhn.fhir.util.BinaryUtil;
import ca.uhn.fhir.util.FhirTerser;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExpandResourceAndWriteBinaryStepYasir implements IJobStepWorker<BulkExportJobParameters, ResourceIdListYasir, BulkExportBinaryFileIdYasir> {
	private static final Logger ourLog = LoggerFactory.getLogger(ExpandResourceAndWriteBinaryStepYasir.class);
	@Autowired
	private FhirContext myFhirContext;
	@Autowired
	private DaoRegistry myDaoRegistry;
	@Autowired
	private InMemoryResourceMatcher myInMemoryResourceMatcher;
	@Autowired
	private IBulkExportProcessor<?> myBulkExportProcessor;
	@Autowired
	private JpaStorageSettings myStorageSettings;
	@Autowired
	private ApplicationContext myApplicationContext;
	@Autowired
	private InterceptorService myInterceptorService;
	@Autowired
	private IIdHelperService myIdHelperService;
	@Autowired
	private IHapiTransactionService myTransactionService;
	private volatile ResponseTerminologyTranslationSvc myResponseTerminologyTranslationSvc;

	public ExpandResourceAndWriteBinaryStepYasir() {
	}

	@Nonnull
	public RunOutcome run(@Nonnull StepExecutionDetails<BulkExportJobParameters, ResourceIdListYasir> theStepExecutionDetails, @Nonnull IJobDataSink<BulkExportBinaryFileIdYasir> theDataSink) throws JobExecutionFailedException {
		NdJsonResourceWriter resourceWriter = new NdJsonResourceWriter(theStepExecutionDetails, theDataSink);
		this.expandResourcesFromList(theStepExecutionDetails, resourceWriter);
		return new RunOutcome(resourceWriter.getNumResourcesProcessed());
	}

	private void expandResourcesFromList(StepExecutionDetails<BulkExportJobParameters, ResourceIdListYasir> theStepExecutionDetails, Consumer<ExpandedResourcesListYasir> theResourceWriter) {
		ResourceIdListYasir idList = (ResourceIdListYasir)theStepExecutionDetails.getData();
		BulkExportJobParameters parameters = (BulkExportJobParameters)theStepExecutionDetails.getParameters();
		Consumer<List<IBaseResource>> resourceListConsumer = new ExpandResourcesConsumer(theStepExecutionDetails, theResourceWriter);
		this.fetchResourcesByIdAndConsumeThem(idList, parameters.getPartitionId(), resourceListConsumer);
	}

	private void fetchResourcesByIdAndConsumeThem(ResourceIdListYasir theIds, RequestPartitionId theRequestPartitionId, Consumer<List<IBaseResource>> theResourceListConsumer) {
		ArrayListMultimap<String, TypedPidJson> typeToIds = ArrayListMultimap.create();
		theIds.getIds().forEach((t) -> {
			typeToIds.put(t.getResourceType(), t);
		});
		Iterator var5 = typeToIds.keySet().iterator();

		while(var5.hasNext()) {
			String resourceType = (String)var5.next();
			IFhirResourceDao<?> dao = this.myDaoRegistry.getResourceDao(resourceType);
			List<TypedPidJson> allIds = typeToIds.get(resourceType);

			while(!allIds.isEmpty()) {
				int batchSize = Math.min(500, allIds.size());
				Set<IResourcePersistentId> nextBatchOfPids = (Set)allIds.subList(0, batchSize).stream().map((t) -> {
					return this.myIdHelperService.newPidFromStringIdAndResourceName(t.getPartitionId(), t.getPid(), resourceType);
				}).collect(Collectors.toSet());
				allIds = allIds.subList(batchSize, allIds.size());
				PersistentIdToForcedIdMap nextBatchOfResourceIds = (PersistentIdToForcedIdMap)this.myTransactionService.withSystemRequestOnPartition(theRequestPartitionId).execute(() -> {
					return this.myIdHelperService.translatePidsToForcedIds(nextBatchOfPids);
				});
				TokenOrListParam idListParam = new TokenOrListParam();
				Iterator var13 = nextBatchOfPids.iterator();

				while(var13.hasNext()) {
					IResourcePersistentId nextPid = (IResourcePersistentId)var13.next();
					Optional<String> resourceId = nextBatchOfResourceIds.get(nextPid);
					idListParam.add((String)resourceId.orElse(nextPid.getId().toString()));
				}

				SearchParameterMap spMap = SearchParameterMap.newSynchronous().add("_id", idListParam);
				IBundleProvider outcome = dao.search(spMap, (new SystemRequestDetails()).setRequestPartitionId(theRequestPartitionId));
				theResourceListConsumer.accept(outcome.getAllResources());
			}
		}

	}

	private void addMetadataExtensionsToBinary(@Nonnull StepExecutionDetails<BulkExportJobParameters, ResourceIdListYasir> theStepExecutionDetails, ExpandedResourcesListYasir expandedResources, IBaseBinary binary) {
		if (binary.getMeta() instanceof IBaseHasExtensions) {
			IBaseHasExtensions meta = (IBaseHasExtensions)binary.getMeta();
			String exportIdentifier = ((BulkExportJobParameters)theStepExecutionDetails.getParameters()).getExportIdentifier();
			IBaseExtension jobExtension;
			if (!StringUtils.isBlank(exportIdentifier)) {
				jobExtension = meta.addExtension();
				jobExtension.setUrl("https://hapifhir.org/NamingSystem/bulk-export-identifier");
				jobExtension.setValue(this.myFhirContext.newPrimitiveString(exportIdentifier));
			}

			jobExtension = meta.addExtension();
			jobExtension.setUrl("https://hapifhir.org/NamingSystem/bulk-export-job-id");
			jobExtension.setValue(this.myFhirContext.newPrimitiveString(theStepExecutionDetails.getInstance().getInstanceId()));
			IBaseExtension<?, ?> typeExtension = meta.addExtension();
			typeExtension.setUrl("https://hapifhir.org/NamingSystem/bulk-export-binary-resource-type");
			typeExtension.setValue(this.myFhirContext.newPrimitiveString(expandedResources.getResourceType()));
		} else {
			ourLog.warn("Could not attach metadata extensions to binary resource, as this binary metadata does not support extensions");
		}

	}

	protected OutputStreamWriter getStreamWriter(ByteArrayOutputStream theOutputStream) {
		return new OutputStreamWriter(theOutputStream, Constants.CHARSET_UTF8);
	}

	@VisibleForTesting
	public void setIdHelperServiceForUnitTest(IIdHelperService theIdHelperService) {
		this.myIdHelperService = theIdHelperService;
	}

	private class NdJsonResourceWriter implements Consumer<ExpandedResourcesListYasir> {
		private final StepExecutionDetails<BulkExportJobParameters, ResourceIdListYasir> myStepExecutionDetails;
		private final IJobDataSink<BulkExportBinaryFileIdYasir> myDataSink;
		private int myNumResourcesProcessed = 0;

		public NdJsonResourceWriter(StepExecutionDetails<BulkExportJobParameters, ResourceIdListYasir> theStepExecutionDetails, IJobDataSink<BulkExportBinaryFileIdYasir> theDataSink) {
			this.myStepExecutionDetails = theStepExecutionDetails;
			this.myDataSink = theDataSink;
		}

		public int getNumResourcesProcessed() {
			return this.myNumResourcesProcessed;
		}

		public void accept(ExpandedResourcesListYasir theExpandedResourcesList) throws JobExecutionFailedException {
			int batchSize = theExpandedResourcesList.getStringifiedResources().size();
			ExpandResourceAndWriteBinaryStepYasir.ourLog.info("Writing {} resources to binary file", batchSize);
			this.myNumResourcesProcessed += batchSize;
			IFhirResourceDao<IBaseBinary> binaryDao = ExpandResourceAndWriteBinaryStepYasir.this.myDaoRegistry.getResourceDao("Binary");
			IBaseBinary binary = BinaryUtil.newBinary(ExpandResourceAndWriteBinaryStepYasir.this.myFhirContext);
			ExpandResourceAndWriteBinaryStepYasir.this.addMetadataExtensionsToBinary(this.myStepExecutionDetails, theExpandedResourcesList, binary);
			binary.setContentType("application/fhir+ndjson");
			int processedRecordsCount = 0;

			String proposedId;
			try {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

				try {
					OutputStreamWriter streamWriter = ExpandResourceAndWriteBinaryStepYasir.this.getStreamWriter(outputStream);

					try {
						Iterator var8 = theExpandedResourcesList.getStringifiedResources().iterator();

						while(true) {
							if (!var8.hasNext()) {
								streamWriter.flush();
								outputStream.flush();
								break;
							}

							proposedId = (String)var8.next();
							streamWriter.append(proposedId);
							streamWriter.append("\n");
							++processedRecordsCount;
						}
					} catch (Throwable var15) {
						if (streamWriter != null) {
							try {
								streamWriter.close();
							} catch (Throwable var13) {
								var15.addSuppressed(var13);
							}
						}

						throw var15;
					}

					if (streamWriter != null) {
						streamWriter.close();
					}

					binary.setContent(outputStream.toByteArray());
				} catch (Throwable var16) {
					try {
						outputStream.close();
					} catch (Throwable var12) {
						var16.addSuppressed(var12);
					}

					throw var16;
				}

				outputStream.close();
			} catch (IOException var17) {
				String errorMsg = String.format("Failure to process resource of type %s : %s", theExpandedResourcesList.getResourceType(), var17.getMessage());
				ExpandResourceAndWriteBinaryStepYasir.ourLog.error(errorMsg);
				String var10002 = Msg.code(2431);
				throw new JobExecutionFailedException(var10002 + errorMsg);
			}

			SystemRequestDetails srd = new SystemRequestDetails();
			BulkExportJobParameters jobParameters = (BulkExportJobParameters)this.myStepExecutionDetails.getParameters();
			RequestPartitionId partitionId = jobParameters.getPartitionId();
			if (partitionId == null) {
				srd.setRequestPartitionId(RequestPartitionId.defaultPartition());
			} else {
				srd.setRequestPartitionId(partitionId);
			}

			while(true) {
				proposedId = RandomTextUtils.newSecureRandomAlphaNumericString(32);
				binary.setId(proposedId);

				try {
					IBaseBinary output = (IBaseBinary)binaryDao.read(binary.getIdElement(), new SystemRequestDetails(), true);
					if (output != null) {
						continue;
					}
				} catch (ResourceNotFoundException var14) {
				}
				break;
			}

			if (ExpandResourceAndWriteBinaryStepYasir.this.myFhirContext.getVersion().getVersion().isNewerThan(FhirVersionEnum.DSTU2) && (StringUtils.isNotBlank(jobParameters.getBinarySecurityContextIdentifierSystem()) || StringUtils.isNotBlank(jobParameters.getBinarySecurityContextIdentifierValue()))) {
				FhirTerser terser = ExpandResourceAndWriteBinaryStepYasir.this.myFhirContext.newTerser();
				terser.setElement(binary, "securityContext.identifier.system", jobParameters.getBinarySecurityContextIdentifierSystem());
				terser.setElement(binary, "securityContext.identifier.value", jobParameters.getBinarySecurityContextIdentifierValue());
			}

			DaoMethodOutcome outcome = binaryDao.update(binary, srd);
			IIdType id = outcome.getId();
			BulkExportBinaryFileIdYasir bulkExportBinaryFileId = new BulkExportBinaryFileIdYasir();
			bulkExportBinaryFileId.setBinaryId(id.getValueAsString());
			bulkExportBinaryFileId.setResourceType(theExpandedResourcesList.getResourceType());
			this.myDataSink.accept(bulkExportBinaryFileId);
			ExpandResourceAndWriteBinaryStepYasir.ourLog.info("Binary writing complete for {} resources of type {}.", processedRecordsCount, theExpandedResourcesList.getResourceType());
		}
	}

	private class ExpandResourcesConsumer implements Consumer<List<IBaseResource>> {
		private final Consumer<ExpandedResourcesListYasir> myResourceWriter;
		private final StepExecutionDetails<BulkExportJobParameters, ResourceIdListYasir> myStepExecutionDetails;

		public ExpandResourcesConsumer(StepExecutionDetails<BulkExportJobParameters, ResourceIdListYasir> theStepExecutionDetails, Consumer<ExpandedResourcesListYasir> theResourceWriter) {
			this.myStepExecutionDetails = theStepExecutionDetails;
			this.myResourceWriter = theResourceWriter;
		}

		public void accept(List<IBaseResource> theResources) throws JobExecutionFailedException {
			String instanceId = this.myStepExecutionDetails.getInstance().getInstanceId();
			String chunkId = this.myStepExecutionDetails.getChunkId();
			ResourceIdListYasir idList = (ResourceIdListYasir)this.myStepExecutionDetails.getData();
			BulkExportJobParameters parameters = (BulkExportJobParameters)this.myStepExecutionDetails.getParameters();
			ExpandResourceAndWriteBinaryStepYasir.ourLog.info("Bulk export instance[{}] chunk[{}] - About to expand {} resource IDs into their full resource bodies.", new Object[]{instanceId, chunkId, idList.getIds().size()});
			String resourceType = idList.getResourceType();
			List<String> postFetchFilterUrls = (List)parameters.getPostFetchFilterUrls().stream().filter((t) -> {
				return t.substring(0, t.indexOf(63)).equals(resourceType);
			}).collect(Collectors.toList());
			if (!postFetchFilterUrls.isEmpty()) {
				this.applyPostFetchFiltering(theResources, postFetchFilterUrls, instanceId, chunkId);
			}

			if (parameters.isExpandMdm()) {
				ExpandResourceAndWriteBinaryStepYasir.this.myBulkExportProcessor.expandMdmResources(theResources);
			}

			if (ExpandResourceAndWriteBinaryStepYasir.this.myStorageSettings.isNormalizeTerminologyForBulkExportJobs()) {
				ResponseTerminologyTranslationSvc terminologyTranslationSvc = ExpandResourceAndWriteBinaryStepYasir.this.myResponseTerminologyTranslationSvc;
				if (terminologyTranslationSvc == null) {
					terminologyTranslationSvc = (ResponseTerminologyTranslationSvc) ExpandResourceAndWriteBinaryStepYasir.this.myApplicationContext.getBean(ResponseTerminologyTranslationSvc.class);
					ExpandResourceAndWriteBinaryStepYasir.this.myResponseTerminologyTranslationSvc = terminologyTranslationSvc;
				}

				terminologyTranslationSvc.processResourcesForTerminologyTranslation(theResources);
			}

			if (ExpandResourceAndWriteBinaryStepYasir.this.myInterceptorService.hasHooks(Pointcut.STORAGE_BULK_EXPORT_RESOURCE_INCLUSION)) {
				Iterator<IBaseResource> iter = theResources.iterator();

				while(iter.hasNext()) {
					HookParams params = (new HookParams()).add(BulkExportJobParameters.class, (BulkExportJobParameters)this.myStepExecutionDetails.getParameters()).add(IBaseResource.class, (IBaseResource)iter.next());
					boolean outcome = ExpandResourceAndWriteBinaryStepYasir.this.myInterceptorService.callHooks(Pointcut.STORAGE_BULK_EXPORT_RESOURCE_INCLUSION, params);
					if (!outcome) {
						iter.remove();
					}
				}
			}

			IParser parser = this.getParser(parameters);
			ListMultimap<String, String> resourceTypeToStringifiedResources = ArrayListMultimap.create();
			Map<String, Integer> resourceTypeToTotalSize = new HashMap();
			Iterator var11 = theResources.iterator();

			while(var11.hasNext()) {
				IBaseResource resource = (IBaseResource)var11.next();
				String type = ExpandResourceAndWriteBinaryStepYasir.this.myFhirContext.getResourceType(resource);
				int existingSize = (Integer)resourceTypeToTotalSize.getOrDefault(type, 0);
				String jsonResource = parser.encodeResourceToString(resource);
				int newSize = existingSize + jsonResource.length();
				long bulkExportFileMaximumSize = ExpandResourceAndWriteBinaryStepYasir.this.myStorageSettings.getBulkExportFileMaximumSize();
				if ((long)newSize > bulkExportFileMaximumSize) {
					if (existingSize == 0) {
						ExpandResourceAndWriteBinaryStepYasir.ourLog.warn("Single resource size {} exceeds allowable maximum of {}, so will ignore maximum", newSize, bulkExportFileMaximumSize);
					} else {
						List<String> stringifiedResourcesx = resourceTypeToStringifiedResources.get(type);
						this.writeStringifiedResources(type, stringifiedResourcesx);
						resourceTypeToStringifiedResources.removeAll(type);
						newSize = jsonResource.length();
					}
				}

				resourceTypeToStringifiedResources.put(type, jsonResource);
				resourceTypeToTotalSize.put(type, newSize);
			}

			var11 = resourceTypeToStringifiedResources.keySet().iterator();

			while(var11.hasNext()) {
				String nextResourceType = (String)var11.next();
				List<String> stringifiedResources = resourceTypeToStringifiedResources.get(nextResourceType);
				this.writeStringifiedResources(nextResourceType, stringifiedResources);
			}

		}

		private void writeStringifiedResources(String theResourceType, List<String> theStringifiedResources) {
			if (!theStringifiedResources.isEmpty()) {
				ExpandedResourcesListYasir output = new ExpandedResourcesListYasir();
				output.setStringifiedResources(theStringifiedResources);
				output.setResourceType(theResourceType);
				this.myResourceWriter.accept(output);
				ExpandResourceAndWriteBinaryStepYasir.ourLog.info("Expanding of {} resources of type {} completed", theStringifiedResources.size(), theResourceType);
			}

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
				ExpandResourceAndWriteBinaryStepYasir.ourLog.info("Bulk export instance[{}] chunk[{}] - {} resources were filtered out because of post-fetch filter URLs", new Object[]{theInstanceId, theChunkId, numRemoved});
			}

		}

		private boolean applyPostFetchFilteringForSingleResource(List<String> thePostFetchFilterUrls, Iterator<IBaseResource> iter) {
			IBaseResource nextResource = (IBaseResource)iter.next();
			String nextResourceType = ExpandResourceAndWriteBinaryStepYasir.this.myFhirContext.getResourceType(nextResource);
			Iterator var5 = thePostFetchFilterUrls.iterator();

			while(var5.hasNext()) {
				String nextPostFetchFilterUrl = (String)var5.next();
				if (nextPostFetchFilterUrl.contains("?")) {
					String resourceType = nextPostFetchFilterUrl.substring(0, nextPostFetchFilterUrl.indexOf(63));
					if (nextResourceType.equals(resourceType)) {
						InMemoryMatchResult matchResult = ExpandResourceAndWriteBinaryStepYasir.this.myInMemoryResourceMatcher.match(nextPostFetchFilterUrl, nextResource, (ResourceIndexedSearchParams)null, new SystemRequestDetails());
						if (matchResult.matched()) {
							return true;
						}
					}
				}
			}

			return false;
		}

		private IParser getParser(BulkExportJobParameters theParameters) {
			return ExpandResourceAndWriteBinaryStepYasir.this.myFhirContext.newJsonParser().setPrettyPrint(false);
		}
	}
}
