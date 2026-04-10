package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.curemd.CommonConstants;
import ca.uhn.fhir.jpa.starter.curemd.KeycloakConstants;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.*;
import org.jboss.logging.Logger;

import java.util.*;

/**
 * Author: Yasir Rehman
 * Description:
 * adds custom extensions to the Capability Statement's `security` section,
 * providing OAuth2 endpoint details like token, authorization, introspection, revocation,
 * and management URLs.
 */
@Interceptor
public class CapabilityStatementCustomizer {

	private static final String US_CORE_PROFILE_BASE = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-";
	private static final String US_CORE_PATIENT_PROFILE = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient";
	private static final String US_CORE_OBSERVATION_PROFILE = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation";

	private static final String US_CORE_CAPABILITY_URL = "http://hl7.org/fhir/us/core/CapabilityStatement/us-core-server";
	private static final String CS_EXPECTATION_URL = "http://hl7.org/fhir/StructureDefinition/capabilitystatement-expectation";
	private static final String US_CORE_VERSION = "6.1.0";
	public static final Set<String> SUPPORTED_RESOURCES = Set.of("AllergyIntolerance", "Appointment", "CarePlan", "CareTeam", "Communication", "Condition", "Coverage", "Device", "DiagnosticReport", "DocumentReference", "Encounter", "Endpoint", "FamilyMemberHistory", "Goal", "Group", "Immunization", "Location", "Media", "Medication", "MedicationDispense", "MedicationRequest", "Observation", "Organization", "Patient", "Practitioner", "PractitionerRole", "Procedure", "Provenance", "QuestionnaireResponse", "RelatedPerson", "ServiceRequest", "Specimen");
	private static final EnumSet<CapabilityStatement.TypeRestfulInteraction> ALLOWED_INTERACTIONS = EnumSet.of(CapabilityStatement.TypeRestfulInteraction.READ, CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE);

	Logger LGR = Logger.getLogger(CapabilityStatementCustomizer.class.getName());

