package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

;

public class AuthResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("practice_url")
	private String practiceUrl;
	public AuthResponse() {
	}

	public AuthResponse(String accessToken, String practiceUrl) {
		this.accessToken = accessToken;
		this.practiceUrl = practiceUrl;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getPracticeUrl() {
		return practiceUrl;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public void setPracticeUrl(String practiceUrl) {
		this.practiceUrl = practiceUrl;
	}

	@Override
	public String toString() {
		return "Access Token: " + accessToken + "\nPractice URL: " + practiceUrl;
	}
}
