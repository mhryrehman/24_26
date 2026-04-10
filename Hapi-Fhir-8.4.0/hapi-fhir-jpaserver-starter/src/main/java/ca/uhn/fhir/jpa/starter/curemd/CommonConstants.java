package ca.uhn.fhir.jpa.starter.curemd;

import java.util.List;
import java.util.Map;

public class CommonConstants {
	public static final Map<String, List<String>> SUPPORTED_PROFILES_BY_RESOURCE = Map.ofEntries(
		Map.entry("AllergyIntolerance", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-allergyintolerance"
		)),
		Map.entry("CarePlan", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-careplan"
		)),
		Map.entry("CareTeam", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-careteam"
		)),
		Map.entry("Condition", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition-encounter-diagnosis",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition-problems-health-concerns"
		)),
		Map.entry("Coverage", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-coverage"
		)),
		Map.entry("Device", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-implantable-device"
		)),
		Map.entry("DiagnosticReport", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-note",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab"
		)),
		Map.entry("DocumentReference", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-documentreference"
		)),
		Map.entry("Encounter", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter"
		)),
		Map.entry("Goal", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-goal"
		)),
		Map.entry("Immunization", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-immunization"
		)),
		Map.entry("Location", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-location"
		)),
		Map.entry("Medication", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-medication"
		)),
		Map.entry("MedicationDispense", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationdispense"
		)),
		Map.entry("MedicationRequest", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationrequest"
		)),
		Map.entry("Observation", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-pregnancystatus",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-pregnancyintent",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-occupation",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-respiratory-rate",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-simple-observation",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-heart-rate",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-body-temperature",
			"http://hl7.org/fhir/us/core/StructureDefinition/pediatric-weight-for-height",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-pulse-oximetry",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-smokingstatus",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-sexual-orientation",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-head-circumference",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-body-height",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-bmi",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-screening-assessment",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-blood-pressure",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-clinical-result",
			"http://hl7.org/fhir/us/core/StructureDefinition/pediatric-bmi-for-age",
			"http://hl7.org/fhir/us/core/StructureDefinition/head-occipital-frontal-circumference-percentile",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-body-weight",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-vital-signs"
		)),
		Map.entry("Organization", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-organization"
		)),
		Map.entry("Patient", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient"
		)),
		Map.entry("Practitioner", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner"
		)),
		Map.entry("PractitionerRole", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitionerrole"
		)),
		Map.entry("Procedure", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-procedure"
		)),
		Map.entry("Provenance", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-provenance"
		)),
		Map.entry("Questionnaire", List.of(
			"http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire"
		)),
		Map.entry("QuestionnaireResponse", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-questionnaireresponse"
		)),
		Map.entry("RelatedPerson", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-relatedperson"
		)),
		Map.entry("ServiceRequest", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-servicerequest"
		)),
		Map.entry("Specimen", List.of(
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-specimen"
		))
	);
}
