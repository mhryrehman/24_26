package ca.uhn.fhir.jpa.starter.curemd.customproviders.models;

// EhrRace.java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EhrRace {
	private String subRace;
	private String parentRace;
	private String raceCode;

	public String getSubRace() { return subRace; }
	public void setSubRace(String subRace) { this.subRace = subRace; }

	public String getParentRace() { return parentRace; }
	public void setParentRace(String parentRace) { this.parentRace = parentRace; }

	public String getRaceCode() { return raceCode; }
	public void setRaceCode(String raceCode) { this.raceCode = raceCode; }

	@Override
	public String toString() {
		return "EhrRace{" +
			"subRace='" + subRace + '\'' +
			", parentRace='" + parentRace + '\'' +
			", raceCode='" + raceCode + '\'' +
			'}';
	}
}
