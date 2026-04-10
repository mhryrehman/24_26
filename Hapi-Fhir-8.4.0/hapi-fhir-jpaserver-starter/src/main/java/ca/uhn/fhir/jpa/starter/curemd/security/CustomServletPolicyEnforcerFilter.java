package ca.uhn.fhir.jpa.starter.curemd.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.keycloak.adapters.authorization.integration.jakarta.ServletPolicyEnforcerFilter;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/**
 * Author: Yasir Rehman
 * Description:
 * This class extends Keycloak's `ServletPolicyEnforcerFilter` to provide custom handling
 * for authorization exceptions. It intercepts requests and enforces Keycloak authorization
 * policies. If an exception occurs, it provides meaningful error responses in JSON format,
 * ensuring better client-side error handling.
 */
public class CustomServletPolicyEnforcerFilter extends ServletPolicyEnforcerFilter {

	/**
	 * Constructor to initialize the custom filter with a Keycloak `ConfigurationResolver`.
	 *
	 * @param configurationResolver The Keycloak configuration resolver used to enforce policies.
	 */
	public CustomServletPolicyEnforcerFilter(ConfigurationResolver configurationResolver) {
		super(configurationResolver);
	}

	/**
	 * Overrides the `doFilter` method to enforce Keycloak authorization policies and handle
	 * exceptions during the filtering process. If an exception occurs, the method sets the
	 * appropriate HTTP status and returns a JSON error response.
	 *
	 * @param servletRequest  The incoming servlet request.
	 * @param servletResponse The servlet response to be sent back.
	 * @param chain           The filter chain to pass the request to the next filter if authorized.
	 * @throws IOException      If an I/O error occurs during request handling.
	 * @throws ServletException If a servlet error occurs during request handling.
	 */
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		try {
			super.doFilter(request, response, chain);
		} catch (Exception e) {
			e.printStackTrace();
			if(e.getCause() instanceof HttpResponseException httpResponseException) {
				response.setStatus(httpResponseException.getStatusCode());
				response.setContentType("application/json");
				response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid client credentials\"}");
			} else {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				response.setContentType("application/json");
				response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Authentication failed: Please check logs.\"}");
			}
		}
	}
}
