package ca.uhn.fhir.jpa.starter.curemd;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

/**
 * Author: Yasir Rehman
 * Description:
 * This filter intercepts requests to the `/fhir/.well-known/smart-configuration` endpoint and generates
 * a JSON response containing SMART on FHIR configuration details. These details include OAuth2 endpoint
 * information such as authorization, token, management, introspection, and revocation endpoints, as well
 * as supported scopes, response types, and capabilities.
 */
@WebFilter(urlPatterns = "/fhir/.well-known/smart-configuration")
public class SmartConfigurationFilter implements Filter {

	/**
	 * Handles the request for the SMART on FHIR configuration endpoint.
	 * If the requested URI matches `/fhir/.well-known/smart-configuration`, the filter generates
	 * a JSON response containing the SMART configuration. Otherwise, the request is passed along
	 * the filter chain.
	 *
	 * @param request  The incoming `ServletRequest`.
	 * @param response The `ServletResponse` to be sent back.
	 * @param chain    The filter chain to pass control to the next filter if applicable.
	 * @throws IOException      If an I/O error occurs during request handling.
	 * @throws ServletException If a servlet error occurs during request handling.
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		System.out.println("SmartConfigurationFilter has been called.");
		if ("/fhir/.well-known/smart-configuration".equals(httpRequest.getRequestURI())) {
			Map<String, Object> smartConfig = KeycloakConstants.buildSmartConfiguration();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			httpResponse.setContentType("application/json");
			httpResponse.setStatus(HttpServletResponse.SC_OK);
			String jsonResponse = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(smartConfig);
			httpResponse.getWriter().write(jsonResponse);
		} else {
			chain.doFilter(request, response);
		}
	}

	/**
	 * Called when the filter is being destroyed.
	 * Cleanup logic can be added here if needed, although it is currently empty.
	 */
	@Override
	public void destroy() {
		// Cleanup logic if needed
	}
}
