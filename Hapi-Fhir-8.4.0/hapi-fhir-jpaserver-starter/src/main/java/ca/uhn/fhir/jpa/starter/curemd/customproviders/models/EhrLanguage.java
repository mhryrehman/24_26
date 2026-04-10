package ca.uhn.fhir.jpa.starter.curemd.customproviders.models;

// EhrLanguage.java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EhrLanguage {
	private String languageCode;
	private String language;
	private Boolean preferred;

	public String getLanguageCode() { return languageCode; }
	public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

	public String getLanguage() { return language; }
	public void setLanguage(String language) { this.language = language; }

	public Boolean getPreferred() { return preferred; }
	public void setPreferred(Boolean preferred) { this.preferred = preferred; }

	@Override
	public String toString() {
		return "EhrLanguage{" +
			"languageCode='" + languageCode + '\'' +
			", language='" + language + '\'' +
			", preferred=" + preferred +
			'}';
	}
}