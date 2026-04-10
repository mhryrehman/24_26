package ca.uhn.fhir.jpa.starter.curemd.sched;

import ca.uhn.fhir.batch2.api.IJobPersistence;
import ca.uhn.fhir.batch2.model.JobInstance;
import ca.uhn.fhir.batch2.model.StatusEnum;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.BulkExportJobResults;
import ca.uhn.fhir.jpa.bulk.export.api.IBulkDataExportJobSchedulingHelper;
import ca.uhn.fhir.jpa.bulk.export.svc.BulkExportHelperService;
import ca.uhn.fhir.jpa.model.sched.IHasScheduledJobs;
import ca.uhn.fhir.jpa.model.sched.ISchedulerService;
import ca.uhn.fhir.jpa.model.sched.ScheduledJobDefinition;
import ca.uhn.fhir.jpa.starter.curemd.KeycloakConstants;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.util.JsonUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBinary;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

class CustomBulkDataExportJobSchedulingHelper implements IBulkDataExportJobSchedulingHelper, IHasScheduledJobs {

	private static final Logger ourLog = LoggerFactory.getLogger(CustomBulkDataExportJobSchedulingHelper.class);
	private static final int DELETE_BATCH_SIZE = 200;
	private final ConcurrentHashMap<String, AtomicBoolean> jobLocks = new ConcurrentHashMap<>();
	private final DaoRegistry myDaoRegistry;
	private final PlatformTransactionManager myTxManager;
	private final JpaStorageSettings myDaoConfig;
	private final BulkExportHelperService myBulkExportHelperSvc;
	private final IJobPersistence myJpaJobPersistence;
	MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	MemoryUsage beforeHeap = memoryMXBean.getHeapMemoryUsage();
	long beforeUsed = beforeHeap.getUsed();
	private TransactionTemplate myTxTemplate;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	public CustomBulkDataExportJobSchedulingHelper(
		DaoRegistry theDaoRegistry,
		PlatformTransactionManager theTxManager,
		JpaStorageSettings theDaoConfig,
		BulkExportHelperService theBulkExportHelperSvc,
		IJobPersistence theJpaJobPersistence,
		TransactionTemplate theTxTemplate
	) {
		ourLog.info("Instantiated parameterized constructor CustomBulkDataExportJobSchedulingHelper [{}]", this.hashCode());
		this.myDaoRegistry = theDaoRegistry;
		this.myTxManager = theTxManager;
		this.myDaoConfig = theDaoConfig;
		this.myBulkExportHelperSvc = theBulkExportHelperSvc;
		this.myJpaJobPersistence = theJpaJobPersistence;
		this.myTxTemplate = theTxTemplate;
	}

	@Transactional(
		propagation = Propagation.NEVER
	)
	public synchronized void cancelAndPurgeAllJobs() {
	}

	@PostConstruct
	public void start() {
		this.myTxTemplate = new TransactionTemplate(this.myTxManager);
	}

