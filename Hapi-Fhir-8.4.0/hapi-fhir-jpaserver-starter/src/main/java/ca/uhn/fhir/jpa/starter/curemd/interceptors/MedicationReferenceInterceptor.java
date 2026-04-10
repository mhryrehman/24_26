package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Author: Muhammad Ahtisham
 * Description:
 * This interceptor validates and updates the `MedicationReference` in `MedicationRequest` resources
 * during incoming requests. It ensures that references to `Medication` resources are correctly resolved
 * within the `DEFAULT` partition and updates the reference if necessary.
 */
@Component
@Interceptor
public class MedicationReferenceInterceptor {

	private static final Logger log = LoggerFactory.getLogger(MedicationReferenceInterceptor.class);
	private IGenericClient fhirClient;
	public MedicationReferenceInterceptor() {
	}
	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void handleMedicationRequestCreated(ServletRequestDetails servletRequestDetails, HttpServletRequest theRequest) {

		if(null == servletRequestDetails){
			return;
		}

		if(theRequest == null) {
			return;
		}

		RequestDetails requestDetails = (RequestDetails) theRequest.getAttribute(RequestDetails.class.getName());

		IBaseResource resource = requestDetails.getResource();


		IBaseResource theResource = servletRequestDetails.getResource();


		if (!(theResource instanceof MedicationRequest)) {
			return;
		}
		MedicationRequest medRequest = (MedicationRequest) theResource;

		MedicationRequest temp = (MedicationRequest) theResource;


		String requestType = servletRequestDetails.getRequestType().toString();
		if (!"PUT".equalsIgnoreCase(requestType) && !"POST".equalsIgnoreCase(requestType)) {
			return;
		}

		log.info("Resource is MedicationRequest. Validating MedicationReference #####.");

		if (!initializeFhirClient(servletRequestDetails)) {
			return;
		}
		validateMedicationReference(medRequest);
	}
	private void validateMedicationReference(MedicationRequest medRequest) {
		Reference medReference = medRequest.getMedicationReference();
		if (medReference != null && medReference.getReference() != null) {
			String reference = medReference.getReference();
			log.info("MedicationReference found: {}", reference);

			if (reference.startsWith("DEFAULT/Medication?identifier=")) {
				String identifier = reference.substring("DEFAULT/Medication?identifier=".length());
				log.info("Extracted identifier: {}", identifier);

				String medicationId = findMedicationId(identifier);
				if (medicationId == null) {
					log.info("Medication with identifier [{}] not found in DEFAULT partition.", identifier);
				} else {
					log.info("Medication with identifier [{}] found in DEFAULT partition. Updating reference.", identifier);
					medRequest.setMedication(new Reference("Medication/" + medicationId));
				}
			} else {
				log.info("MedicationReference does not start with expected prefix. Skipping validation.");
			}
		} else {
			log.info("No MedicationReference present or reference is null. Skipping validation.");
		}
	}

	private String findMedicationId(String identifier) {
		try {
			String[] parts = identifier.split("\\|", 2);
			if (parts.length < 2) {
				log.info("Invalid identifier format. Expected 'system|value'.");
				return null;
			}

			Bundle results = fhirClient
				.search()
				.forResource(Medication.class)
				.where(new TokenClientParam("identifier").exactly().systemAndCode(parts[0], parts[1]))
				.returnBundle(Bundle.class)
				.execute();

			if (results != null && !results.getEntry().isEmpty()) {
				Medication medication = (Medication) results.getEntryFirstRep().getResource();
				return medication.getIdElement().getIdPart();
			}
		} catch (Exception e) {
			log.error("Exception during Medication search: {}", e.getMessage(), e);
		}
		return null;
	}

	private boolean initializeFhirClient(ServletRequestDetails requestDetails) {
		String baseUrl = requestDetails.getFhirServerBase();
		baseUrl = baseUrl == null || baseUrl.isEmpty() ? null : baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

		if (baseUrl == null) {
			log.info("Cannot initialize FHIR client outside of HTTP request context.");
			return false;
		}

		String lastSegment = baseUrl.substring(baseUrl.lastIndexOf("/", baseUrl.length() - 2) + 1, baseUrl.length() - 1);
		baseUrl = !"DEFAULT".equals(lastSegment) ? baseUrl.substring(0, baseUrl.lastIndexOf("/", baseUrl.length() - 2) + 1) + "DEFAULT/" : baseUrl;

		FhirContext fhirContext = requestDetails.getFhirContext();

		this.fhirClient = fhirContext.newRestfulGenericClient(baseUrl);
		configureClientAuthentication(requestDetails);
		log.info("FhirClient initialized with base URL: {}", baseUrl);
		return true;
	}

	private void configureClientAuthentication(ServletRequestDetails requestDetails) {
		String authHeader = requestDetails.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			this.fhirClient.registerInterceptor(new BearerTokenAuthInterceptor(authHeader.substring(7)));
			log.info("FhirClient configured with Authorization token");
		}
	}
}