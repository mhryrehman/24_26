package ca.uhn.fhir.jpa.starter.curemd;

import java.util.List;

/**
 * Author: Yasir Rehman
 * Description:
 * This class contains constants used for configuring SMART on FHIR settings. These constants
 * define the supported scopes, response types, and capabilities required for OAuth2 and SMART
 * on FHIR functionality.
 */
public class SmartConfigConstants {
	public static final List<String> SCOPES_SUPPORTED = List.of(
		"launch", "launch/patient", "openid", "fhirUser", "offline_access",
		"patient/*.read", "patient/Medication.read", "patient/AllergyIntolerance.read",
		"patient/CarePlan.read", "patient/CareTeam.read", "patient/Condition.read",
		"patient/Device.read", "patient/DiagnosticReport.read", "patient/DocumentReference.read",
		"patient/Encounter.read", "patient/Goal.read", "patient/Immunization.read",
		"patient/Location.read", "patient/MedicationRequest.read", "patient/Observation.read",
		"patient/Organization.read", "patient/Patient.read", "patient/Practitioner.read",
		"patient/Procedure.read", "patient/Provenance.read", "patient/PractitionerRole.read",
		"user/*.read", "user/Medication.read", "user/AllergyIntolerance.read",
		"user/CarePlan.read", "user/CareTeam.read", "user/Condition.read", "user/Device.read",
		"user/DiagnosticReport.read", "user/DocumentReference.read", "user/Encounter.read",
		"user/Goal.read", "user/Immunization.read", "user/Location.read", "user/MedicationRequest.read",
		"user/Observation.read", "user/Organization.read", "user/Patient.read", "user/Practitioner.read",
		"user/Procedure.read", "user/Provenance.read", "user/PractitionerRole.read", "system/Medication.read",
		"system/AllergyIntolerance.read", "system/CarePlan.read", "system/*.read", "system/CareTeam.read",
		"system/Condition.read", "system/Device.read", "system/DiagnosticReport.read", "system/DocumentReference.read",
		"system/Encounter.read", "system/Goal.read", "system/Immunization.read", "system/Location.read",
		"system/MedicationRequest.read", "system/Observation.read", "system/Organization.read", "system/Patient.read",
		"system/Practitioner.read", "system/Procedure.read", "system/Provenance.read"
	);

	public static List<String> SCOPES_SUPPORTED_WITH_RS() {
		return SCOPES_SUPPORTED.stream()
			.flatMap(scope -> {
				if (scope.endsWith(".read")) {
					return java.util.stream.Stream.of(
						scope,
						scope.replace(".read", ".rs")
					);
				}
				return java.util.stream.Stream.of(scope);
			})
			.distinct()
			.sorted()
			.toList();
	}

	public static final List<String> GRANT_TYPES_SUPPORTED = List.of("authorization_code", "client_credentials", "refresh_token", "implicit", "password");
	public static final List<String> CODE_CHALLENGE_METHOD_SUPPORTED = List.of("S256");
	public static final List<String> RESPONSE_TYPES_SUPPORTED = List.of("code", "code id_token", "token", "token id_token");
	public static final List<String> CAPABILITIES = List.of(
		"launch-ehr", "launch-standalone", "client-public", "client-confidential-symmetric",
		"sso-openid-connect", "context-banner", "context-style", "context-ehr-patient", "context-ehr-encounter",
		"context-standalone-patient", "permission-offline", "permission-patient", "permission-user","client-confidential-asymmetric",
		"authorize-post", "permission-v1", "permission-v2"
	);
}
