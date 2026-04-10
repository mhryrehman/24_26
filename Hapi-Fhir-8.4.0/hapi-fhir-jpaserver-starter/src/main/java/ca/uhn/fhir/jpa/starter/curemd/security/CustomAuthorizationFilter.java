package ca.uhn.fhir.jpa.starter.curemd.security;

import ca.uhn.fhir.jpa.entity.Search;
import ca.uhn.fhir.jpa.search.cache.ISearchCacheSvc;
import ca.uhn.fhir.jpa.starter.curemd.KeycloakConstants;
import ca.uhn.fhir.jpa.starter.curemd.audit.AuditAttrs;
import ca.uhn.fhir.jpa.starter.curemd.audit.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Author: Yasir Rehman
 * Description:
 * This custom filter is responsible for handling authorization for FHIR API requests. It validates
 * the incoming requests based on JWT tokens and checks for required scopes, roles, and tenant/partition
 * permissions. It intercepts all incoming requests, applies authorization logic, and determines whether
 * the request should proceed or be denied with an appropriate error response.
 */
@Component
public class CustomAuthorizationFilter extends OncePerRequestFilter {

	private static final Logger LGR = Logger.getLogger(CustomAuthorizationFilter.class.getName());

	private final ISearchCacheSvc iSearchCacheSvc;
	private final AuditService auditService;

