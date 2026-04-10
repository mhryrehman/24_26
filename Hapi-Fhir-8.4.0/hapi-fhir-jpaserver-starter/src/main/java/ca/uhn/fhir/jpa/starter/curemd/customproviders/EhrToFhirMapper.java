package ca.uhn.fhir.jpa.starter.curemd.customproviders;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.*;

import java.util.List;

public class EhrToFhirMapper {
    public static Patient fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ca.uhn.fhir.jpa.starter.curemd.customproviders.EhrPatientDto dto = objectMapper.readValue(json, ca.uhn.fhir.jpa.starter.curemd.customproviders.EhrPatientDto.class);

            Patient patient = new Patient();
            patient.setId(String.valueOf(dto.getPatientId()));

            // Name
            HumanName name = new HumanName();
            name.setFamily(dto.getLastName());
            name.addGiven(dto.getFirstName());
            name.setPrefix(List.of(new StringType(dto.getTitle())));
            patient.addName(name);

            // Gender
            if ("M".equalsIgnoreCase(dto.getGender())) {
                patient.setGender(Enumerations.AdministrativeGender.MALE);
            } else if ("F".equalsIgnoreCase(dto.getGender())) {
                patient.setGender(Enumerations.AdministrativeGender.FEMALE);
            } else {
                patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
            }

            // Birthdate
            patient.setBirthDateElement(new DateType(dto.getBirthDate().substring(0, 10)));

            // Marital status
            CodeableConcept maritalStatus = new CodeableConcept();
            maritalStatus.addCoding(new Coding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus")
                    .setCode("S")
                    .setDisplay("Never Married"));
            patient.setMaritalStatus(maritalStatus);

            // Active
            patient.setActive(dto.isActive());

            // Email
            patient.addTelecom(new ContactPoint()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(dto.getEmail())
                    .setUse(ContactPoint.ContactPointUse.HOME));

            // Address
            if (dto.getAddresses() != null && !dto.getAddresses().isEmpty()) {
                ca.uhn.fhir.jpa.starter.curemd.customproviders.EhrPatientDto.AddressDto a = dto.getAddresses().get(0);
                Address address = new Address();
                address.setLine(List.of(new StringType(a.getAddress1())));
                address.setCity(a.getCity());
                address.setState(a.getState());
                address.setPostalCode(a.getZipCode());
                address.setCountry(a.getCountry());
                patient.addAddress(address);
            }

            // Language
            if (dto.getLanguages() != null && !dto.getLanguages().isEmpty()) {
                ca.uhn.fhir.jpa.starter.curemd.customproviders.EhrPatientDto.LanguageDto lang = dto.getLanguages().get(0);
                Patient.PatientCommunicationComponent communication = new Patient.PatientCommunicationComponent();
                communication.setLanguage(new CodeableConcept().addCoding(
                        new Coding().setSystem("urn:ietf:bcp:47").setCode(lang.getLanguageCode()).setDisplay(lang.getLanguage())
                ));
                communication.setPreferred(lang.isPreferred());
                patient.addCommunication(communication);
            }

            // Ethnicity extension
            Extension ethnicityExt = new Extension();
            ethnicityExt.setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");
            ethnicityExt.addExtension(new Extension("text", new StringType(dto.getEthnicity())));
            ethnicityExt.addExtension(new Extension("code", new Coding().setCode(dto.getEthnicityCode())));
            patient.addExtension(ethnicityExt);

            return patient;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse patient JSON", e);
        }
    }

    public static Patient map(ca.uhn.fhir.jpa.starter.curemd.customproviders.PatientResponseDTO dto) {
        Patient patient = new Patient();
        patient.setId(String.valueOf(dto.Id));

        // Name
        HumanName name = new HumanName()
                .setFamily(dto.Name.LastName)
                .addGiven(dto.Name.FirstName);
        if (dto.Name.Title != null) name.setPrefix(List.of(new StringType(dto.Name.Title)));
        patient.addName(name);

        // Gender
        if ("M".equalsIgnoreCase(dto.Gender)) {
            patient.setGender(Enumerations.AdministrativeGender.MALE);
        } else if ("F".equalsIgnoreCase(dto.Gender)) {
            patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        }

        // BirthDate
        if (dto.DOB != null) {
            patient.setBirthDateElement(new DateType(dto.DOB.substring(0, 10)));
        }

        // Email
        if (dto.Email != null) {
            patient.addTelecom(new ContactPoint()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(dto.Email)
                    .setUse(ContactPoint.ContactPointUse.HOME));
        }

        // Phone
        if (dto.Phones != null && !dto.Phones.isEmpty()) {
            for (ca.uhn.fhir.jpa.starter.curemd.customproviders.PatientResponseDTO.Phone phone : dto.Phones) {
                patient.addTelecom(new ContactPoint()
                        .setSystem(ContactPoint.ContactPointSystem.PHONE)
                        .setValue(phone.Number));
            }
        }

        // Address
        if (dto.Address != null) {
            Address address = new Address()
                    .addLine(dto.Address.Address1)
                    .addLine(dto.Address.Address2)
                    .setCity(dto.Address.City)
                    .setState(dto.Address.State)
                    .setPostalCode(dto.Address.ZipCode);
            patient.addAddress(address);
        }

        return patient;
    }
}
