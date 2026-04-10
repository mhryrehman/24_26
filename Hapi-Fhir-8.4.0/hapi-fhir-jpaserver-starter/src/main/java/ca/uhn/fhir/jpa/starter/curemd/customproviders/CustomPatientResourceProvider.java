package ca.uhn.fhir.jpa.starter.curemd.customproviders;

import ca.uhn.fhir.jpa.starter.curemd.customproviders.client.EhrPatientClient;
import ca.uhn.fhir.jpa.starter.curemd.customproviders.models.EhrPatientData;
import ca.uhn.fhir.jpa.starter.curemd.customproviders.models.EhrPatientResponse;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.SearchContainedModeEnum;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.jboss.logging.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class CustomPatientResourceProvider implements IResourceProvider {

	private static final Logger log = Logger.getLogger(CustomPatientResourceProvider.class);
	private static final EhrPatientClient clientservice = new EhrPatientClient(new RestTemplate());
    public static final String PATIENT_JSON = """
        {
          "practiceId":2,
          "patientId":12100,
          "religion":null,
          "title":"Mr",
          "suffix":"",
          "firstName":"Matthew",
          "middleName":"",
          "lastName":"Cade",
          "previousFirstName":"",
          "previousLastName":"",
          "gender":"M",
          "birthDate":"2013-10-17T00:00:00",
          "maritalStatus":"SIN",
          "ethnicity":"Not Hispanic or Latino",
          "parentEthnicity":"Not Hispanic/Latino",
          "isActive":false,
          "email":"matthew@osxofulk.com",
          "inActiveReason":0,
          "deathDate":null,
          "ethnicityCode":"2186-5",
          "photo":null,
          "Races":[],
          "Addresses":[{
              "address1":"3493 Moonlight Drive",
              "address2":"",
              "city":"NEW YORK CITY",
              "state":"NY",
              "zipCode":"10005",
              "country":"United States of America",
              "periodStart":"",
              "periodEnd":null
          }],
          "Phones":[],
          "Languages":[{
              "languageCode":"en",
              "language":"English (US)",
              "preferred":true
          }],
          "Contacts":[],
          "preferredPharmacy":0,
          "tenantId":"CMDGO",
          "resourceId":12100,
          "resourceType":"Patient",
          "op":"C",
          "ts_ms":1750664911817
        }
        """;

    private final ca.uhn.fhir.jpa.starter.curemd.customproviders.ApiClient apiClient = new ca.uhn.fhir.jpa.starter.curemd.customproviders.ApiClient();


    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }

    @Read
    public Patient read(@IdParam IdType id) {
//        if (!"123".equals(id.getIdPart())) {
//            throw new ResourceNotFoundException(id);
//        }
        //clientservice.getpatient
		 log.info("Received read request for Patient with ID: " + id.getIdPart());
		 EhrPatientData ehrPatientData = clientservice.fetchPatientById(id.getIdPartAsLong());
		 log.info("Fetched patient data from EHR: " + ehrPatientData);

		 EhrPatientToFhirMapper mapper = new EhrPatientToFhirMapper();
        return mapper.toFhirPatient(ehrPatientData);

//        Patient patient = new Patient();
//        patient.setId("123");
//        patient.addIdentifier().setSystem("http://hospital.org").setValue("MRN123456");
//        patient.addName().setFamily("Doe").addGiven("John");
//        patient.setGender(Enumerations.AdministrativeGender.MALE);
//        patient.setBirthDateElement(new DateType("1980-01-01"));
//        return patient;
    }

    @Search
    public IBundleProvider search(HttpServletRequest theServletRequest, HttpServletResponse theServletResponse, RequestDetails theRequestDetails, @Description(shortDefinition = "Search the contents of the resource's data using a filter") @OptionalParam(name = "_filter") StringAndListParam theFtFilter, @Description(shortDefinition = "Search the contents of the resource's data using a fulltext search") @OptionalParam(name = "_content") StringAndListParam theFtContent, @Description(shortDefinition = "Search the contents of the resource's narrative using a fulltext search") @OptionalParam(name = "_text") StringAndListParam theFtText, @Description(shortDefinition = "Search for resources which have the given tag") @OptionalParam(name = "_tag") TokenAndListParam theSearchForTag, @Description(shortDefinition = "Search for resources which have the given security labels") @OptionalParam(name = "_security") TokenAndListParam theSearchForSecurity, @Description(shortDefinition = "Search for resources which have the given profile") @OptionalParam(name = "_profile") UriAndListParam theSearchForProfile, @Description(shortDefinition = "Search the contents of the resource's data using a list") @OptionalParam(name = "_list") StringAndListParam theList, @Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") TokenAndListParam theResourceLanguage, @Description(shortDefinition = "Search for resources which have the given source value (Resource.meta.source)") @OptionalParam(name = "_source") UriAndListParam theSearchForSource, @Description(shortDefinition = "Return resources linked to by the given target") @OptionalParam(name = "_has") HasAndListParam theHas, @Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id, @Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam the_lastUpdated, @Description(shortDefinition = "The profile of the resource") @OptionalParam(name = "_profile") UriAndListParam the_profile, @Description(shortDefinition = "The security of the resource") @OptionalParam(name = "_security") TokenAndListParam the_security, @Description(shortDefinition = "The tag of the resource") @OptionalParam(name = "_tag") TokenAndListParam the_tag, @Description(shortDefinition = "Whether the patient record is active") @OptionalParam(name = "active") TokenAndListParam theActive, @Description(shortDefinition = "A server defined search that may match any of the string fields in the Address, including line, city, district, state, country, postalCode, and/or text") @OptionalParam(name = "address") StringAndListParam theAddress, @Description(shortDefinition = "A city specified in an address") @OptionalParam(name = "address-city") StringAndListParam theAddress_city, @Description(shortDefinition = "A country specified in an address") @OptionalParam(name = "address-country") StringAndListParam theAddress_country, @Description(shortDefinition = "A postalCode specified in an address") @OptionalParam(name = "address-postalcode") StringAndListParam theAddress_postalcode, @Description(shortDefinition = "A state specified in an address") @OptionalParam(name = "address-state") StringAndListParam theAddress_state, @Description(shortDefinition = "A use code specified in an address") @OptionalParam(name = "address-use") TokenAndListParam theAddress_use, @Description(shortDefinition = "The patient's date of birth") @OptionalParam(name = "birthdate") DateRangeParam theBirthdate, @Description(shortDefinition = "The date of death has been provided and satisfies this search value") @OptionalParam(name = "death-date") DateRangeParam theDeath_date, @Description(shortDefinition = "This patient has been marked as deceased, or as a death date entered") @OptionalParam(name = "deceased") TokenAndListParam theDeceased, @Description(shortDefinition = "A value in an email contact") @OptionalParam(name = "email") TokenAndListParam theEmail, @Description(shortDefinition = "A portion of the family name of the patient") @OptionalParam(name = "family") StringAndListParam theFamily, @Description(shortDefinition = "Gender of the patient") @OptionalParam(name = "gender") TokenAndListParam theGender, @Description(shortDefinition = "Patient's nominated general practitioner, not the organization that manages the record") @OptionalParam(name = "general-practitioner",targetTypes = {}) ReferenceAndListParam theGeneral_practitioner, @Description(shortDefinition = "A portion of the given name of the patient") @OptionalParam(name = "given") StringAndListParam theGiven, @Description(shortDefinition = "A patient identifier") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier, @Description(shortDefinition = "Language code (irrespective of use value)") @OptionalParam(name = "language") TokenAndListParam theLanguage, @Description(shortDefinition = "All patients linked to the given patient") @OptionalParam(name = "link",targetTypes = {}) ReferenceAndListParam theLink, @Description(shortDefinition = "A server defined search that may match any of the string fields in the HumanName, including family, give, prefix, suffix, suffix, and/or text") @OptionalParam(name = "name") StringAndListParam theName, @Description(shortDefinition = "The organization that is the custodian of the patient record") @OptionalParam(name = "organization",targetTypes = {}) ReferenceAndListParam theOrganization, @Description(shortDefinition = "A value in a phone contact") @OptionalParam(name = "phone") TokenAndListParam thePhone, @Description(shortDefinition = "A portion of either family or given name using some kind of phonetic matching algorithm") @OptionalParam(name = "phonetic") StringAndListParam thePhonetic, @Description(shortDefinition = "The value in any kind of telecom details of the patient") @OptionalParam(name = "telecom") TokenAndListParam theTelecom, @RawParam Map<String, List<String>> theAdditionalRawParams, @IncludeParam Set<Include> theIncludes, @IncludeParam(reverse = true) Set<Include> theRevIncludes, @Sort SortSpec theSort, @Count Integer theCount, @Offset Integer theOffset, SummaryEnum theSummaryMode, SearchTotalModeEnum theSearchTotalMode, SearchContainedModeEnum theSearchContainedMode) {

        ca.uhn.fhir.jpa.starter.curemd.customproviders.PatientResponseDTO dto = apiClient.getPatientFromApi(
                theName != null ? extractString(theName) : "",
                theFamily != null ? extractString(theFamily) : ""
        );

        if (dto == null) {
            return new SimpleBundleProvider(Collections.emptyList());
        }

        List<Patient> results = List.of(ca.uhn.fhir.jpa.starter.curemd.customproviders.EhrToFhirMapper.map(dto));

        return new SimpleBundleProvider(results) {
            @Override
            public Integer size() {
                return results.size(); // total result count
            }

            @Override
            public IPrimitiveType<Date> getPublished() {
                return new DateTimeType(new Date());
            }

            @Override
            public String getUuid() {
                return "urn:uuid:" + results.get(0).getId(); // optional tracking UUID
            }
        };
    }

    public String extractString(StringAndListParam param) {
        if (param != null && !param.getValuesAsQueryTokens().isEmpty()) {
            StringOrListParam orList = param.getValuesAsQueryTokens().get(0);
            if (!orList.getValuesAsQueryTokens().isEmpty()) {
                return orList.getValuesAsQueryTokens().get(0).getValue();
            }
        }
        return null;
    }

}