	@Transactional(
		propagation = Propagation.NEVER
	)
	public void purgeExpiredFiles() {
		ourLog.info(">>> START purgeExpiredFiles() in CustomBulkDataExportJobSchedulingHelper called by [{}] at [{}]",
			Thread.currentThread().getName(), LocalDateTime.now());

		if (!this.myDaoConfig.isEnableTaskBulkExportJobExecution()) {
			ourLog.debug("bulk export disabled:  doing nothing");
		} else {
			List<JobInstance> jobInstancesToDelete = this.myTxTemplate.execute((t) -> {
				return this.myJpaJobPersistence.fetchInstances("BULK_EXPORT", StatusEnum.getEndedStatuses(), this.computeCutoffFromConfig(), PageRequest.of(0, 50));
			});
			if (jobInstancesToDelete != null && !jobInstancesToDelete.isEmpty()) {
				Iterator var2 = jobInstancesToDelete.iterator();

				while(var2.hasNext()) {
					JobInstance jobInstance = (JobInstance)var2.next();
					String jobId = jobInstance.getInstanceId();
					AtomicBoolean jobLock = jobLocks.computeIfAbsent(jobId, id -> new AtomicBoolean(false));

					if (!jobLock.compareAndSet(false, true)) {
						ourLog.warn("Job [{}] is already being purged. Skipping duplicate execution.", jobId);
						return;
					}

					try {
						ourLog.info("Heap used before purge: {} MB", beforeUsed / (1024 * 1024));
						ourLog.info("Deleting batch 2 bulk export job: {}", jobInstance);
						this.myTxTemplate.execute((t) -> {
							Optional<JobInstance> optJobInstanceForInstanceId = this.myJpaJobPersistence.fetchInstance(jobInstance.getInstanceId());
							if (optJobInstanceForInstanceId.isEmpty()) {
								ourLog.error("Can't find job instance for ID: {} despite having retrieved it in the first step", jobInstance.getInstanceId());
								return null;
							} else {
								JobInstance jobInstanceForInstanceId = optJobInstanceForInstanceId.get();
								ourLog.info("Deleting bulk export job: {}", jobInstanceForInstanceId);
								if (StatusEnum.FAILED == jobInstanceForInstanceId.getStatus()) {
									ourLog.info("skipping because the status is FAILED for ID: {}" + jobInstanceForInstanceId.getInstanceId());
									return null;
								} else {
									this.purgeBinariesIfNeededOld(jobInstanceForInstanceId, jobInstanceForInstanceId.getReport());
									String batch2BulkExportJobInstanceId = jobInstanceForInstanceId.getInstanceId();
									ourLog.debug("*** About to delete batch 2 bulk export job with ID {}", batch2BulkExportJobInstanceId);
									this.myJpaJobPersistence.deleteInstanceAndChunks(batch2BulkExportJobInstanceId);
									ourLog.info("Finished deleting bulk export job: {}", jobInstance.getInstanceId());
									return null;
								}
							}
						});
						MemoryUsage afterHeap = memoryMXBean.getHeapMemoryUsage();
						long afterUsed = afterHeap.getUsed();
						ourLog.info("Heap used after purge: {} MB", afterUsed / (1024 * 1024));
						ourLog.info("Heap increased by: {} MB", (afterUsed - beforeUsed) / (1024 * 1024));
						ourLog.info("Finished deleting bulk export jobs");
					} finally {
						jobLock.set(false);
						jobLocks.remove(jobId); // Optional: clean up to avoid memory bloat
					}


				}} else {
				ourLog.debug("No batch 2 bulk export jobs found!  Nothing to do!");
				ourLog.info("Finished bulk export job deletion with nothing to do");
			}
		}
	}


	private void purgeBinariesIfNeeded(JobInstance jobInstance, String reportJson) {
		if (reportJson == null || reportJson.isBlank()) {
			ourLog.warn("No report found for job {}", jobInstance.getInstanceId());
			return;
		}

		try {
			JsonFactory factory = new JsonFactory();
			try (JsonParser parser = factory.createParser(reportJson)) {

				while (!parser.isClosed()) {
					JsonToken token = parser.nextToken();

					if (token == JsonToken.FIELD_NAME && "resourceType2BinaryIds".equals(parser.getCurrentName())) {
						token = parser.nextToken(); // should be START_OBJECT

						if (token == JsonToken.START_OBJECT) {
							while (parser.nextToken() != JsonToken.END_OBJECT) {
								String resourceType = parser.getCurrentName();
								parser.nextToken(); // should be START_ARRAY

								while (parser.nextToken() != JsonToken.END_ARRAY) {
									String binaryRef = parser.getValueAsString();

									try {
										IIdType id = this.myBulkExportHelperSvc.toId(binaryRef);
										this.getBinaryDao().delete(id, new SystemRequestDetails());
										ourLog.info("Thread Name: {} and Purged binary: {}",Thread.currentThread().getName(), binaryRef);
									} catch (Exception e) {
										ourLog.warn("Failed to purge binary {}: {}", binaryRef, e.getMessage());
									}
								}
							}
						}
					}
				}

			}
		} catch (Exception ex) {
			ourLog.error("Failed to parse or purge binaries for job {}: {}", jobInstance.getInstanceId(), ex.getMessage(), ex);
		}
	}

	private void purgeBinariesIfNeededOld(JobInstance theJobInstanceForInstanceId, String theJobInstanceReportString) {
		Optional<BulkExportJobResults> optBulkExportJobResults = this.getBulkExportJobResults(theJobInstanceReportString);
		if (optBulkExportJobResults.isPresent()) {
			BulkExportJobResults bulkExportJobResults = optBulkExportJobResults.get();
			ourLog.debug("job: {} resource type to binary ID: {}", theJobInstanceForInstanceId.getInstanceId(), bulkExportJobResults.getResourceTypeToBinaryIds());
			Map<String, List<String>> resourceTypeToBinaryIds = bulkExportJobResults.getResourceTypeToBinaryIds();
			Iterator var6 = resourceTypeToBinaryIds.keySet().iterator();
			List<String> batch = null;
			while (var6.hasNext()) {
				String resourceType = (String) var6.next();
				List<String> binaryIds = resourceTypeToBinaryIds.get(resourceType);
				ourLog.info("Start purging of ResourceType: {} of size: {} with data : {}", resourceType, binaryIds.size(), binaryIds);
				for (int i = 0; i < binaryIds.size(); i += DELETE_BATCH_SIZE) {
					batch = binaryIds.subList(i, Math.min(i + DELETE_BATCH_SIZE, binaryIds.size()));
					ourLog.info("Purging batch {}–{} of ResourceType: {}", i + 1, i + batch.size(), resourceType);
					purgeBinariesManually(batch);
				}
			}
		}

	}

