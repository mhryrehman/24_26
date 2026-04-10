package ca.uhn.fhir.jpa.starter.curemd.customproviders.models;

// EhrPatientResponse.java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EhrPatientResponse {
	private boolean success;
	private EhrPatientData data;

	public boolean isSuccess() { return success; }
	public void setSuccess(boolean success) { this.success = success; }

	public EhrPatientData getData() { return data; }
	public void setData(EhrPatientData data) { this.data = data; }
}
