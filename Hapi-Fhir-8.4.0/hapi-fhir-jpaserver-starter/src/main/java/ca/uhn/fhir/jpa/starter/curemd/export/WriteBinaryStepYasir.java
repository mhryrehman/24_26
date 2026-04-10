//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.batch2.api.*;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.starter.curemd.export.models.BulkExportBinaryFileIdYasir;
import ca.uhn.fhir.jpa.starter.curemd.export.models.ExpandedResourcesListYasir;
import ca.uhn.fhir.jpa.util.RandomTextUtils;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BinaryUtil;
import ca.uhn.fhir.util.FhirTerser;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBinary;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;

public class WriteBinaryStepYasir implements IJobStepWorker<BulkExportJobParameters, ExpandedResourcesListYasir, BulkExportBinaryFileIdYasir> {
	private static final Logger ourLog = LoggerFactory.getLogger(WriteBinaryStepYasir.class);
	@Autowired
	private FhirContext myFhirContext;
	@Autowired
	private DaoRegistry myDaoRegistry;

	public WriteBinaryStepYasir() {
	}

	@Nonnull
	public RunOutcome run(@Nonnull StepExecutionDetails<BulkExportJobParameters, ExpandedResourcesListYasir> theStepExecutionDetails, @Nonnull IJobDataSink<BulkExportBinaryFileIdYasir> theDataSink) throws JobExecutionFailedException {
		ExpandedResourcesListYasir expandedResources = (ExpandedResourcesListYasir)theStepExecutionDetails.getData();
		int numResourcesProcessed = expandedResources.getStringifiedResources().size();
		ourLog.info("Write binary step of Job Export");
		ourLog.info("Writing {} resources to binary file", numResourcesProcessed);
		IFhirResourceDao<IBaseBinary> binaryDao = this.myDaoRegistry.getResourceDao("Binary");
		IBaseBinary binary = BinaryUtil.newBinary(this.myFhirContext);
		this.addMetadataExtensionsToBinary(theStepExecutionDetails, expandedResources, binary);
		binary.setContentType("application/fhir+ndjson");
		int processedRecordsCount = 0;

		String proposedId;
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			try {
				OutputStreamWriter streamWriter = this.getStreamWriter(outputStream);

				try {
					Iterator var10 = expandedResources.getStringifiedResources().iterator();

					while(true) {
						if (!var10.hasNext()) {
							streamWriter.flush();
							outputStream.flush();
							break;
						}

						proposedId = (String)var10.next();
						streamWriter.append(proposedId);
						streamWriter.append("\n");
						++processedRecordsCount;
					}
				} catch (Throwable var17) {
					if (streamWriter != null) {
						try {
							streamWriter.close();
						} catch (Throwable var15) {
							var17.addSuppressed(var15);
						}
					}

					throw var17;
				}

				if (streamWriter != null) {
					streamWriter.close();
				}

				binary.setContent(outputStream.toByteArray());
			} catch (Throwable var18) {
				try {
					outputStream.close();
				} catch (Throwable var14) {
					var18.addSuppressed(var14);
				}

				throw var18;
			}

			outputStream.close();
		} catch (IOException var19) {
			String errorMsg = String.format("Failure to process resource of type %s : %s", expandedResources.getResourceType(), var19.getMessage());
			ourLog.error(errorMsg);
			String var10002 = Msg.code(2238);
			throw new JobExecutionFailedException(var10002 + errorMsg);
		}

		SystemRequestDetails srd = new SystemRequestDetails();
		BulkExportJobParameters jobParameters = (BulkExportJobParameters)theStepExecutionDetails.getParameters();
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
			} catch (ResourceNotFoundException var16) {
			}
			break;
		}

		if (this.myFhirContext.getVersion().getVersion().isNewerThan(FhirVersionEnum.DSTU2) && (StringUtils.isNotBlank(jobParameters.getBinarySecurityContextIdentifierSystem()) || StringUtils.isNotBlank(jobParameters.getBinarySecurityContextIdentifierValue()))) {
			FhirTerser terser = this.myFhirContext.newTerser();
			terser.setElement(binary, "securityContext.identifier.system", jobParameters.getBinarySecurityContextIdentifierSystem());
			terser.setElement(binary, "securityContext.identifier.value", jobParameters.getBinarySecurityContextIdentifierValue());
		}

		DaoMethodOutcome outcome = binaryDao.update(binary, srd);
		IIdType id = outcome.getId();
		BulkExportBinaryFileIdYasir bulkExportBinaryFileId = new BulkExportBinaryFileIdYasir();
		bulkExportBinaryFileId.setBinaryId(id.getValueAsString());
		bulkExportBinaryFileId.setResourceType(expandedResources.getResourceType());
		theDataSink.accept(bulkExportBinaryFileId);
		ourLog.info("Binary writing complete for {} resources of type {}.", processedRecordsCount, expandedResources.getResourceType());
		return new RunOutcome(numResourcesProcessed);
	}

	private void addMetadataExtensionsToBinary(@Nonnull StepExecutionDetails<BulkExportJobParameters, ExpandedResourcesListYasir> theStepExecutionDetails, ExpandedResourcesListYasir expandedResources, IBaseBinary binary) {
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
}
