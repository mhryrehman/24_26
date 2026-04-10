package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

;

@Component
public class ExternalDocumentFetcher {

	private static final Logger logger = LoggerFactory.getLogger(ExternalDocumentFetcher.class);

	private final RestTemplate restTemplate;
	private String authUrl;

	private String clientId;

	private String clientSecret;

	private String grantType;

	private String username;

	private String password;

	private String oldTenantId = null;
	private AuthResponse authResponse = null;

	public ExternalDocumentFetcher() {
		this.restTemplate = new RestTemplate();
	}

	public void embedAttachmentsForDocumentReferenceResource(String tenantId, IBaseResource theResource) {
		DocumentReference documentReferenceResource = (DocumentReference) theResource;

		logger.info("Processing Tenant Id {}", tenantId);

		if (documentReferenceResource.getContent() == null || documentReferenceResource.getContent().isEmpty()) {
			logger.info("No content available in the DocumentReference to process.");
			return;
		}

		boolean isAuthenticated = isValidAuthResponse(tenantId);

		HttpHeaders docRefHeaders = new HttpHeaders();
		if(isAuthenticated) {
			docRefHeaders.setBearerAuth(authResponse.getAccessToken());
			oldTenantId = tenantId;
		}
		for (DocumentReference.DocumentReferenceContentComponent content : documentReferenceResource.getContent()) {
			if (content.getAttachment() != null && content.getAttachment().getUrl() != null && !content.getAttachment().getUrl().isEmpty()) {
				String referenceUrl = content.getAttachment().getUrl();
				logger.info("Found URL: {}", referenceUrl);
				Base64BinaryType binaryData = isAuthenticated ? getBinaryDocument(authResponse.getPracticeUrl(), referenceUrl, docRefHeaders) : null;
				if (Objects.nonNull(binaryData)) {
					logger.info("Resource Fetch Successful");
					content.getAttachment().setData(binaryData.getValue());
					content.getAttachment().setUrl(null);

				} else {

					logger.error("Failed to fetch document for URL: {}", referenceUrl);
					content.getAttachment().getDataElement().addExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason", new StringType("error"));
					content.getAttachment().setUrl(null);


				}
			}
		}
	}

	public void embedAttachmentsForPatient(String tenantId, IBaseResource theResource ) {

		Patient patientResource = (Patient) theResource;
		logger.info("Embedding attachments for Patient resource for tenantId: {}", tenantId);

		if (patientResource.getPhoto() == null || patientResource.getPhoto().isEmpty()) {
			logger.info("No photo available in the Patient to process.");
			return;
		}

		boolean isAuthenticated = isValidAuthResponse(tenantId);

		HttpHeaders docRefHeaders = new HttpHeaders();

		if(isAuthenticated) {
			docRefHeaders.setBearerAuth(authResponse.getAccessToken());
			oldTenantId = tenantId;
		}
		for (Attachment photo : patientResource.getPhoto()) {
			if (photo.getUrl() == null || photo.getUrl().isEmpty()) {
				continue;
			}

			logger.info("Found Photo URL: {}", photo.getUrl());
			Base64BinaryType binaryData = isAuthenticated ? getBinaryDocument(authResponse.getPracticeUrl(), photo.getUrl(), docRefHeaders) : null;

			if (Objects.nonNull(binaryData)) {
				logger.info("Photo Fetch Successful");
				photo.setData(binaryData.getValue());
				photo.setUrl(null);

			} else {

				logger.error("Failed to fetch photo for URL: {}", photo.getUrl());
				photo.getDataElement().addExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason", new StringType("error"));
				photo.setUrl(null);

			}
		}
	}

	public boolean testMethod(String tenandId,IBaseResource theResourse)
	{
        return tenandId != null && theResourse != null;
    }
	public void embedAttachmentsForDiagnosticReport(String tenantId, IBaseResource theResource ) {
		DiagnosticReport diagnosticReportResource = (DiagnosticReport) theResource;

		logger.info("Embedding attachments for DiagnosticReport resource for tenantId: {}", tenantId);

		if (diagnosticReportResource.getPresentedForm() == null || diagnosticReportResource.getPresentedForm().isEmpty()) {
			logger.info("No presentedForm attachments available in the DiagnosticReport to process.");
			return;
		}

		boolean isAuthenticated = isValidAuthResponse(tenantId);

		HttpHeaders docRefHeaders = new HttpHeaders();

		if(isAuthenticated) {
			docRefHeaders.setBearerAuth(authResponse.getAccessToken());
			oldTenantId = tenantId;
		}
		for (Attachment attachment : diagnosticReportResource.getPresentedForm()) {
			if (attachment.getUrl() == null || attachment.getUrl().isEmpty()) {
				continue;
			}

			logger.info("Found URL: {}", attachment.getUrl());

			Base64BinaryType binaryData = isAuthenticated ? getBinaryDocument(authResponse.getPracticeUrl(), attachment.getUrl(), docRefHeaders) : null;

			if (Objects.nonNull(binaryData)) {
				logger.info("Resource Fetch Successful");
				attachment.setData(binaryData.getValue());
				attachment.setUrl(null);

			} else {
				logger.error("Failed to fetch document for URL: {}", attachment.getUrl());
				attachment.getDataElement().addExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason", new StringType("error"));
				attachment.setUrl(null);

			}
		}
	}



