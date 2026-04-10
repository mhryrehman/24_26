package ca.uhn.fhir.jpa.starter.curemd;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Yasir Rehman
 * Description:
 * This class serves as a centralized holder for Keycloak-related configuration constants
 * and partitioning settings. It retrieves values from the Spring `Environment` and initializes
 * static variables with corresponding property values at runtime.
 */
@Component
public class KeycloakConstants {
	public static String extensionUrl;

	public static String tokenUrl;

	public static String authorizeUrl;

	public static String manageUrl;

	public static String introspectUrl;

	public static String revokeUrl;
	public static String issuerUrl;
	public static String jwksUrl;

	public static boolean partitioningEnabled = false;
	public static boolean allowReferencesAcrossPartitions = false;
	public static boolean partitioningIncludeInSearchHashes = true;
	public static long bulkExportFileRetentionPeriodHours = 2;

	public static String introspectionUri;
	public static String clientId;
	public static String clientSecret;
	public static boolean useIntrospection;

	@Autowired
	private Environment environment;

	/**
	 * Initializes the static constants for Keycloak OAuth2 endpoints and partitioning settings
	 * by retrieving values from the application properties using the Spring `Environment`.
	 *
	 * This method is annotated with `@PostConstruct` to ensure it runs after the Spring container has initialized.
	 */
	@PostConstruct
	public void init() {
		KeycloakConstants.extensionUrl = environment.getProperty("keycloak.extension-url");
		KeycloakConstants.tokenUrl = environment.getProperty("keycloak.token-url");
		KeycloakConstants.authorizeUrl = environment.getProperty("keycloak.authorize-url");
		KeycloakConstants.manageUrl = environment.getProperty("keycloak.manage-url");
		KeycloakConstants.introspectUrl = environment.getProperty("keycloak.introspect-url");
		KeycloakConstants.revokeUrl = environment.getProperty("keycloak.revoke-url");
		KeycloakConstants.issuerUrl = environment.getProperty("keycloak.issuer-url");
		KeycloakConstants.jwksUrl = environment.getProperty("keycloak.jwks-url");
		KeycloakConstants.introspectionUri = environment.getProperty("keycloak.introspect-url");
		KeycloakConstants.clientId = environment.getProperty("keycloak.resource");
		KeycloakConstants.clientSecret = environment.getProperty("keycloak.credentials.secret");
		KeycloakConstants.useIntrospection = Boolean.parseBoolean(
			environment.getProperty("keycloak.use-introspection", "false"));

		KeycloakConstants.partitioningEnabled = environment.containsProperty("hapi.fhir.partitioning.enabled");
		KeycloakConstants.allowReferencesAcrossPartitions = Boolean.parseBoolean(
			environment.getProperty("hapi.fhir.partitioning.allow_references_across_partitions", "false"));
		KeycloakConstants.partitioningIncludeInSearchHashes = Boolean.parseBoolean(
			environment.getProperty("hapi.fhir.partitioning.partitioning_include_in_search_hashes", "true"));
		KeycloakConstants.bulkExportFileRetentionPeriodHours = Long.parseLong(environment.getProperty("hapi.fhir.bulk-export.file-retention-hours", "2"));
	}



	/**
	 * Builds and returns a SMART on FHIR configuration map.
	 *
	 * @return a {@link Map} representing the SMART on FHIR configuration.
	 *
	 * @see KeycloakConstants
	 * @see SmartConfigConstants
	 */
	public static Map<String, Object> buildSmartConfiguration(){
		Map<String, Object> smartConfig = new HashMap<>();

		smartConfig.put("authorization_endpoint", KeycloakConstants.authorizeUrl);
		smartConfig.put("token_endpoint", KeycloakConstants.tokenUrl);
		smartConfig.put("scopes_supported", SmartConfigConstants.SCOPES_SUPPORTED_WITH_RS());
		smartConfig.put("response_types_supported", SmartConfigConstants.RESPONSE_TYPES_SUPPORTED);
		smartConfig.put("management_endpoint", KeycloakConstants.manageUrl);
		smartConfig.put("introspection_endpoint", KeycloakConstants.introspectUrl);
		smartConfig.put("revocation_endpoint", KeycloakConstants.revokeUrl);
		smartConfig.put("capabilities", SmartConfigConstants.CAPABILITIES);
		smartConfig.put("grant_types_supported", SmartConfigConstants.GRANT_TYPES_SUPPORTED);
		smartConfig.put("code_challenge_methods_supported", SmartConfigConstants.CODE_CHALLENGE_METHOD_SUPPORTED);
		smartConfig.put("issuer", KeycloakConstants.issuerUrl);
		smartConfig.put("jwks_uri", KeycloakConstants.jwksUrl);

		return smartConfig;


	}

}
