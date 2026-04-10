package ca.uhn.fhir.jpa.starter.curemd.customproviders.models;

// EhrPatientData.java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EhrPatientData {
	private String resourceType;
	private Integer practiceId;
	private Integer patientId;
	private String title;
	private String suffix;
	private String firstName;
	private String middleName;
	private String lastName;
	private String previousFirstName;
	private String previousLastName;
	private String gender;
	private String birthDate;
	private String maritalStatus;
	private String ethnicity;
	private String parentEthnicity;
	private Boolean isActive;
	private String email;
	private Integer inActiveReason;
	private String deathDate;
	private String ethnicityCode;
	private String tenantId;
	private Integer resourceId;
	private String ts_ms;

	private List<EhrRace> Races;
	private List<EhrAddress> Addresses;
	private List<EhrPhone> Phones;
	private List<EhrLanguage> Languages;
	private List<EhrContact> Contacts;

	public Integer getPatientId() { return patientId; }
	public void setPatientId(Integer patientId) { this.patientId = patientId; }

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	public String getSuffix() { return suffix; }
	public void setSuffix(String suffix) { this.suffix = suffix; }

	public String getFirstName() { return firstName; }
	public void setFirstName(String firstName) { this.firstName = firstName; }

	public String getMiddleName() { return middleName; }
	public void setMiddleName(String middleName) { this.middleName = middleName; }

	public String getLastName() { return lastName; }
	public void setLastName(String lastName) { this.lastName = lastName; }

	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }

	public String getBirthDate() { return birthDate; }
	public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

	public String getMaritalStatus() { return maritalStatus; }
	public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

	public String getEthnicity() { return ethnicity; }
	public void setEthnicity(String ethnicity) { this.ethnicity = ethnicity; }

	public String getEthnicityCode() { return ethnicityCode; }
	public void setEthnicityCode(String ethnicityCode) { this.ethnicityCode = ethnicityCode; }

	public Boolean getIsActive() { return isActive; }
	public void setIsActive(Boolean active) { isActive = active; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public List<EhrAddress> getAddresses() { return Addresses; }
	public void setAddresses(List<EhrAddress> addresses) { Addresses = addresses; }

	public List<EhrPhone> getPhones() { return Phones; }
	public void setPhones(List<EhrPhone> phones) { Phones = phones; }

	public List<EhrLanguage> getLanguages() { return Languages; }
	public void setLanguages(List<EhrLanguage> languages) { Languages = languages; }

	public List<EhrContact> getContacts() { return Contacts; }
	public void setContacts(List<EhrContact> contacts) { Contacts = contacts; }

	public Integer getResourceId() { return resourceId; }
	public void setResourceId(Integer resourceId) { this.resourceId = resourceId; }

	@Override
	public String toString() {
		return "EhrPatientData{" +
			"resourceType='" + resourceType + '\'' +
			", practiceId=" + practiceId +
			", patientId=" + patientId +
			", title='" + title + '\'' +
			", suffix='" + suffix + '\'' +
			", firstName='" + firstName + '\'' +
			", middleName='" + middleName + '\'' +
			", lastName='" + lastName + '\'' +
			", previousFirstName='" + previousFirstName + '\'' +
			", previousLastName='" + previousLastName + '\'' +
			", gender='" + gender + '\'' +
			", birthDate='" + birthDate + '\'' +
			", maritalStatus='" + maritalStatus + '\'' +
			", ethnicity='" + ethnicity + '\'' +
			", parentEthnicity='" + parentEthnicity + '\'' +
			", isActive=" + isActive +
			", email='" + email + '\'' +
			", inActiveReason=" + inActiveReason +
			", deathDate='" + deathDate + '\'' +
			", ethnicityCode='" + ethnicityCode + '\'' +
			", tenantId='" + tenantId + '\'' +
			", resourceId=" + resourceId +
			", ts_ms='" + ts_ms + '\'' +
			", Races=" + Races +
			", Addresses=" + Addresses +
			", Phones=" + Phones +
			", Languages=" + Languages +
			", Contacts=" + Contacts +
			'}';
	}
}