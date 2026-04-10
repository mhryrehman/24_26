package ca.uhn.fhir.jpa.starter.curemd.customproviders;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EhrPatientDto {

    @JsonProperty("practiceId")
    private int practiceId;

    @JsonProperty("patientId")
    private int patientId;

    @JsonProperty("religion")
    private String religion;

    @JsonProperty("title")
    private String title;

    @JsonProperty("suffix")
    private String suffix;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("middleName")
    private String middleName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("previousFirstName")
    private String previousFirstName;

    @JsonProperty("previousLastName")
    private String previousLastName;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("birthDate")
    private String birthDate;

    @JsonProperty("maritalStatus")
    private String maritalStatus;

    @JsonProperty("ethnicity")
    private String ethnicity;

    @JsonProperty("parentEthnicity")
    private String parentEthnicity;

    @JsonProperty("isActive")
    private boolean active;

    @JsonProperty("email")
    private String email;

    @JsonProperty("inActiveReason")
    private int inActiveReason;

    @JsonProperty("deathDate")
    private String deathDate;

    @JsonProperty("ethnicityCode")
    private String ethnicityCode;

    @JsonProperty("photo")
    private String photo;

    @JsonProperty("Races")
    private List<RaceDto> races;

    @JsonProperty("Addresses")
    private List<AddressDto> addresses;

    @JsonProperty("Phones")
    private List<PhoneDto> phones;

    @JsonProperty("Languages")
    private List<LanguageDto> languages;

    @JsonProperty("Contacts")
    private List<ContactDto> contacts;

    @JsonProperty("preferredPharmacy")
    private int preferredPharmacy;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("resourceId")
    private int resourceId;

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("op")
    private String op;

    @JsonProperty("ts_ms")
    private long tsMs;

    // Getters and setters...

    public int getPracticeId() { return practiceId; }
    public void setPracticeId(int practiceId) { this.practiceId = practiceId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

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

    public String getPreviousFirstName() { return previousFirstName; }
    public void setPreviousFirstName(String previousFirstName) { this.previousFirstName = previousFirstName; }

    public String getPreviousLastName() { return previousLastName; }
    public void setPreviousLastName(String previousLastName) { this.previousLastName = previousLastName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public String getEthnicity() { return ethnicity; }
    public void setEthnicity(String ethnicity) { this.ethnicity = ethnicity; }

    public String getParentEthnicity() { return parentEthnicity; }
    public void setParentEthnicity(String parentEthnicity) { this.parentEthnicity = parentEthnicity; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getInActiveReason() { return inActiveReason; }
    public void setInActiveReason(int inActiveReason) { this.inActiveReason = inActiveReason; }

    public String getDeathDate() { return deathDate; }
    public void setDeathDate(String deathDate) { this.deathDate = deathDate; }

    public String getEthnicityCode() { return ethnicityCode; }
    public void setEthnicityCode(String ethnicityCode) { this.ethnicityCode = ethnicityCode; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public List<RaceDto> getRaces() { return races; }
    public void setRaces(List<RaceDto> races) { this.races = races; }

    public List<AddressDto> getAddresses() { return addresses; }
    public void setAddresses(List<AddressDto> addresses) { this.addresses = addresses; }

    public List<PhoneDto> getPhones() { return phones; }
    public void setPhones(List<PhoneDto> phones) { this.phones = phones; }

    public List<LanguageDto> getLanguages() { return languages; }
    public void setLanguages(List<LanguageDto> languages) { this.languages = languages; }

    public List<ContactDto> getContacts() { return contacts; }
    public void setContacts(List<ContactDto> contacts) { this.contacts = contacts; }

    public int getPreferredPharmacy() { return preferredPharmacy; }
    public void setPreferredPharmacy(int preferredPharmacy) { this.preferredPharmacy = preferredPharmacy; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }

    public long getTsMs() { return tsMs; }
    public void setTsMs(long tsMs) { this.tsMs = tsMs; }

    // Nested DTOs
    public static class RaceDto {
        private String raceCode;
        private String raceDescription;

        public String getRaceCode() { return raceCode; }
        public void setRaceCode(String raceCode) { this.raceCode = raceCode; }

        public String getRaceDescription() { return raceDescription; }
        public void setRaceDescription(String raceDescription) { this.raceDescription = raceDescription; }
    }

    public static class AddressDto {
        private String address1;
        private String address2;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private String periodStart;
        private String periodEnd;

        public String getAddress1() { return address1; }
        public void setAddress1(String address1) { this.address1 = address1; }

        public String getAddress2() { return address2; }
        public void setAddress2(String address2) { this.address2 = address2; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getPeriodStart() { return periodStart; }
        public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }

        public String getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(String periodEnd) { this.periodEnd = periodEnd; }
    }

    public static class PhoneDto {
        private String number;
        private String type;
        private boolean isPrimary;

        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }
    }

    public static class LanguageDto {
        private String languageCode;
        private String language;
        private boolean preferred;

        public String getLanguageCode() { return languageCode; }
        public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public boolean isPreferred() { return preferred; }
        public void setPreferred(boolean preferred) { this.preferred = preferred; }
    }

    public static class ContactDto {
        private String name;
        private String relationship;
        private String phone;
        private String email;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRelationship() { return relationship; }
        public void setRelationship(String relationship) { this.relationship = relationship; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
