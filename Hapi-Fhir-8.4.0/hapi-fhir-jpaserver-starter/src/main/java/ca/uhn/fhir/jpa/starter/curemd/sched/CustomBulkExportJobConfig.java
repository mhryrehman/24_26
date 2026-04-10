package ca.uhn.fhir.jpa.starter.curemd.sched;

import ca.uhn.fhir.batch2.api.IJobPersistence;
import ca.uhn.fhir.batch2.coordinator.JobDefinitionRegistry;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.bulk.export.api.IBulkDataExportJobSchedulingHelper;
import ca.uhn.fhir.jpa.bulk.export.svc.BulkExportHelperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class CustomBulkExportJobConfig {

	private static final Logger ourLog = LoggerFactory.getLogger(CustomBulkExportJobConfig.class);

//	@Autowired
//	private final JpaStorageSettings storageSettings;
//
//	public DisableBulkExportCleanupJobConfig(JpaStorageSettings storageSettings) {
//		this.storageSettings = storageSettings;
//	}
//
//	@PostConstruct
//	public void customizeRetention() {
//		System.out.println("Retention before: " + storageSettings.getBulkExportFileRetentionPeriodHours());
//		storageSettings.setBulkExportFileRetentionPeriodHours(1);
//		System.out.println("Retention after: " + storageSettings.getBulkExportFileRetentionPeriodHours());
//	}


//	@Bean
//	@Primary
//	public IBulkDataExportJobSchedulingHelper bulkDataExportJobSchedulingHelperOverride() {
//		return new BulkDataExportJobSchedulingHelperImpl(null, null, null, null, null, null) {
//			@Override
//			public void scheduleJobs(ca.uhn.fhir.jpa.model.sched.ISchedulerService theSchedulerService) {
//				// Override to disable scheduling
//				logger.warn("Overridden Bulk Export Purge Job - scheduling DISABLED.");
//			}
//
//			@Override
//			public void purgeExpiredFiles() {
//				// Optional: prevent purge if called directly
//				logger.warn("Overridden purgeExpiredFiles() - not executing.");
//			}
//		};
//	}

	@Bean
	@Primary
	@ConditionalOnProperty(prefix = "hapi.fhir.bulk-export", name = "export-disabled", havingValue = "true")
	public JobDefinitionRegistry jobDefinitionRegistry() {
		return new DisabledJobDefinitionRegistry();
	}

	@Bean
	@Primary
	@ConditionalOnProperty(prefix = "hapi.fhir.bulk-export", name = "purge-disabled", havingValue = "true")
	public IBulkDataExportJobSchedulingHelper customBulkDataExportJobSchedulingHelper() {
		ourLog.info("returning NoOpBulkDataExportJobSchedulingHelper instance");
		return new NoOpBulkDataExportJobSchedulingHelper();
	}

	@Bean
	@Primary
	@ConditionalOnProperty(prefix = "hapi.fhir.bulk-export", name = "custom-purge-enabled", havingValue = "true")
	public IBulkDataExportJobSchedulingHelper customBulkExportHelper(
		DaoRegistry daoRegistry,
		PlatformTransactionManager txManager,
		JpaStorageSettings daoConfig,
		BulkExportHelperService helperService,
		IJobPersistence jobPersistence,
		TransactionTemplate txTemplate
	) {
		ourLog.info("returning CustomBulkDataExportJobSchedulingHelper instance");
		return new CustomBulkDataExportJobSchedulingHelper(
			daoRegistry,
			txManager,
			daoConfig,
			helperService,
			jobPersistence,
			txTemplate
		);
	}
}