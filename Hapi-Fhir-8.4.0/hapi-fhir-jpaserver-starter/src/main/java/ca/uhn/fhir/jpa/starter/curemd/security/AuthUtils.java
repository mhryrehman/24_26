package ca.uhn.fhir.jpa.starter.curemd.security;

import ca.uhn.fhir.jpa.starter.curemd.KeycloakConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.jboss.logging.Logger;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public final class AuthUtils {

	private static final Logger LGR = Logger.getLogger(AuthUtils.class.getName());

	private AuthUtils() {
	}

	/**
	 * Extracts FHIR scopes from the Principal.
	 *
	 * @param principal the principal
	 * @return a list of FHIR scopes
	 */
	public static List<String> extractFhirScopes(OAuth2AuthenticatedPrincipal principal) {
		if (principal == null) {
			return Collections.emptyList();
		}

		Object scopeClaim = principal.getAttribute(Constants.FHIR_SCOPES_CLAIM);
		if (scopeClaim == null) {
			return Collections.emptyList();
		}

		List<String> scopes;
		if (scopeClaim instanceof String str) {
			scopes = Arrays.asList(str.split(" "));
		} else if (scopeClaim instanceof Collection<?> collection) {
			scopes = collection.stream()
				.filter(Objects::nonNull)
				.map(Object::toString)
				.toList();
		} else {
			return Collections.emptyList();
		}

		return scopes.stream()
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(AuthUtils::normalizeSmartScope)
			.toList();
	}

	/**
	 * Removes SMART compartment prefix while preserving granular scopes.
	 *
	 * Examples:
	 *  patient/Observation.rs                           -> Observation.rs
	 *  patient/Observation.rs?category=system|code      -> Observation.rs?category=system|code
	 *  user/Condition.cruds                             -> Condition.cruds
	 *  system/*.read                                   -> *.read
	 */
	private static String normalizeSmartScope(String scope) {
		if (scope.startsWith("patient/")) {
			return scope.substring("patient/".length());
		}
		if (scope.startsWith("user/")) {
			return scope.substring("user/".length());
		}
		if (scope.startsWith("system/")) {
			return scope.substring("system/".length());
		}
		return scope;
	}

	/**
	 * Extracts scopes from the Principal.
	 *
	 * @param principal the principal
	 * @return a list of FHIR scopes
	 */
	public static List<String> extractScopes(OAuth2AuthenticatedPrincipal principal) {
		if (principal == null) {
			return Collections.emptyList();
		}

		Object scopeClaim = principal.getAttribute(Constants.FHIR_SCOPES_CLAIM);
		if (scopeClaim == null) {
			return Collections.emptyList();
		}

		if (scopeClaim instanceof String str) {
			return Arrays.asList(str.split(" "));
		} else if (scopeClaim instanceof Collection<?> collection) {
			return collection.stream()
				.filter(Objects::nonNull)
				.map(Object::toString)
				.toList();
		} else {
			return Collections.emptyList();
		}
	}


	/**
	 * Extracts the partition name (the first segment after /fhir/) from the URI.
	 *
	 * @param uri the request URI
	 * @return the partition name, or null if not found
	 */
	public static String extractPartitionName(String uri) {
		if (KeycloakConstants.partitioningEnabled) {
			int fhirIndex = uri.indexOf(Constants.FHIR_URI);
			if (fhirIndex != -1) {
				String pathAfterFhir = uri.substring(fhirIndex + Constants.FHIR_URI.length());
				String[] segments = pathAfterFhir.split("/");
				return segments.length > 0 ? segments[0] : null;
			}
		}
		return null;
	}

	/**
	 * Retrieves the required scope from the request URI.
	 *
	 * @param request the HTTP servlet request
	 * @return the required scope as a string
	 */
	public static String getRequiredScope(HttpServletRequest request) {
		String[] segments = request.getRequestURI().split("/");
		String requestedScope = null;
		if (KeycloakConstants.partitioningEnabled) {
			LGR.info("partitioning is enabled.");
			requestedScope = segments.length > 3 ? segments[3].split("\\?")[0] : "";
		} else {
			requestedScope = segments.length > 2 ? segments[2].split("\\?")[0] : "";
		}
		LGR.info("requested scope is : " + requestedScope);
		return requestedScope;
	}

	/**
	 * Checks if the required resource type exists in the provided token scopes.
	 *
	 * @param jwtScopes    The list of scopes extracted from the token, defining the user's access permissions.
	 * @param resourceType The type of the resource being accessed (e.g., "Patient", "Observation").
	 * @return {@code true} if the required resource type exists in the token scopes, {@code false} otherwise.
	 */
	public static boolean hasRequiredScopeForResource(List<String> jwtScopes, String resourceType) {
		if (jwtScopes.contains("*.*")) {
			return true;
		}
		return jwtScopes.stream().anyMatch(scope -> {
			if (isCategoryGranularScope(scope)) {
				LGR.info("Scope is a category granular scope: " + scope);     // Observation.rs?category=system|code
				scope = scope.contains("?") ? scope.substring(0, scope.indexOf('?')) : scope;
			}
			String[] segments = scope.split("\\.");
			return segments.length == 2
				&& ("*".equals(segments[0]) || resourceType.equals(segments[0]));
		});
	}

	/**
	 * Validates if the required scope exists in the token's scopes.
	 *
	 * @param jwtScopes the list of JWT scopes
	 * @param request   the HTTP servlet request
	 * @return true if the scope exists, otherwise false
	 */
	public static boolean isScopeExistInTokenScopes(List<String> jwtScopes, HttpServletRequest request) {
		String httpMethod = request.getMethod(); // e.g. GET, POST, PUT, DELETE

		String requiredScope = getRequiredScope(request);
		if (!StringUtils.hasText(requiredScope)) {
			LGR.infof("Required scope is empty or invalid for URI: {}", request.getRequestURI());
			return false;
		}
		// Detect if this is a HAPI FHIR POST-based search: POST .../_search
		boolean isPostSearch = isPostSearchRequest(request);

		// Map HTTP method to required permissions according to SMART v2 (c/r/u/d/s)
		Set<Character> requiredPermissions = mapHttpMethodToPermissions(httpMethod, isPostSearch);
		if (requiredPermissions.isEmpty()) {
			LGR.infof("HTTP method {} is not mapped to SMART permissions. Denying by default.", httpMethod);
			return false;
		}

		return jwtScopes.stream().anyMatch(scope -> isScopeMatching(scope, requiredScope, requiredPermissions));
	}

	private static boolean isScopeMatching(String scope,
														String requiredScope,
														Set<Character> requiredPermissions) {

		if (!StringUtils.hasText(scope)) {
			return false;
		}


		if (isCategoryGranularScope(scope)) {
			LGR.info("Scope is a category granular scope: " + scope);     // Observation.rs?category=system|code
			scope = scope.contains("?") ? scope.substring(0, scope.indexOf('?')) : scope;
		}

		// Example scope formats:
		//  - patient/Observation.rs
		//  - user/Patient.cruds
		//  - Observation.read
		String[] segments = scope.split("\\.");
		if (segments.length != 2) {
			return false;
		}

		String resourcePart = segments[0];   // e.g. "patient/Observation" or "Observation" or "*"
		String privilegePart = segments[1];  // e.g. "rs", "cruds", "read", "write", "*"

		if (!resourceMatches(resourcePart, requiredScope)) {
			return false;
		}

		return privilegeMatches(privilegePart, requiredPermissions);
	}

	/**
	 * Matches the resource part of the scope with the required resource.
	 * Examples:
	 * - "*" matches everything
	 * - "patient/Observation" endsWith("/Observation") → matches "Observation"
	 * - "Observation" equals requiredScope → matches
	 */
	private static boolean resourceMatches(String resourcePart, String requiredScope) {
		if (!StringUtils.hasText(resourcePart)) {
			return false;
		}

		if (Constants.WILDCARD.equals(resourcePart)) {
			return true;
		}

		return resourcePart.equals(requiredScope);
	}

	/**
	 * Checks if the privilege part (e.g. "rs", "cruds", "read", "write", "*")
	 * satisfies required permissions like { 'r', 's' }, { 'c' }, { 'u' }, { 'd' }.
	 */
	private static boolean privilegeMatches(String privilegePart, Set<Character> requiredPermissions) {
		if (!StringUtils.hasText(privilegePart)) {
			return false;
		}

		if (Constants.WILDCARD.equals(privilegePart)) {
			return true;
		}

		if (Constants.PRIV_READ.equalsIgnoreCase(privilegePart)) {
			return requiredPermissions.contains(Constants.PERM_READ) || requiredPermissions.contains(Constants.PERM_SEARCH);
		}
		if (Constants.PRIV_WRITE.equalsIgnoreCase(privilegePart)) {
			return requiredPermissions.stream().anyMatch(ch -> ch == Constants.PERM_CREATE || ch == Constants.PERM_UPDATE || ch == Constants.PERM_DELETE);
		}

		// SMART v2 CRUDS letters (e.g. "rs", "cruds")
		for (char ch : privilegePart.toCharArray()) {
			if (requiredPermissions.contains(ch)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Maps HTTP methods to SMART v2 permission letters.
	 * GET/HEAD            → r (read), s (search)
	 * POST (normal)       → c (create)
	 * POST (HAPI _search) → r (read), s (search)
	 * PUT/PATCH           → u (update)
	 * DELETE              → d (delete)
	 *
	 * @param method       the HTTP method
	 * @param isPostSearch whether this is a POST-based _search request
	 */
	private static Set<Character> mapHttpMethodToPermissions(String method, boolean isPostSearch) {
		if (method == null) {
			return Collections.emptySet();
		}

		return switch (method) {
			case Constants.GET -> new HashSet<>(Arrays.asList(Constants.PERM_READ, Constants.PERM_SEARCH));
			case Constants.POST -> {
				if (isPostSearch) {
					// POST .../_search → treat like GET search: r + s
					yield new HashSet<>(Arrays.asList(Constants.PERM_READ, Constants.PERM_SEARCH));
				} else {
					// Normal POST → create
					yield Collections.singleton(Constants.PERM_CREATE);
				}
			}
			case Constants.PUT, Constants.PATCH -> Collections.singleton(Constants.PERM_UPDATE);
			case Constants.DELETE -> Collections.singleton(Constants.PERM_DELETE);
			default -> Collections.emptySet();
		};
	}

	/**
	 * Extracts roles from the "realm_access" claim in the Principal Object.
	 *
	 * @param principal the principal
	 * @return a list of roles, or an empty list if none found
	 */
	public static List<String> extractRolesFromPrincipal(OAuth2AuthenticatedPrincipal principal) {
		Map<String, Object> realmAccess = principal.getAttribute(Constants.REALM_ACCESS_CLAIM);
		if (realmAccess != null && realmAccess.containsKey(Constants.ROLES_CLAIM)) {
			Object rolesObj = realmAccess.get(Constants.ROLES_CLAIM);
			if (rolesObj instanceof Collection<?> col) {
				return col.stream().map(String::valueOf).toList();
			}
		}
		// Some deployments also mirror roles into "roles" or "authorities"; add fallbacks if needed.
		return Collections.emptyList();
	}

	/**
	 * Validates if the partition in the URI matches the tenant ID in the JWT token.
	 *
	 * @param partitionName the partition name from the URI
	 * @param principal     the Principal
	 * @return true if valid, otherwise false
	 */
	public static boolean isPartitionValid(String partitionName, OAuth2AuthenticatedPrincipal principal) {
		String tenantId = principal.getAttribute(Constants.TENANT_ID_CLAIM);

		Object scopeClaim = principal.getAttribute(Constants.FHIR_SCOPES_CLAIM);
		List<String> scopes = Collections.emptyList();

		if (scopeClaim instanceof Collection<?> collection) {
			scopes = collection.stream()
				.filter(Objects::nonNull)
				.map(Object::toString)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		}
		LGR.info("Validating partition. Tenant ID: " + tenantId + ", Partition Name: " + partitionName + ", Scopes: " + scopes);
		return StringUtils.hasText(partitionName)
			&& (Constants.DEFAULT_PARTITION.equals(partitionName)
			|| Objects.equals(partitionName, tenantId)
			|| scopes.contains(partitionName));
	}

	/**
	 * Validates that all values in the given parameter string exist in the list of valid scopes.
	 *
	 * @param typeParamValue   The extracted value of the "_type" query parameter (comma-separated).
	 * @param allowedResources The list of valid FHIR resource scopes.
	 * @return true if all values exist in validScopes, false otherwise.
	 */
	public static boolean validateTypeParamValues(String typeParamValue, List<String> allowedResources) {
		if (typeParamValue == null || typeParamValue.trim().isEmpty()) {
			return false;
		}

		Set<String> allowedResourcesSet = new HashSet<>(allowedResources);

		Set<String> requestedResources = Arrays.stream(typeParamValue.split(","))
			.map(String::trim)
			.collect(Collectors.toSet());

		return allowedResourcesSet.containsAll(requestedResources);
	}

	/**
	 * Determines whether the given URI represents a SMART on FHIR configuration request.
	 *
	 * @param uri the request URI (e.g., /fhir/CM0FW/.well-known/smart-configuration)
	 * @return true if the URI matches the SMART config endpoint pattern, false otherwise
	 */
	public static boolean isSmartConfigurationRequest(String uri) {
		return uri.matches(Constants.SMART_CONFIG_URL_REGEX) ||
			Constants.SMART_CONFIG_URL.equals(uri);
	}

	/**
	 * Determines whether the incoming HTTP request is a HAPI FHIR POST-based search operation.
	 *
	 * @param request the incoming {@link HttpServletRequest}
	 * @return {@code true} if the request is a POST search request (POST .../_search), otherwise {@code false}
	 */
	public static boolean isPostSearchRequest(HttpServletRequest request) {

		// Must be POST method for HAPI FHIR POST-based search
		if (!Constants.POST.equalsIgnoreCase(request.getMethod())) {
			return false;
		}

		String uri = request.getRequestURI();

		// Normalize URI: remove trailing slashes and strip query params
		uri = uri.replaceAll("/+$", "").split("\\?")[0];

		// Extract the last segment of the URI
		String[] segments = uri.split("/");
		String lastSegment = segments.length > 0 ? segments[segments.length - 1] : "";

		boolean isPostSearch = Constants._SEARCH.equalsIgnoreCase(lastSegment);

		LGR.info("Is POST search request? : " + isPostSearch);

		return isPostSearch;
	}


	public static boolean isCategoryGranularScope(String scope) {
		if (scope == null || scope.isBlank()) {
			return false;
		}

		String s = scope.trim()
			.replace("\"", "")
			.replace(",", "");

		s = java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);

		Matcher m = Constants.CATEGORY_SCOPE_PATTERN.matcher(s);
		return m.matches();
	}

	public static boolean isClientToken(OAuth2AuthenticatedPrincipal principal) {
		if (principal == null) {
			return false;
		}

		Object clientId = principal.getAttribute("client_id");
		if (clientId != null && !clientId.toString().isBlank()) {
			return true;
		}

		Object preferredUsername = principal.getAttribute("preferred_username");
        return preferredUsername != null && preferredUsername.toString().startsWith("service-account");

		// Fallback (rare, but safe)
    }

	public static String extractUsername(OAuth2AuthenticatedPrincipal principal) {
		if (principal == null) {
			return null;
		}

		Object preferredUsername = principal.getAttribute("preferred_username");
		if (preferredUsername != null) {
			return preferredUsername.toString();
		}

		Object email = principal.getAttribute("email");
		if (email != null) {
			return email.toString();
		}

		Object username = principal.getAttribute("username");
		if (username != null) {
			return username.toString();
		}

		// Fallback (usually client-id or subject)
		return principal.getName();
	}

	public static String extractUserId(OAuth2AuthenticatedPrincipal principal) {
		if (principal == null) {
			return null;
		}

		if (isClientToken(principal)){
			return extractUsername(principal);
		}

		Object sub = principal.getAttribute("sub");
		if (sub != null) {
			return sub.toString();
		}

		// Fallback (rare, but safe)
		return principal.getName();
	}

	public static String extractClientId(OAuth2AuthenticatedPrincipal principal) {
		if (principal == null) {
			return null;
		}

		Object clientId = principal.getAttribute("client_id");
		if (clientId != null) {
			return clientId.toString();
		}

		Object azp = principal.getAttribute("azp");
		if (azp != null) {
			return azp.toString();
		}

		Object issuedFor = principal.getAttribute("issued_for");
		if (issuedFor != null) {
			return issuedFor.toString();
		}

		/*
		 * 4) Fallback – principal name
		 *    (usually subject, but better than null)
		 */
		return principal.getName();
	}



}