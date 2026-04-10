package ca.uhn.fhir.jpa.starter.curemd.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Author: Yasir Rehman
 * Description:
 * This class defines constant values used throughout the application, primarily for
 * security-related purposes. It includes predefined URIs, claim keys, roles, default
 * partitioning, and error messages related to authorization and access control.
 */
public class Constants {
	public static final List<String> PUBLIC_URIS = Arrays.asList("/fhir/metadata", "/fhir/Composition", "/fhir/Parameters"
		, "/fhir/Binary", "/metadata/.well-known/smart-configuration","/smart/config/smart-style.json", "/fhir/.well-known/smart-configuration"
		, "/fhir/Bundle/BaseURLs", "/fhir/DEFAULT/Bundle/ServiceBaseURL", "/fhir/DEFAULT/Bundle/ServiceBaseURLs");
	public static final List<String> PARTIALLY_AUTHORIZED_ENDPOINTS = Arrays.asList("$export-poll-status", "Binary");
	private static final Set<String> FHIR_RESOURCES = new HashSet<>(Set.of(
		"Account", "ActivityDefinition", "AdverseEvent", "AllergyIntolerance", "Appointment",
		"AppointmentResponse", "AuditEvent", "Basic", "Binary", "BiologicallyDerivedProduct",
		"BodyStructure", "Bundle", "CapabilityStatement", "CarePlan", "CareTeam", "CatalogEntry",
		"ChargeItem", "ChargeItemDefinition", "Claim", "ClaimResponse", "ClinicalImpression",
		"CodeSystem", "Communication", "CommunicationRequest", "CompartmentDefinition",
		"Composition", "ConceptMap", "Condition", "Consent", "Contract", "Coverage",
		"CoverageEligibilityRequest", "CoverageEligibilityResponse", "DetectedIssue", "Device",
		"DeviceDefinition", "DeviceMetric", "DeviceRequest", "DeviceUseStatement",
		"DiagnosticReport", "DocumentManifest", "DocumentReference", "EffectEvidenceSynthesis",
		"Encounter", "Endpoint", "EnrollmentRequest", "EnrollmentResponse", "EpisodeOfCare",
		"EventDefinition", "Evidence", "EvidenceVariable", "ExampleScenario", "ExplanationOfBenefit",
		"FamilyMemberHistory", "Flag", "Goal", "GraphDefinition", "Group", "GuidanceResponse",
		"HealthcareService", "ImagingStudy", "Immunization", "ImmunizationEvaluation",
		"ImmunizationRecommendation", "ImplementationGuide", "InsurancePlan", "Invoice", "Library",
		"Linkage", "List", "Location", "Measure", "MeasureReport", "Media", "Medication",
		"MedicationAdministration", "MedicationDispense", "MedicationKnowledge", "MedicationRequest",
		"MedicationStatement", "MedicinalProduct", "MedicinalProductAuthorization",
		"MedicinalProductContraindication", "MedicinalProductIndication", "MedicinalProductIngredient",
		"MedicinalProductInteraction", "MedicinalProductManufactured", "MedicinalProductPackaged",
		"MedicinalProductPharmaceutical", "MedicinalProductUndesirableEffect", "MessageDefinition",
		"MessageHeader", "MolecularSequence", "NamingSystem", "NutritionOrder", "Observation",
		"ObservationDefinition", "OperationDefinition", "OperationOutcome", "Organization",
		"OrganizationAffiliation", "Parameters", "Patient", "PaymentNotice", "PaymentReconciliation",
		"Person", "PlanDefinition", "Practitioner", "PractitionerRole", "Procedure", "Provenance",
		"Questionnaire", "QuestionnaireResponse", "RelatedPerson", "RequestGroup", "ResearchDefinition",
		"ResearchElementDefinition", "ResearchStudy", "ResearchSubject", "Resource", "RiskAssessment",
		"RiskEvidenceSynthesis", "Schedule", "SearchParameter", "ServiceRequest", "Slot", "Specimen",
		"SpecimenDefinition", "StructureDefinition", "StructureMap", "Subscription", "Substance",
		"SubstanceNucleicAcid", "SubstancePolymer", "SubstanceProtein", "SubstanceReferenceInformation",
		"SubstanceSourceMaterial", "SubstanceSpecification", "SupplyDelivery", "SupplyRequest", "Task",
		"TerminologyCapabilities", "TestReport", "TestScript", "ValueSet", "VerificationResult",
		"VisionPrescription"
	));
	public static final String SMART_CONFIG_URL = "/fhir/.well-known/smart-configuration/";
	public static final String SMART_CONFIG_URL_REGEX = "^/fhir(?:/[^/]+)?/\\.well-known/smart-configuration/?$";
	public static final String METADATA_URL_REGEX = "^/fhir(?:/[^/]+)?/metadata/?$";
	public static final String FHIR_URI = "/fhir/";
	public static final String BULK_EXPORT = "$export";
	public static final String EXPORT_POLL_STATUS = "$export-poll-status";
	public static final Set<String> AUDIT_ENDPOINTS = Set.of("$audit", "$audit-count");
	public static final String FHIR_SCOPES_CLAIM = "scope";
	public static final String TENANT_ID_CLAIM = "tenantId";
	public static final String REALM_ACCESS_CLAIM = "realm_access";
	public static final String ROLES_CLAIM = "roles";
	public static final String CLIENT_ID_CLAIM = "client_id";
	public static final String DEFAULT_PARTITION = "DEFAULT";
	public static final String ADMIN_ROLE = "admin";
	public static final String BULK_DATA_ROLE = "BulkData";
	public static final String PARTITION_ALL = "partition_all";
	public static final String AUDIT_ROLE = "audit";
	public static final String UNAUTHORIZED_TOKEN_MESSAGE = "Unauthorized: Token not present or invalid.";
	public static final String INVALID_TENANT_ID_MESSAGE = "Access Denied:  Invalid tenant ID.";
	public static final String INVALID_URI_MESSAGE = "Access Denied: Invalid URI.";
	public static final String ADMIN_PRIVILEGE_REQUIRED_MESSAGE = "Access Denied: Admin privilege required.";
	public static final String REQUIRED_SCOPE_NOT_FOUND_MESSAGE = "Access Denied: You are not authorized to access this resource.";
	public static final String SCOPE_MISSING_4_RESOURCE_TYPE_MESSAGE = "Access Denied: Missing required scope for ResourceType: ";
	public static final String SCOPE_MISSING_4_TYPE_QUERY_PARAM_MESSAGE = "Access Denied: The requested resource(s) in the '_type' query parameter are not permitted.";
	public static final String NO_ELIGIBLE_RESOURCES_FOR_BULK_EXPORT = "Access Denied: No eligible resources available for bulk export.";
	public static final String NO_VALID_RESOURCES_FOR_BULK_EXPORT = "Access Denied: No valid resources found in the '_type' query parameter.";

