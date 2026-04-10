package ca.uhn.fhir.jpa.starter.curemd.sched;

import ca.uhn.fhir.jpa.bulk.export.api.IBulkDataExportJobSchedulingHelper;
import ca.uhn.fhir.jpa.model.sched.IHasScheduledJobs;
import ca.uhn.fhir.jpa.model.sched.ISchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpBulkDataExportJobSchedulingHelper implements IBulkDataExportJobSchedulingHelper, IHasScheduledJobs {

	private static final Logger logger = LoggerFactory.getLogger(NoOpBulkDataExportJobSchedulingHelper.class);

	@Override
	public void purgeExpiredFiles() {
		logger.info("Skipping purgeExpiredFiles()");
		// Do nothing — disable purging
	}

	@Override
	public void cancelAndPurgeAllJobs() {
		logger.info("cancelAndPurgeAllJobs is disabled.");
	}

	@Override
	public void scheduleJobs(ISchedulerService iSchedulerService) {
		logger.info("Purge task is disabled.");

	}
}
