package ca.uhn.fhir.jpa.starter.curemd.customproviders.models;

// EhrPhone.java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EhrPhone {
	private String phoneType;
	private String phoneNo;
	private Integer index;

	public String getPhoneType() { return phoneType; }
	public void setPhoneType(String phoneType) { this.phoneType = phoneType; }

	public String getPhoneNo() { return phoneNo; }
	public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }

	@Override
	public String toString() {
		return "EhrPhone{" +
			"phoneType='" + phoneType + '\'' +
			", phoneNo='" + phoneNo + '\'' +
			", index=" + index +
			'}';
	}
}
