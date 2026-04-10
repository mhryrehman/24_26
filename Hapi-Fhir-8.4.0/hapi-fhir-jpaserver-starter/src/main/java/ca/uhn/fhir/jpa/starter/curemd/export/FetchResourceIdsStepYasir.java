//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.batch2.api.*;
import ca.uhn.fhir.batch2.jobs.chunk.TypedPidJson;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.bulk.export.api.IBulkExportProcessor;
import ca.uhn.fhir.jpa.bulk.export.model.ExportPIDIteratorParameters;
import ca.uhn.fhir.jpa.model.dao.JpaPid;
import ca.uhn.fhir.jpa.starter.curemd.export.models.ResourceIdListYasir;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import ca.uhn.fhir.rest.api.server.storage.IResourcePersistentId;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class FetchResourceIdsStepYasir implements IFirstJobStepWorker<BulkExportJobParameters, ResourceIdListYasir> {
	private static final Logger ourLog = LoggerFactory.getLogger(FetchResourceIdsStepYasir.class);
	@Autowired
	private IBulkExportProcessor myBulkExportProcessor;
	@Autowired
	private JpaStorageSettings myStorageSettings;

	public FetchResourceIdsStepYasir() {
	}

	@Nonnull
	public RunOutcome run(@Nonnull StepExecutionDetails<BulkExportJobParameters, VoidModel> theStepExecutionDetails, @Nonnull IJobDataSink<ResourceIdListYasir> theDataSink) throws JobExecutionFailedException {
		BulkExportJobParameters params = (BulkExportJobParameters)theStepExecutionDetails.getParameters();
		ourLog.info("Fetching resource IDs for bulk export job instance[{}]", theStepExecutionDetails.getInstance().getInstanceId());
		ExportPIDIteratorParameters providerParams = new ExportPIDIteratorParameters();
		providerParams.setInstanceId(theStepExecutionDetails.getInstance().getInstanceId());
		providerParams.setChunkId(theStepExecutionDetails.getChunkId());
		providerParams.setFilters(params.getFilters());
		providerParams.setStartDate(params.getSince());
		providerParams.setEndDate(params.getUntil());
		providerParams.setExportStyle(params.getExportStyle());
		providerParams.setGroupId(params.getGroupId());
		providerParams.setPatientIds(params.getPatientIds());
		providerParams.setExpandMdm(params.isExpandMdm());
		providerParams.setPartitionId(params.getPartitionId());
		providerParams.setRequestedResourceTypes(params.getResourceTypes());
		int submissionCount = 0;

		try {
			Set<TypedPidJson> submittedBatchResourceIds = new HashSet();
			Iterator var7 = params.getResourceTypes().iterator();

			while(var7.hasNext()) {
				String resourceType = (String)var7.next();
				providerParams.setResourceType(resourceType);
				ourLog.info("Running FetchResourceIdsStep for resource type: {} with params: {}", resourceType, providerParams);
				Iterator<IResourcePersistentId> pidIterator = this.myBulkExportProcessor.getResourcePidIterator(providerParams);
				LinkedHashSet<IResourcePersistentId> pidList = new LinkedHashSet<>();
				JpaPid pid1 = new JpaPid();
				pid1.setId(99502L);
				JpaPid pid2 = new JpaPid();
				pid2.setId(99503L);
				JpaPid pid3 = new JpaPid();
				pid3.setId(99504L);
				pidList.add(pid1);
				pidList.add(pid2);
				pidList.add(pid3);

//				Iterator<IResourcePersistentId> pidIterator = pidList.iterator();

				List<TypedPidJson> idsToSubmit = new ArrayList();
				int estimatedChunkSize = 0;
				if (!pidIterator.hasNext()) {
					ourLog.debug("Bulk Export generated an iterator with no results!");
				}

				while(pidIterator.hasNext()) {
					IResourcePersistentId<?> pid = (IResourcePersistentId)pidIterator.next();
					TypedPidJson batchResourceId;
					if (pid.getResourceType() != null) {
						batchResourceId = new TypedPidJson(pid.getResourceType(), pid);
					} else {
						batchResourceId = new TypedPidJson(resourceType, pid);
					}

					if (submittedBatchResourceIds.add(batchResourceId)) {
						idsToSubmit.add(batchResourceId);
						if (estimatedChunkSize > 0) {
							++estimatedChunkSize;
						}

						estimatedChunkSize += batchResourceId.estimateSerializedSize();
						if (idsToSubmit.size() >= this.myStorageSettings.getBulkExportFileMaximumCapacity() || (long)estimatedChunkSize >= this.myStorageSettings.getBulkExportFileMaximumSize()) {
							this.submitWorkChunk(idsToSubmit, resourceType, theDataSink);
							++submissionCount;
							idsToSubmit = new ArrayList();
							estimatedChunkSize = 0;
						}
					}
				}

				if (!idsToSubmit.isEmpty()) {
					this.submitWorkChunk(idsToSubmit, resourceType, theDataSink);
					++submissionCount;
				}
			}
		} catch (Exception var14) {
			ourLog.error(var14.getMessage(), var14);
			theDataSink.recoveredError(var14.getMessage());
			String var10002 = Msg.code(2239);
			throw new JobExecutionFailedException(var10002 + " : " + var14.getMessage());
		}

		ourLog.info("Submitted {} groups of ids for processing", submissionCount);
		return RunOutcome.SUCCESS;
	}

	private void submitWorkChunk(List<TypedPidJson> theBatchResourceIds, String theResourceType, IJobDataSink<ResourceIdListYasir> theDataSink) {
		ResourceIdListYasir idList = new ResourceIdListYasir();
		idList.setIds(theBatchResourceIds);
		idList.setResourceType(theResourceType);
		theDataSink.accept(idList);
	}

	@VisibleForTesting
	public void setBulkExportProcessorForUnitTest(IBulkExportProcessor theBulkExportProcessor) {
		this.myBulkExportProcessor = theBulkExportProcessor;
	}
}
