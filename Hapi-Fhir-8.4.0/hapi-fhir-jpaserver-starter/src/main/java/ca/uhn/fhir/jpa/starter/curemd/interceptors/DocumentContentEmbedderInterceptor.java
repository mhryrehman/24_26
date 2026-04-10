package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

;
/**
 * Author: Muhammad Ahtisham
 */
@Interceptor
public class DocumentContentEmbedderInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(DocumentContentEmbedderInterceptor.class);
	private Properties properties = new Properties();
	private boolean isConfigLoaded = false;

	@Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
	public void embedBinaryContent(IBaseResource theResource, RequestDetails theRequest, ResponseDetails theResponse) {
		try {
			String requestMethod = theRequest.getRequestType().toString();
			String tenantId = theRequest.getTenantId();

			if (theResource == null || theResource.fhirType() == null || !"GET".equals(requestMethod)) {
				return;
			}

			if (!isConfigLoaded) {
				loadPropertiesFromFile();
			}

			boolean isEnabled = Boolean.parseBoolean(properties.getProperty("embedBinaryContent.enabled", "false"));

			if (!isEnabled) {
				logger.info("DocumentContentEmbedderInterceptor is not enable");
				return;
			}
			logger.info("DocumentContentEmbedderInterceptor processing the outgoing response");

			if (theResource instanceof DocumentReference) {

				logger.info("Processing DocumentReference for tenantId: {}", tenantId);
				getConfiguredEDF(tenantId).embedAttachmentsForDocumentReferenceResource(tenantId, theResource);

			} else if (theResource instanceof Patient) {

				logger.info("Processing Patient for tenantId: {}", tenantId);
				getConfiguredEDF(tenantId).embedAttachmentsForPatient(tenantId, theResource);

			} else if (theResource instanceof DiagnosticReport) {

				logger.info("Processing DiagnosticReport for tenantId: {}", tenantId);
				getConfiguredEDF(tenantId).embedAttachmentsForDiagnosticReport(tenantId, theResource);

			} else if (theResource instanceof Communication) {

				logger.info("Processing Communication for tenantId: {}", tenantId);
				getConfiguredEDF(tenantId).embedAttachmentsForCommunication(tenantId, theResource);

			} else if ("Bundle".equals(theResource.fhirType())) {

				Bundle bundle = (Bundle) theResource;
				if (!bundle.getEntry().isEmpty()) {
					if (bundle.getEntry().get(0).getResource() instanceof DocumentReference) {
						logger.info("Processing Bundle containing DocumentReference(s) for tenantId: {}", tenantId);
						ExternalDocumentFetcher documentFetcher = getConfiguredEDF(tenantId);
						for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
							documentFetcher.embedAttachmentsForDocumentReferenceResource(tenantId, entry.getResource());
						}
					} else if (bundle.getEntry().get(0).getResource() instanceof Patient) {

						logger.info("Processing Bundle containing Patient(s) for tenantId: {}", tenantId);
						ExternalDocumentFetcher documentFetcher = getConfiguredEDF(tenantId);
						for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
							documentFetcher.embedAttachmentsForPatient(tenantId, entry.getResource());
						}

					} else if (bundle.getEntry().get(0).getResource() instanceof DiagnosticReport) {

						logger.info("Processing Bundle containing DiagnosticReport(s) for tenantId: {}", tenantId);
						ExternalDocumentFetcher documentFetcher = getConfiguredEDF(tenantId);
						for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
							documentFetcher.embedAttachmentsForDiagnosticReport(tenantId, entry.getResource());
						}

					} else if (bundle.getEntry().get(0).getResource() instanceof Communication) {

						logger.info("Processing Bundle containing Communication(s) for tenantId: {}", tenantId);
						ExternalDocumentFetcher documentFetcher = getConfiguredEDF(tenantId);
						for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
							documentFetcher.embedAttachmentsForCommunication(tenantId, entry.getResource());
						}

					}
				}

			}

		} catch (Exception e) {
			logger.error("Error while embedding binary content: {}", e.getMessage(), e);
		}
	}

	/**
	 * Hook method invoked during Bulk Export to conditionally embed binary content
	 * into selected FHIR resource types (e.g., DocumentReference, Patient, etc.).
	 * <p>
	 * This method is registered with the {@link Pointcut#STORAGE_BULK_EXPORT_RESOURCE_INCLUSION}
	 * pointcut and is triggered once for each resource being prepared for export.
	 * </p>
	 *
	 * @param parameters  The {@link BulkExportJobParameters} containing job context, including tenant/partition ID.
	 * @param theResource The individual {@link IBaseResource} to be included in the bulk export.
	 * @return {@code true} if the resource should be included in the export output (including after enrichment),
	 *         {@code false} if the resource should be skipped (not applicable in this implementation).
	 */
	@Hook(Pointcut.STORAGE_BULK_EXPORT_RESOURCE_INCLUSION)
	public boolean embedBinaryContentOnExport(BulkExportJobParameters parameters, IBaseResource theResource) {
		try {
			if (parameters == null || theResource == null) {
				logger.warn("Bulk export parameters/theResource are null.");
				return true;
			}

			if (parameters.getPartitionId() == null ||
				parameters.getPartitionId().getPartitionNames() == null ||
				parameters.getPartitionId().getPartitionNames().isEmpty()) {
				logger.warn("Missing tenant/partition information in bulk export parameters.");
				return true;
			}

			if (!isConfigLoaded) {
				loadPropertiesFromFile();
			}

			boolean isEnabled = Boolean.parseBoolean(properties.getProperty("embedBinaryContent.enabled", "false"));
			if (!isEnabled) {
				logger.info("DocumentContentEmbedderInterceptor is disabled via configuration.");
				return true;
			}

			String tenantId = parameters.getPartitionId().getPartitionNames().get(0);
			logger.debug("Embedding binary content for tenantId: {}, resourceType: {}", tenantId, theResource.fhirType());

			ExternalDocumentFetcher fetcher = getConfiguredEDF(tenantId);

			switch (theResource.fhirType()) {
				case "DocumentReference" -> fetcher.embedAttachmentsForDocumentReferenceResource(tenantId, theResource);
				case "Patient" -> fetcher.embedAttachmentsForPatient(tenantId, theResource);
				case "DiagnosticReport" -> fetcher.embedAttachmentsForDiagnosticReport(tenantId, theResource);
				case "Communication" -> fetcher.embedAttachmentsForCommunication(tenantId, theResource);
				default ->
					logger.debug("Resource type '{}' is not handled by this interceptor.", theResource.fhirType());
			}

		} catch (Exception e) {
			logger.error("Error while embedding binary content in bulk export: {}", e.getMessage(), e);
		}

		return true;
	}

	private void loadPropertiesFromFile() {
			try (InputStream input = getClass().getClassLoader().getResourceAsStream("document-interceptor.properties")) {
				if (input != null) {
					properties.load(input);
					logger.info("Properties file loaded successfully.");
					isConfigLoaded = true;
				} else {
					logger.warn("document-interceptor.properties not found in classpath.");
				}
			} catch (IOException e) {
				logger.error("Failed to load the properties file.", e);
			}
		}

	private ExternalDocumentFetcher getConfiguredEDF(String tenantId) {
		ExternalDocumentFetcher fetcher = new ExternalDocumentFetcher();
		if(properties!=null) {
			fetcher.setAuthUrl(properties.getProperty("auth.server.url",null));
			fetcher.setClientId(properties.getProperty("auth.client.id",null));
			fetcher.setClientSecret(properties.getProperty("auth.client.secret",null));
			fetcher.setGrantType(properties.getProperty("auth.grant.type",null));
			fetcher.setPassword(properties.getProperty("auth.password",null));
			fetcher.setUsername(tenantId + "FHIR");
		}
		return fetcher;
	}
}