	/**
	 * Hook method triggered when the Capability Statement is being generated.
	 * This method adds custom extensions to the Capability Statement's `security` section,
	 * providing OAuth2 endpoint details like token, authorization, introspection, revocation,
	 * and management URLs.
	 *
	 * @param theCapabilityStatement The Capability Statement being generated.
	 *                               It is cast to a `CapabilityStatement` to allow modification.
	 */
	@Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
	public void customize(IBaseConformance theCapabilityStatement) {

		CapabilityStatement cs = (CapabilityStatement) theCapabilityStatement;

		cs.setUrl(US_CORE_CAPABILITY_URL);
		cs.setVersion(US_CORE_VERSION);

		cs.addInstantiates(US_CORE_CAPABILITY_URL);
//		if (!cs.getInstantiates().isEmpty()) {
//			cs.getInstantiates().get(0)
//				.addExtension(new Extension(CS_EXPECTATION_URL,
//					new CodeType("SHALL")));
//		}


		Extension securityExtension = new Extension(KeycloakConstants.extensionUrl);

		securityExtension.addExtension(new Extension("token", new UriType(KeycloakConstants.tokenUrl)));
		securityExtension.addExtension(new Extension("authorize", new UriType(KeycloakConstants.authorizeUrl)));
		securityExtension.addExtension(new Extension("manage", new UriType(KeycloakConstants.manageUrl)));
		securityExtension.addExtension(new Extension("introspect", new UriType(KeycloakConstants.introspectUrl)));
		securityExtension.addExtension(new Extension("revoke", new UriType(KeycloakConstants.revokeUrl)));

		cs.getRestFirstRep().getSecurity().addExtension(securityExtension);

		cs.getRestFirstRep().getSecurity().setCors(true);
		CodeableConcept smartService = new CodeableConcept();
		smartService.addCoding(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/restful-security-service").setCode("SMART-on-FHIR"));
		cs.getRestFirstRep().getSecurity().addService(smartService);

		configureUsCoreReadOnlyResources(cs);
	}

	/**
	 * Ensures the CapabilityStatement advertises support for required US Core profiles:
	 * This is needed to satisfy Inferno test 10.1.05:
	 * "Capability Statement lists support for required US Core Profiles".
	 */
	private void configureUsCoreReadOnlyResources(CapabilityStatement cs) {
		LGR.info("Configuring US Core Read only resources.");

		if (cs == null || cs.getRest() == null || cs.getRest().isEmpty()) {
			LGR.info("CapabilityStatement REST component is missing or empty — nothing to do.");
			return;
		}

		CapabilityStatement.CapabilityStatementRestComponent rest = cs.getRestFirstRep();
		if (rest == null || rest.getResource() == null) {
			LGR.info("REST component has no resources — nothing to do.");
			return;
		}
		LGR.infof("CapabilityStatement has %d resources defined in REST component.", rest.getResource().size());
		// Keep only hardcoded supported resources
		List<CapabilityStatement.CapabilityStatementRestResourceComponent> supportedResources =
			rest.getResource().stream()
				.filter(Objects::nonNull)
				.filter(r -> {
					String t = r.getType();
					return t != null && SUPPORTED_RESOURCES.contains(t);
				})
				.toList();

		LGR.infof("Found %d supported resources in CapabilityStatement after filtering.", supportedResources.size());

		for (CapabilityStatement.CapabilityStatementRestResourceComponent res : supportedResources) {

			String resourceType = res.getType();
			if (resourceType == null || resourceType.isEmpty()) {
				continue;
			}

			// Clear interactions and set only READ and SEARCH-TYPE
			res.getInteraction().clear();
			for (CapabilityStatement.TypeRestfulInteraction interaction : ALLOWED_INTERACTIONS) {
				res.addInteraction().setCode(interaction);
			}


			List<String> suppProfiles = CommonConstants.SUPPORTED_PROFILES_BY_RESOURCE.get(resourceType);
			if (suppProfiles != null && !suppProfiles.isEmpty()) {
				// (Optional) set the "primary" profile as the first item
				res.setProfile(suppProfiles.get(0));
				for (String profileUrl : suppProfiles) {
					addSupportedProfileIfMissing(res, profileUrl);
				}
				LGR.debug("Applied hardcoded supportedProfiles for " + resourceType + ": " + suppProfiles.size());
			} else {
				String profileUrl = US_CORE_PROFILE_BASE + resourceType.toLowerCase();
				res.setProfile(profileUrl);
				addSupportedProfileIfMissing(res, profileUrl);
			}

		}
		rest.setResource(supportedResources);
	}

	private CapabilityStatement.CapabilityStatementRestResourceComponent findOrCreateResource(
		CapabilityStatement.CapabilityStatementRestComponent rest, String resourceType) {

		Optional<CapabilityStatement.CapabilityStatementRestResourceComponent> existing =
			rest.getResource().stream()
				.filter(r -> resourceType.equals(r.getType()))
				.findFirst();

		if (existing.isPresent()) {
			LGR.info("Found existing resource in CapabilityStatement: " + resourceType);
			return existing.get();
		}

		CapabilityStatement.CapabilityStatementRestResourceComponent created =
			new CapabilityStatement.CapabilityStatementRestResourceComponent();
		created.setType(resourceType);
		rest.addResource(created);
		LGR.info("Created new resource in CapabilityStatement: " + resourceType);
		return created;
	}

	private void addSupportedProfileIfMissing(
		CapabilityStatement.CapabilityStatementRestResourceComponent res,
		String profileUrl
	) {
		boolean alreadyPresent = res.getSupportedProfile().stream()
			.anyMatch(c -> profileUrl.equals(c.getValueAsString()));

		if (!alreadyPresent) {
			res.addSupportedProfile(profileUrl);
			LGR.debug("Added supportedProfile for " + res.getType() + ": " + profileUrl);
		} else {
			LGR.debug("supportedProfile already present for " + res.getType() + ": " + profileUrl);
		}
	}

}