	public CustomAuthorizationFilter(ISearchCacheSvc iSearchCacheSvc, AuditService auditService) {
		this.iSearchCacheSvc = iSearchCacheSvc;
		this.auditService = auditService;
	}
	/**
	 * Filters each incoming request to validate authorization based on JWT tokens and URI patterns.
	 * Public URIs are allowed without validation, while protected URIs require valid tokens and appropriate
	 * roles/scopes. If validation fails, an error response is sent; otherwise, the request is passed to the
	 * next filter in the chain.
	 *
	 * @param request     the HTTP servlet request
	 * @param response    the HTTP servlet response
	 * @param filterChain the filter chain to pass the request to the next filter if authorized
	 * @throws IOException if an I/O error occurs during filtering
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws IOException {
		try {
			String requestUri = request.getRequestURI();
			LGR.info("Custom authorization filter invoked for URI: " + requestUri + "and query parameter: " + request.getQueryString());

			if (AuthUtils.isSmartConfigurationRequest(requestUri)) {
				prepareSmartConfigurationResponse(response);
				return;
			}

			// If the URI is public, continue the request without further checks
			if (Constants.PUBLIC_URIS.contains(requestUri) || requestUri.matches(Constants.METADATA_URL_REGEX)) {
				LGR.info("Public URI accessed: " + requestUri + ". Skipping authorization checks.");
				filterChain.doFilter(request, response);
				return;
			}

			// Retrieve the JWT token from the SecurityContext
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal)) {
				LGR.warning("Unauthorized access attempt to URI: " + requestUri);
				sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, Constants.UNAUTHORIZED_TOKEN_MESSAGE);
				filterChain.doFilter(request, response);
				return;
			}

			String partitionName = AuthUtils.extractPartitionName(requestUri);
			List<String> roles = AuthUtils.extractRolesFromPrincipal(principal);
			String requiredScope = AuthUtils.getRequiredScope(request);
			List<String> jwtScopes = AuthUtils.extractFhirScopes(principal);

			// populate attributes for interceptor + for filter logs
			AuditAttrs.setFromSecurityFilter(request, partitionName, requiredScope, roles, jwtScopes,
				AuthUtils.extractUsername(principal), AuthUtils.extractUserId(principal), AuthUtils.extractClientId(principal));

			if (Constants.AUDIT_ENDPOINTS.contains(requiredScope) && (roles.contains(Constants.ADMIN_ROLE) || roles.contains(Constants.AUDIT_ROLE))) {
				LGR.info("Audit endpoint accessed by admin/audit role. Granting access to URI: " + requestUri);
				filterChain.doFilter(request, response);
				return;
			}

			if (roles.contains(Constants.ADMIN_ROLE) || roles.contains(Constants.BULK_DATA_ROLE)) {
				LGR.info("Admin/Bulk Data role detected. Granting access to URI: " + requestUri);
				filterChain.doFilter(request, response);
				return;
			}
			// Check if partition name in URI is DEFAULT or matches with the tenantId in the JWT claims
			if (KeycloakConstants.partitioningEnabled && !jwtScopes.contains(Constants.PARTITION_ALL) && !AuthUtils.isPartitionValid(partitionName, principal)) {
				LGR.info("Invalid tenant access attempt to partition: " + partitionName + " for URI: " + requestUri);
				sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, Constants.INVALID_TENANT_ID_MESSAGE);
				return;
			}

			// Retrieve and validate required scopes from JWT and request header
			if (requiredScope.isEmpty()) {
				String searchId = request.getParameter("_getpages");
				if (requestUri.contains("/fhir") && StringUtils.hasText(searchId)) {
					//Validate if the request is for paginated search results (_getpages) and ensure that the token (jwtScopes) includes the required scope for the requested resource type.
					if (validateSearchPageAccess(jwtScopes, searchId, request, response)) {
						filterChain.doFilter(request, response);
					}
					LGR.info("Required scope not found for URI: " + requestUri);
					return;
				} else {
					LGR.info("Invalid URI access attempt without required scope: " + requestUri);
					sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, Constants.INVALID_URI_MESSAGE);
				}
				return;
			}

			if (Constants.BULK_EXPORT.equals(requiredScope)) {
				boolean hasWildcardAccess = jwtScopes.contains("*.*") || jwtScopes.contains("*.read");
				List<String> allowedResources = Constants.getReadScopeBaseNames(jwtScopes);
				String allowedTypeParam = Constants.getCommaSeparatedValidScopes(allowedResources);

				if (!hasWildcardAccess) {
					String typeParam = request.getParameter("_type");

					boolean hasInvalidTypeParam = StringUtils.hasText(typeParam) && !AuthUtils.validateTypeParamValues(typeParam, allowedResources);
					if (hasInvalidTypeParam) {
						LGR.info("Scope missing for requested resource types in _type parameter: " + typeParam);
						sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, Constants.SCOPE_MISSING_4_TYPE_QUERY_PARAM_MESSAGE);
						return;
					}

					if (null != typeParam && typeParam.isEmpty()) {
						LGR.info("No valid resources found for bulk export in _type parameter.");
						sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, Constants.NO_VALID_RESOURCES_FOR_BULK_EXPORT);
						return;
					}

					if (!StringUtils.hasText(allowedTypeParam)) {
						LGR.info("No eligible resources found for bulk export based on token scopes.");
						sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, Constants.NO_ELIGIBLE_RESOURCES_FOR_BULK_EXPORT);
						return;
					}
				}

				LGR.info("Modifying bulk export request to include only allowed resource types: " + allowedTypeParam);
				ModifiedHttpServletRequest modifiedRequest = new ModifiedHttpServletRequest(request, allowedTypeParam);
				filterChain.doFilter(modifiedRequest, response);
				return;
			}

			if (Constants.PARTIALLY_AUTHORIZED_ENDPOINTS.contains(requiredScope)) {
				LGR.info("Partially authorized endpoint accessed: " + requestUri);
				filterChain.doFilter(request, response);
				return;
			}

			if (requiredScope.startsWith("$") && !roles.contains(Constants.ADMIN_ROLE) && !roles.contains(Constants.BULK_DATA_ROLE)) {
				LGR.info("Admin privilege required for operation: " + requiredScope + " on URI: " + requestUri);
				sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, Constants.ADMIN_PRIVILEGE_REQUIRED_MESSAGE);
				return;
			}

			LGR.info("scopes available in token : " + jwtScopes);
			GranularScopeConstraints gsc = GranularScopeConstraints.fromScopes(AuthUtils.extractScopes(principal));
			request.setAttribute(GranularScopeConstraints.ATTR_NAME, gsc);

			if (!jwtScopes.contains("*.*") && !AuthUtils.isScopeExistInTokenScopes(jwtScopes, request)) {
				LGR.info("Required scope not found in token for URI: " + requestUri);
				sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, Constants.REQUIRED_SCOPE_NOT_FOUND_MESSAGE);
				return;
			}
			LGR.info("Authorization successful for URI: " + requestUri);
			filterChain.doFilter(request, response);
		} catch (Exception ex) {
			ex.printStackTrace();
			if (!response.isCommitted()) {
				sendErrorResponse(request, response, HttpServletResponse.SC_FORBIDDEN, "Some error occurred");
			}
		}
	}

	/**
	 * Validates whether the requested paginated resource (_getpages) is accessible based on the provided token scopes.
	 *
	 * @param jwtScopes The list of scopes extracted from the token, defining the user's access permissions.
	 * @param searchId  The unique identifier of the cached search (_getpages).
	 * @param response  The {@link HttpServletResponse} to send error responses in case of validation failure.
	 * @return {@code true} if the validation is successful and access is granted, {@code false} otherwise.
	 * @throws IOException if an error occurs while sending the error response.
	 */
	private boolean validateSearchPageAccess(List<String> jwtScopes, String searchId, HttpServletRequest request, HttpServletResponse response) throws IOException {
		Optional<Search> optionalSearch = iSearchCacheSvc.fetchByUuid(searchId, null);
		if (optionalSearch.isEmpty()) {
			sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Access Denied: Search ID [" + searchId + "] does not exist");
			return false;
		}
		String resourceType = optionalSearch.get().getResourceType();
		if (!AuthUtils.hasRequiredScopeForResource(jwtScopes, resourceType)) {
			sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, Constants.SCOPE_MISSING_4_RESOURCE_TYPE_MESSAGE + resourceType);
			return false;
		}
		return true;
	}

	/**
	 * Sends an error response with a given status code and message.
	 *
	 * @param response   the HTTP servlet response
	 * @param statusCode the status code to set
	 * @param message    the error message to include
	 * @throws IOException if an input or output error occurs
	 */
	private void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, int statusCode, String message) throws IOException {
		response.setStatus(statusCode);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		String jsonResponse = String.format("{\"error\": \"%s\"}", message);
		response.getWriter().write(jsonResponse);
		LGR.warning(message);
		auditService.write(request, response, "AUTHZ_DENIED", AuthUtils.getRequiredScope(request), null, null);
	}

	/**
	 * Handles the SMART on FHIR configuration request by writing a JSON response containing
	 * OAuth2 endpoints and supported capabilities.
	 *
	 * @param response the HttpServletResponse to write the SMART configuration to
	 * @throws IOException if an error occurs while writing the response
	 */
	private void prepareSmartConfigurationResponse(HttpServletResponse response) throws IOException {
		LGR.info("preparing response for SMART config request");

		Map<String, Object> smartConfig = KeycloakConstants.buildSmartConfiguration();

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		String jsonResponse = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(smartConfig);
		response.getWriter().write(jsonResponse);
	}
}