	public void embedAttachmentsForCommunication(String tenantId, IBaseResource theResource ) {
		Communication communicationResource = (Communication) theResource;
		logger.info("Embedding attachments for Communication resource for tenantId: {}", tenantId);

		if (communicationResource.getPayload() == null || communicationResource.getPayload().isEmpty()) {
			logger.info("No payloads available in the Communication to process.");
			return;
		}
		boolean hasAttachments = communicationResource.getPayload().stream()
			.anyMatch(payload -> payload.hasContentAttachment() && payload.getContentAttachment().getUrl() != null);

		if (!hasAttachments) {
			logger.info("No attachments found in the payloads of the Communication.");
			return;
		}

		boolean isAuthenticated =  isValidAuthResponse(tenantId);
		HttpHeaders docRefHeaders = new HttpHeaders();
		if(isAuthenticated) {
			docRefHeaders.setBearerAuth(authResponse.getAccessToken());
			oldTenantId = tenantId;
		}
		for (Communication.CommunicationPayloadComponent payload : communicationResource.getPayload()) {
			if (payload.hasContentAttachment() && payload.getContentAttachment().getUrl() != null && !payload.getContentAttachment().getUrl().isEmpty()) {
				Attachment attachment = payload.getContentAttachment();
				String attachmentUrl = attachment.getUrl();

				logger.info("Found URL in payload contentAttachment: {}", attachmentUrl);
				Base64BinaryType binaryData = isAuthenticated ? getBinaryDocument(authResponse.getPracticeUrl(), attachmentUrl, docRefHeaders) : null;
				if (Objects.nonNull(binaryData)) {
					logger.info("Resource Fetch Successful");
					attachment.setData(binaryData.getValue());
					attachment.setUrl(null);
				} else {
					logger.error("Failed to fetch document for URL: {}", attachmentUrl);
					attachment.getDataElement().addExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason", new StringType("error"));
					attachment.setUrl(null);
				}
			}
		}
	}

	private boolean isValidAuthResponse(String tenantId) {
		if (tenantId.equals(oldTenantId) && authResponse != null) {
			return true;
		}
		return initializeAuthBodyForTenant(tenantId);
	}

	private boolean initializeAuthBodyForTenant(String tenantId) {

		if (authUrl == null || clientId == null || clientSecret == null || grantType == null || password == null || username == null) {
			logger.error("Authentication configuration is missing. Please ensure all variables are set.");
			return false;
		}

		HttpHeaders authHeaders = new HttpHeaders();
		authHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> authBody = new LinkedMultiValueMap<>();
		authBody.add("client_id", clientId);
		authBody.add("client_secret", clientSecret);
		authBody.add("grant_type", grantType);
		authBody.add("password", password);
		authBody.add("username", username);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(authBody, authHeaders);
		try {
			ResponseEntity<AuthResponse> response = restTemplate.exchange(authUrl, HttpMethod.POST, request, AuthResponse.class);
			if (response.getStatusCodeValue() == 200 && response.getBody() != null && (response.getBody().getAccessToken() != null || response.getBody().getPracticeUrl() != null)) {
				logger.info("Token generation successful for tenant: {}", tenantId);
				authResponse = response.getBody();
				return true;
			} else {
				logger.error("Failed to generate token for tenant: {}, Status Code: {}", tenantId, response.getStatusCodeValue() == 200);
				return false;
			}
		} catch (Exception e) {
			logger.error("Exception during token generation for tenant: {}. Exception: {}", tenantId, e.getMessage());
			return false;
		}
	}

	private Base64BinaryType getBinaryDocument(String practiceUrl, String url, HttpHeaders headers) {
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		try {
			logger.info("Practice : {}", practiceUrl);
			ResponseEntity<Base64BinaryType> response = restTemplate.exchange(practiceUrl + "/document/" + url, HttpMethod.GET, requestEntity, Base64BinaryType.class);
			if (response.getStatusCodeValue() == 200) {
				return response.getBody();
			} else {
				logger.error("Failed to fetch document at URL: {}, Status Code: {}", url, response.getStatusCodeValue() == 200);
			}
		} catch (Exception e) {
			logger.error("Exception while fetching document at URL: {}. Exception: {}", url, e.getMessage());
		}
		return null;
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

	public String getAuthUrl() {
		return authUrl;
	}

	public void setAuthUrl(String authUrl) {
		this.authUrl = authUrl;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getGrantType() {
		return grantType;
	}

	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getOldTenantId() {
		return oldTenantId;
	}

	public void setOldTenantId(String oldTenantId) {
		this.oldTenantId = oldTenantId;
	}

	public AuthResponse getAuthResponse() {
		return authResponse;
	}

	public void setAuthResponse(AuthResponse authResponse) {
		this.authResponse = authResponse;
	}

}
