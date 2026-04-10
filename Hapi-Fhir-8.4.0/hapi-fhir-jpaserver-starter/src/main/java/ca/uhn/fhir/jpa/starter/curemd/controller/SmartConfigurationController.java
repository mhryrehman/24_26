package ca.uhn.fhir.jpa.starter.curemd.controller;

import ca.uhn.fhir.jpa.starter.curemd.KeycloakConstants;
import ca.uhn.fhir.jpa.starter.curemd.SmartConfigConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * Author: Yasir Rehman
 * Description:
 * This controller provides the SMART on FHIR configuration details required for OAuth2 and SMART
 * on FHIR workflows.
 */
@Controller
@RequestMapping
public class SmartConfigurationController {

	/**
	 * Handles GET requests to the `/smart-configuration` endpoint.
	 * Generates a JSON response containing SMART on FHIR configuration details.
	 *
	 * @return A `ResponseEntity` containing the SMART configuration as a JSON response.
	 */
	@GetMapping("/metadata/.well-known/smart-configuration")
	public ResponseEntity<Map<String, Object>> getSmartConfiguration() {
		Map<String, Object> response = Map.of(
			"authorization_endpoint", KeycloakConstants.authorizeUrl,
			"token_endpoint", KeycloakConstants.tokenUrl,
			"scopes_supported", SmartConfigConstants.SCOPES_SUPPORTED,
			"response_types_supported", SmartConfigConstants.RESPONSE_TYPES_SUPPORTED,
			"management_endpoint", KeycloakConstants.manageUrl,
			"introspection_endpoint", KeycloakConstants.introspectUrl,
			"revocation_endpoint", KeycloakConstants.revokeUrl,
			"capabilities", SmartConfigConstants.CAPABILITIES
		);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		return ResponseEntity.ok()
			.headers(headers)
			.body(response);
	}

	/**
	 * Handles GET requests to the `/smart/config/smart-style.json` endpoint.
	 * This method serves the `smart-style.json` file from the `resources/static` directory.
	 *
	 * @return A `ResponseEntity` containing the `smart-style.json` file as a JSON response.
	 * The response has the `application/json` content type.
	 */
	@GetMapping("/smart/config/smart-style.json")
	public ResponseEntity<Resource> getSmartStyle() {
		try {
			Resource resource = new ClassPathResource("static/smart-style.json");

			if (!resource.exists()) {
				return ResponseEntity.notFound().build();
			}

			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(resource);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}
}