package ca.uhn.fhir.jpa.starter.curemd.provider;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class ExportCancelOperationProvider {
	@Autowired
	private final JdbcTemplate jdbcTemplate;

	public ExportCancelOperationProvider(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Operation(name = "$export-cancel", idempotent = true, manualResponse = false)
	@Transactional
	public Parameters exportCancel(
		@OperationParam(name = "_jobId", min = 1, max = 1) String jobId,
		RequestDetails requestDetails) {

		if (jobId == null || jobId.trim().isEmpty()) {
			throw new InvalidRequestException("Parameter _jobId is required.");
		}

		String deleteChunks = "DELETE FROM bt2_work_chunk WHERE instance_id = ?";
		int chunksDeleted = jdbcTemplate.update(deleteChunks, jobId);

		String deleteJob = "DELETE FROM bt2_job_instance WHERE id = ?";
		int jobDeleted = jdbcTemplate.update(deleteJob, jobId);

		if (jobDeleted == 0) {
			throw new InvalidRequestException("No job found with ID: " + jobId);
		}

		Parameters response = new Parameters();
		response.addParameter().setName("status").setValue(new org.hl7.fhir.r4.model.StringType(
			"Successfully cancelled job ID " + jobId + " (Deleted " + chunksDeleted + " chunks)"
		));
		return response;
	}

}