	private void purgeBinariesManually(List<String> inputBatch) {

		List<String> batch =
			inputBatch.stream()
				.map(
					ref -> {
						if (ref != null && ref.startsWith("Binary/")) {
							String idPart = ref.substring("Binary/".length());
							int slashIndex = idPart.indexOf("/_history");
							if (slashIndex != -1) {
								idPart = idPart.substring(0, slashIndex);
							}
							return idPart;
						}
						return ref;
					})
				.collect(Collectors.toList());
		ourLog.info("going to purge batch : " + batch);
		try {
			String deleteVer =
				"DELETE FROM HFJ_RES_VER WHERE RES_ID IN ("
					+ "SELECT RES_ID FROM HFJ_RESOURCE WHERE RES_TYPE = 'Binary' AND FHIR_ID IN (%s))"
					.formatted(generatePlaceholders(batch.size()));

			jdbcTemplate.update(deleteVer, batch.toArray());

			// Delete from HFJ_RESOURCE
			String deleteRes =
				"DELETE FROM HFJ_RESOURCE WHERE RES_TYPE = 'Binary' AND FHIR_ID IN (%s)"
					.formatted(generatePlaceholders(batch.size()));

			jdbcTemplate.update(deleteRes, batch.toArray());

			// Optional: delete from HFJ_FORCED_ID
			String deleteForced =
				"DELETE FROM HFJ_FORCED_ID WHERE RESOURCE_TYPE = 'Binary' AND FORCED_ID IN (%s)"
					.formatted(generatePlaceholders(batch.size()));

			jdbcTemplate.update(deleteForced, batch.toArray());

			ourLog.info("records updated successfully using purgeBinariesManually");

		} catch (Exception e) {
			ourLog.error("Batch failed : ", e.getMessage(), e);
		}
	}

	private String generatePlaceholders(int count) {
		return String.join(",", Collections.nCopies(count, "?"));
	}

	@Nonnull
	private Optional<BulkExportJobResults> getBulkExportJobResults(String theJobInstanceReportString) {
		if (StringUtils.isBlank(theJobInstanceReportString)) {
			ourLog.error(String.format("Cannot parse job report string because it's null or blank: %s", theJobInstanceReportString));
			return Optional.empty();
		} else {
			try {
				return Optional.of(JsonUtil.deserialize(theJobInstanceReportString, BulkExportJobResults.class));
			} catch (Exception var3) {
				ourLog.error(String.format("Cannot parse job report string: %s", theJobInstanceReportString), var3);
				return Optional.empty();
			}
		}
	}

	private IFhirResourceDao<IBaseBinary> getBinaryDao() {
		return myDaoRegistry.getResourceDao(Binary.class.getSimpleName());
	}

	@Nonnull
	private Date computeCutoffFromConfig() {
		int bulkExportFileRetentionPeriodHours = this.myDaoConfig.getBulkExportFileRetentionPeriodHours();
		LocalDateTime cutoffLocalDateTime = LocalDateTime.now().minusHours(KeycloakConstants.bulkExportFileRetentionPeriodHours);
		return Date.from(cutoffLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	@Override
	public void scheduleJobs(ISchedulerService theSchedulerService){
		ScheduledJobDefinition jobDefinition = new ScheduledJobDefinition();
		jobDefinition.setId(SafePurgeExpiredFilesJob.class.getName());
		jobDefinition.setJobClass(SafePurgeExpiredFilesJob.class);
		theSchedulerService.scheduleClusteredJob(3600000L, jobDefinition);
	}

	public static class SafePurgeExpiredFilesJob implements ca.uhn.fhir.jpa.model.sched.HapiJob {

		@Autowired
		private IBulkDataExportJobSchedulingHelper myTarget;
//		public SafePurgeExpiredFilesJob(IBulkDataExportJobSchedulingHelper myTarget) {
//			this.myTarget = myTarget;
//		}

		public SafePurgeExpiredFilesJob() {
		}

		@Override
		public void execute(org.quartz.JobExecutionContext context) {
			this.myTarget.purgeExpiredFiles();
		}
	}
}
