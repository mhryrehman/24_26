//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export.config;

import ca.uhn.fhir.batch2.api.VoidModel;
import ca.uhn.fhir.batch2.model.JobDefinition;
import ca.uhn.fhir.jpa.api.model.BulkExportJobResults;
import ca.uhn.fhir.jpa.starter.curemd.export.*;
import ca.uhn.fhir.jpa.starter.curemd.export.models.BulkExportBinaryFileIdYasir;
import ca.uhn.fhir.jpa.starter.curemd.export.models.ExpandedResourcesListYasir;
import ca.uhn.fhir.jpa.starter.curemd.export.models.ResourceIdListYasir;
import ca.uhn.fhir.model.api.IModelJson;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class BulkExportAppCtxYasir {
	public static final String WRITE_TO_BINARIES = "write-to-binaries";
	public static final String CREATE_REPORT_STEP = "create-report-step";

	public BulkExportAppCtxYasir() {
	}

	@Bean
	@Primary
	public JobDefinition bulkExportJobDefinitionYasir() {
		JobDefinition.Builder<IModelJson, VoidModel> builder = JobDefinition.newBuilder();
		builder.setJobDefinitionId("BULK_EXPORT_YASIR");
		builder.setJobDescription("FHIR Bulk Export Yasir");
		builder.setJobDefinitionVersion(1);
		JobDefinition def = builder.setParametersType(BulkExportJobParameters.class).setParametersValidator(this.bulkExportJobParametersValidatorYasir()).gatedExecution().addFirstStep("fetch-resources", "Fetches resource PIDs for exporting", ResourceIdListYasir.class, this.fetchResourceIdsStepYasir()).addIntermediateStep("expand-resources", "Expand out resources", ExpandedResourcesListYasir.class, this.expandResourcesStepYasir()).addIntermediateStep("write-to-binaries", "Writes the expanded resources to the binaries and saves", BulkExportBinaryFileIdYasir.class, this.writeBinaryStepYasir()).addFinalReducerStep("create-report-step", "Creates the output report from a bulk export job", BulkExportJobResults.class, this.createReportStepYasir()).build();
		return def;
	}

	@Bean
	public JobDefinition bulkExportJobV2DefinitionYasir() {
		JobDefinition.Builder<IModelJson, VoidModel> builder = JobDefinition.newBuilder();
		builder.setJobDefinitionId("BULK_EXPORT_YASIR_V2");
		builder.setJobDescription("FHIR Bulk Export");
		builder.setJobDefinitionVersion(2);
		JobDefinition def = builder.setParametersType(BulkExportJobParameters.class).setParametersValidator(this.bulkExportJobParametersValidatorYasir()).gatedExecution().addFirstStep("fetch-resources", "Fetches resource PIDs for exporting", ResourceIdListYasir.class, this.fetchResourceIdsStepYasir()).addIntermediateStep("write-to-binaries", "Writes the expanded resources to the binaries and saves", BulkExportBinaryFileIdYasir.class, this.expandResourceAndWriteBinaryStepYasir()).addFinalReducerStep("create-report-step", "Creates the output report from a bulk export job", BulkExportJobResults.class, this.createReportStepYasir()).build();
		return def;
	}

	@Bean
	public BulkExportJobParametersValidatorYasir bulkExportJobParametersValidatorYasir() {
		return new BulkExportJobParametersValidatorYasir();
	}

	@Bean
	public FetchResourceIdsStepYasir fetchResourceIdsStepYasir() {
    return new FetchResourceIdsStepYasir();
	}

	@Bean
	public ExpandResourcesStepYasir expandResourcesStepYasir() {
		return new ExpandResourcesStepYasir();
	}

	@Bean
	public WriteBinaryStepYasir writeBinaryStepYasir() {
		return new WriteBinaryStepYasir();
	}

	@Bean
	public ExpandResourceAndWriteBinaryStepYasir expandResourceAndWriteBinaryStepYasir() {
		return new ExpandResourceAndWriteBinaryStepYasir();
	}

	@Bean
	public BulkExportCreateReportStepYasir createReportStepYasir() {
		return new BulkExportCreateReportStepYasir();
	}
}
