package ca.uhn.fhir.jpa.starter.curemd.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.Properties;

@Configuration
public class ExternalApiConfig {

	private static final Logger log = LoggerFactory.getLogger(ExternalApiConfig.class);

	private static final String ENV_PATH = "DOCUMENT_INTERCEPTOR_CONFIG_PATH";
	private static final String DEFAULT_PATH = "document-interceptor.properties";

	private final Properties props = new Properties();

	@PostConstruct
	public void load() {
		try (InputStream input = getClass().getClassLoader().getResourceAsStream(DEFAULT_PATH)) {
			props.load(input);
			log.info("Loaded document-interceptor properties from {}", DEFAULT_PATH);
		} catch (Exception e) {
			throw new IllegalStateException(
				"Failed to load document-interceptor.properties from " + DEFAULT_PATH, e
			);
		}
	}

	/**
	 * Returns the value for ANY key.
	 * Throws exception if missing or blank.
	 */
	public String get(String key) {
		return getRequired(key);
	}

	/**
	 * Returns the value for ANY key or null if missing.
	 */
	public String getOptional(String key) {
		String value = props.getProperty(key);
		return isBlank(value) ? null : value;
	}


	public String getAuthUrl() {
		return getRequired("auth.server.url");
	}

	public String getClientId() {
		return getRequired("auth.client.id");
	}

	public String getClientSecret() {
		return getRequired("auth.client.secret");
	}

	public String getGrantType() {
		return getRequired("auth.grant.type");
	}

    /* ======================
       Tenant credential lookup
       ====================== */

	public String getUsername(String tenantId) {
		return getRequired(tenantId + ".password");
	}

	public String getPassword(String tenantId) {
		return getRequired(tenantId + ".username");
	}

    /* ======================
       Helpers
       ====================== */

	private String key(String tenantId, String key) {
		if (isBlank(tenantId) || isBlank(key)) {
			throw new IllegalArgumentException("tenantId and userKey are required");
		}
		return tenantId + "." + key;
	}

	private String getRequired(String key) {
		String value = props.getProperty(key);
		if (isBlank(value)) {
			throw new IllegalStateException("Missing required property: " + key);
		}
		return value;
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	@Bean
	public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
		f.setConnectTimeout(5000);
		f.setReadTimeout(30000);
		return new RestTemplate(f);
	}
}
