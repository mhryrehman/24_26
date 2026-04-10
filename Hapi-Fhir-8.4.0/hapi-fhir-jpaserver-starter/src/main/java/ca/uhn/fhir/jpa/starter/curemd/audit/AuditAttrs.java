package ca.uhn.fhir.jpa.starter.curemd.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.List;

public final class AuditAttrs {

	private AuditAttrs() {
	}

	// marker to prevent duplicate writes (filter + interceptor)
	public static final String WRITTEN = "audit.written";
	public static final String START_NANO = "audit.startNano";

	// what we want stored
	public static final String TENANT = "audit.tenant";
	public static final String REQUIRED_SCOPE = "audit.requiredScope";
	public static final String ROLES = "audit.roles";
	public static final String SCOPES = "audit.scopes";
	public static final String USERNAME = "audit.username";
	public static final String USER_ID = "audit.userId";
	public static final String CLIENT_ID = "audit.clientId";

	public static void markStart(HttpServletRequest request) {
		request.setAttribute(START_NANO, System.nanoTime());
	}

	/**
	 * Option A (recommended): your security filter sets these explicitly.
	 * Use this helper to set them in one place.
	 */
	public static void setFromSecurityFilter(HttpServletRequest request, String tenant, String requiredScope, List<String> roles, List<String> scopes, String username, String userId, String clientId) {
		request.setAttribute(TENANT, tenant);
		request.setAttribute(REQUIRED_SCOPE, requiredScope);
		request.setAttribute(ROLES, roles != null ? String.join(",", roles) : null);
		request.setAttribute(SCOPES, scopes != null ? String.join(",", scopes) : null);
		request.setAttribute(USERNAME, username);
		request.setAttribute(USER_ID, userId);
		request.setAttribute(CLIENT_ID, clientId);
	}

	/**
	 * Option B: interceptor/service can try to fetch from SecurityContext if filter didn’t set.
	 * This is best-effort (not all requests guarantee principal type).
	 */
	public static void tryPopulateFromSecurityContextIfMissing(HttpServletRequest request) {
		if (request.getAttribute(USERNAME) != null || request.getAttribute(USER_ID) != null) {
			return;
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) return;
		if (!(auth.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal)) return;

		Object preferredUsername = principal.getAttribute("preferred_username");
		Object sub = principal.getAttribute("sub");

		if (preferredUsername != null) request.setAttribute(USERNAME, preferredUsername.toString());
		if (sub != null) request.setAttribute(USER_ID, sub.toString());
	}
}
