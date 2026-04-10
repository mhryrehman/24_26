//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.batch2.api.*;
import ca.uhn.fhir.batch2.model.ChunkOutcome;
import ca.uhn.fhir.batch2.model.JobInstance;
import ca.uhn.fhir.jpa.api.model.BulkExportJobResults;
import ca.uhn.fhir.jpa.starter.curemd.export.models.BulkExportBinaryFileIdYasir;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BulkExportCreateReportStepYasir implements IReductionStepWorker<BulkExportJobParameters, BulkExportBinaryFileIdYasir, BulkExportJobResults> {
	private static final Logger ourLog = LoggerFactory.getLogger(BulkExportCreateReportStepYasir.class);
	private Map<String, List<String>> myResourceToBinaryIds;

	public BulkExportCreateReportStepYasir() {
	}

	@Nonnull
	public ChunkOutcome consume(ChunkExecutionDetails<BulkExportJobParameters, BulkExportBinaryFileIdYasir> theChunkDetails) {
		BulkExportBinaryFileIdYasir fileId = (BulkExportBinaryFileIdYasir)theChunkDetails.getData();
		if (this.myResourceToBinaryIds == null) {
			this.myResourceToBinaryIds = new HashMap();
		}

		this.myResourceToBinaryIds.putIfAbsent(fileId.getResourceType(), new ArrayList());
		((List)this.myResourceToBinaryIds.get(fileId.getResourceType())).add(fileId.getBinaryId());
		return ChunkOutcome.SUCCESS();
	}

	@Nonnull
	public RunOutcome run(@Nonnull StepExecutionDetails<BulkExportJobParameters, BulkExportBinaryFileIdYasir> theStepExecutionDetails, @Nonnull IJobDataSink<BulkExportJobResults> theDataSink) throws JobExecutionFailedException {
		BulkExportJobResults results = new BulkExportJobResults();
		String requestUrl = getOriginatingRequestUrl(theStepExecutionDetails, results);
		results.setOriginalRequestUrl(requestUrl);
		if (this.myResourceToBinaryIds != null) {
			ourLog.info("Bulk Export Report creation step for instance: {}", theStepExecutionDetails.getInstance().getInstanceId());
			results.setResourceTypeToBinaryIds(this.myResourceToBinaryIds);
			this.myResourceToBinaryIds = null;
		} else {
			String msg = "Export complete, but no data to generate report for job instance: " + theStepExecutionDetails.getInstance().getInstanceId();
			ourLog.warn(msg);
			results.setReportMsg(msg);
		}

		theDataSink.accept(results);
		return RunOutcome.SUCCESS;
	}

	private static String getOriginatingRequestUrl(@Nonnull StepExecutionDetails<BulkExportJobParameters, BulkExportBinaryFileIdYasir> theStepExecutionDetails, BulkExportJobResults results) {
		IJobInstance instance = theStepExecutionDetails.getInstance();
		String url = "";
		if (instance instanceof JobInstance) {
			JobInstance jobInstance = (JobInstance)instance;
			BulkExportJobParameters parameters = (BulkExportJobParameters)jobInstance.getParameters(BulkExportJobParameters.class);
			String originalRequestUrl = parameters.getOriginalRequestUrl();
			url = originalRequestUrl;
		}

		return url;
	}
}