	// SMART privilege keywords
	public static final String PRIV_READ = "read";
	public static final String PRIV_WRITE = "write";

	// CRUDS single-letter permissions
	public static final char PERM_CREATE = 'c';
	public static final char PERM_READ = 'r';
	public static final char PERM_UPDATE = 'u';
	public static final char PERM_DELETE = 'd';
	public static final char PERM_SEARCH = 's';
	public static final String WILDCARD = "*";

	public static final String GET = "GET";
	public static final String POST = "POST";
	public static final String PUT = "PUT";
	public static final String PATCH = "PATCH";
	public static final String DELETE = "DELETE";

	public static final String _SEARCH = "_search";
	public static final Pattern CATEGORY_SCOPE_PATTERN = Pattern.compile("^(?:.*/)?([^./?]+)\\.rs\\?category=(.+)$");

	/**
	 * Filters the input list of scopes and returns only valid FHIR resource scopes.
	 *
	 * @param scopes List of scopes to filter
	 * @return List of valid FHIR resource scopes
	 */
	public static List<String> filterValidScopes(List<String> scopes) {
		return scopes.stream()
			.filter(FHIR_RESOURCES::contains)  // Keep only scopes that exist in the FHIR resource set
			.collect(Collectors.toList());
	}

	/**
	 * Filters the input list of scopes and returns a comma-separated string of valid FHIR resources.
	 *
	 * @param allowedResources List of scopes to filter.
	 * @return Comma-separated string of valid FHIR resource scopes.
	 */
	public static String getCommaSeparatedValidScopes(List<String> allowedResources) {
		return allowedResources.stream()
			.filter(FHIR_RESOURCES::contains)  // Keep only valid FHIR resources
			.collect(Collectors.joining(",")); // Join with commas
	}

	/**
	 * Filters scopes that end with ".read" and returns a list of their base values (without ".read").
	 *
	 * @param scopes The list of scopes (e.g., ["openid", "Patient.read", "Observation.write"]).
	 * @return A list of scope names without ".read".
	 */
	public static List<String> getReadScopeBaseNames(List<String> scopes) {
		return scopes.stream()
			.filter(scope -> scope.endsWith(".read"))
			.map(scope -> scope.replace(".read", ""))
			.collect(Collectors.toList());
	}
}
