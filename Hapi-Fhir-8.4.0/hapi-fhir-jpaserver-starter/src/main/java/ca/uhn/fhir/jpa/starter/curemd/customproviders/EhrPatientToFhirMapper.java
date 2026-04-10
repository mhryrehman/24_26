package ca.uhn.fhir.jpa.starter.curemd.customproviders;

// EhrPatientToFhirMapper.java

import ca.uhn.fhir.jpa.starter.curemd.customproviders.models.EhrAddress;
import ca.uhn.fhir.jpa.starter.curemd.customproviders.models.EhrLanguage;
import ca.uhn.fhir.jpa.starter.curemd.customproviders.models.EhrPatientData;
import ca.uhn.fhir.jpa.starter.curemd.customproviders.models.EhrPhone;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class EhrPatientToFhirMapper {

	public Patient toFhirPatient(EhrPatientData ehr) {
		Patient patient = new Patient();

		if (ehr.getResourceId() != null) {
			patient.setId(String.valueOf(ehr.getResourceId()));
		}

		// Identifier
		if (ehr.getPatientId() != null) {
			patient.addIdentifier()
				.setSystem("http://cmdlhrltx579:7777/api/FHIR/Patient")
				.setValue(String.valueOf(ehr.getPatientId()));
		}

		// Name
		HumanName name = patient.addName();

		if (ehr.getTitle() != null && !ehr.getTitle().isBlank()) {
			name.addPrefix(ehr.getTitle());
		}

		if (ehr.getFirstName() != null && !ehr.getFirstName().isBlank()) {
			name.addGiven(ehr.getFirstName());
		}

		if (ehr.getMiddleName() != null && !ehr.getMiddleName().isBlank()) {
			name.addGiven(ehr.getMiddleName());
		}

		if (ehr.getLastName() != null && !ehr.getLastName().isBlank()) {
			name.setFamily(ehr.getLastName());
		}

		if (ehr.getSuffix() != null && !ehr.getSuffix().isBlank()) {
			name.addSuffix(ehr.getSuffix());
		}

		// Gender
		patient.setGender(mapGender(ehr.getGender()));

		// Birth date
		if (ehr.getBirthDate() != null && !ehr.getBirthDate().isBlank()) {
			LocalDate dob = LocalDate.parse(ehr.getBirthDate().substring(0, 10));
			patient.setBirthDate(java.sql.Date.valueOf(dob));
		}

		// Active
		if (ehr.getIsActive() != null) {
			patient.setActive(ehr.getIsActive());
		}

		// Telecom
		if (ehr.getEmail() != null && !ehr.getEmail().isBlank()) {
			patient.addTelecom()
				.setSystem(ContactPoint.ContactPointSystem.EMAIL)
				.setValue(ehr.getEmail())
				.setUse(ContactPoint.ContactPointUse.HOME);
		}

		if (ehr.getPhones() != null) {
			for (EhrPhone phone : ehr.getPhones()) {
				if (phone.getPhoneNo() != null && !phone.getPhoneNo().isBlank()) {
					ContactPoint cp = patient.addTelecom()
						.setValue(phone.getPhoneNo())
						.setUse(ContactPoint.ContactPointUse.HOME);

					if (phone.getPhoneType() != null &&
						phone.getPhoneType().equalsIgnoreCase("mobile")) {
						cp.setSystem(ContactPoint.ContactPointSystem.PHONE);
						cp.setUse(ContactPoint.ContactPointUse.MOBILE);
					} else {
						cp.setSystem(ContactPoint.ContactPointSystem.PHONE);
					}
				}
			}
		}

		// Address
		if (ehr.getAddresses() != null) {
			for (EhrAddress addr : ehr.getAddresses()) {
				Address address = patient.addAddress();
				if (addr.getAddress1() != null) address.addLine(addr.getAddress1());
				if (addr.getAddress2() != null && !addr.getAddress2().isBlank()) address.addLine(addr.getAddress2());
				address.setCity(addr.getCity());
				address.setState(addr.getState());
				address.setPostalCode(addr.getZipCode());
				address.setCountry(addr.getCountry());
			}
		}

		// Language
		if (ehr.getLanguages() != null) {
			for (EhrLanguage lang : ehr.getLanguages()) {
				if (Boolean.TRUE.equals(lang.getPreferred()) && lang.getLanguageCode() != null) {
					patient.addCommunication()
						.setLanguage(new CodeableConcept()
							.setText(lang.getLanguage())
							.addCoding(new Coding()
								.setSystem("urn:ietf:bcp:47")
								.setCode(lang.getLanguageCode())))
						.setPreferred(true);
				}
			}
		}

		// You can add race / ethnicity as extensions later if needed

		return patient;
	}

	private Enumerations.AdministrativeGender mapGender(String gender) {
		if (gender == null) {
			return Enumerations.AdministrativeGender.UNKNOWN;
		}
		return switch (gender.trim().toUpperCase(Locale.ROOT)) {
			case "M", "MALE" -> Enumerations.AdministrativeGender.MALE;
			case "F", "FEMALE" -> Enumerations.AdministrativeGender.FEMALE;
			case "O", "OTHER" -> Enumerations.AdministrativeGender.OTHER;
			default -> Enumerations.AdministrativeGender.UNKNOWN;
		};
	}
}