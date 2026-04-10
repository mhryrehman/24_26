package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.curemd.security.GranularScopeConstraints;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.rest.server.util.ICachedSearchDetails;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
@Interceptor
public class GranularScopeEnforcerInterceptor {
	private static final Logger LGR = Logger.getLogger(GranularScopeEnforcerInterceptor.class);

	/**
	 * This pointcut provides SearchParameterMap for JPA searches.
	 * It runs before the query executes, so we can validate/modify it.
	 */
	@Hook(Pointcut.STORAGE_PRESEARCH_REGISTERED)
	public void preCheckForCachedSearch(
		ICachedSearchDetails theSearchDetails,
		RequestDetails requestDetails,
		ServletRequestDetails servletRequestDetails,
		SearchParameterMap map,
		RequestPartitionId theRequestPartitionId
	) {
		LGR.info("Granular scope enforcement interceptor invoked");
		String resourceType = requestDetails.getResourceName();
		if (resourceType == null) return;

		Object attr = servletRequestDetails.getServletRequest().getAttribute(GranularScopeConstraints.ATTR_NAME);
		if (!(attr instanceof GranularScopeConstraints gsc)) {
			return;
		}

		Set<String> allowed = gsc.allowedCategoryTokens(resourceType);
		LGR.infof("Granular scope enforcement: resource=%s allowed=%s", resourceType, allowed);

		if (gsc.hasGlobalRead(resourceType)) {
			LGR.info("Global read scope present; skipping category enforcement");
			return;
		}

		if (allowed == null || allowed.isEmpty()) {
			throw new ForbiddenOperationException("Access denied: token does not grant any category-level access for " + resourceType);
		}

		// Grab existing category params
		List<List<IQueryParameterType>> existing = map.get("category");

		// If category provided -> validate
		if (existing != null && !existing.isEmpty()) {
			Set<String> requested = new HashSet<>();

			for (List<IQueryParameterType> orList : existing) {
				for (IQueryParameterType p : orList) {

					String req = normalizeToken(p, resourceType);
					requested.add(req);

					// requested ⊆ allowed
					if (!isRequestedAllowed(req, allowed)) {
						throw new ForbiddenOperationException("Requested category not permitted by granular scopes for " + resourceType);
					}
				}
			}
			return;
		}

		// If category NOT provided -> inject allowed categories (OR list)
		List<IQueryParameterType> enforcedOrList = new ArrayList<>();
		for (String token : allowed) {
			String[] parts = token.split("\\|", 2);
			String system = parts[0];
			String code = parts.length > 1 ? parts[1] : null;
			enforcedOrList.add(new TokenParam(system, code));
		}
		LGR.info("Injecting enforced category param for " + resourceType + ": " + enforcedOrList);
		map.put("category", Collections.singletonList(enforcedOrList));
	}

	/**
	 * OUTGOING enforcement - invoked for READ / VREAD and also for search result rendering.
	 * This is the hook you need for /Condition/{id} or /Observation/{id}.
	 */
	@Hook(Pointcut.STORAGE_PRESHOW_RESOURCES)
	public void preShowEnforceCategory(
		IPreResourceShowDetails preShow,
		RequestDetails requestDetails,
		ServletRequestDetails servletRequestDetails
	) {
		LGR.info("Granular scope pre-show enforcement interceptor invoked");
		String resourceType = requestDetails.getResourceName();
		if (resourceType == null) return;

		GranularScopeConstraints gsc = getGsc(servletRequestDetails);
		if (gsc == null) return;

		LGR.info("going to check global access.");
		if (gsc.hasGlobalRead(resourceType)) {
			LGR.info("Global read scope present; skipping category enforcement");
			return;
		}

		RestOperationTypeEnum op = requestDetails.getRestOperationType();

		// READ/VREAD -> strict (403 if not allowed)
		if (op == RestOperationTypeEnum.READ || op == RestOperationTypeEnum.VREAD) {
			for (int i = 0; i < preShow.size(); i++) {
				IBaseResource r = preShow.getResource(i);
				if (r == null) continue;
				enforceSingleOrThrow(gsc, r, requestDetails);
			}
		}
	}

	private void enforceSingleOrThrow(GranularScopeConstraints gsc, IBaseResource r, RequestDetails requestDetails) {
		String resourceType = r.fhirType();

		if (gsc.hasGlobalRead(resourceType)) return;

		Set<String> allowed = gsc.allowedCategoryTokens(resourceType);
		LGR.info("Granular scope enforcement for single resource: resource=" + resourceType + " allowed=" + allowed);
		if (allowed == null || allowed.isEmpty()) {
			throw new ForbiddenOperationException("Access denied: token does not grant any category-level access for " + resourceType);
		}

		Set<String> resourceTokens = extractCategoryTokens(r, requestDetails.getFhirContext());
		LGR.info("Resource category tokens: " + resourceTokens);

		// Secure default: if token needs category-level access but resource has none -> deny
		if (resourceTokens.isEmpty()) {
			throw new ForbiddenOperationException("Access denied: " + resourceType + " has no category; granular category scope required");
		}

		if (!anyMatch(allowed, resourceTokens)) {
			throw new ForbiddenOperationException("Access denied: " + resourceType + " category not permitted by granular scopes");
		}
	}

	private boolean anyMatch(Set<String> allowed, Set<String> resourceTokens) {
		for (String t : resourceTokens) {
			if (allowed.contains(t)) return true;
		}
		return false;
	}

	/**
	 * Generic extraction: category.coding -> system|code tokens
	 * Works for Observation, Condition, and any resource that has category as CodeableConcept.
	 */
	private Set<String> extractCategoryTokens(IBaseResource resource, FhirContext fhirContext) {
		Set<String> tokens = new HashSet<>();
		IFhirPath fp = fhirContext.newFhirPath();

		// Returns Coding nodes as IBaseCoding in HAPI
		List<IBase> codings = fp.evaluate(resource, "category.coding", IBase.class);
		for (IBase b : codings) {
			if (!(b instanceof IBaseCoding coding)) continue;

			String system = coding.getSystem();
			String code = coding.getCode();
			if (system == null || system.isBlank()) continue;
			if (code == null || code.isBlank()) continue;

			tokens.add(system + "|" + code);
		}
		return tokens;
	}

	private GranularScopeConstraints getGsc(ServletRequestDetails servletRequestDetails) {
		Object attr = servletRequestDetails.getServletRequest().getAttribute(GranularScopeConstraints.ATTR_NAME);
		return (attr instanceof GranularScopeConstraints gsc) ? gsc : null;
	}

	@NotNull
	private static String normalizeToken(IQueryParameterType p, String resourceType) {
		if (!(p instanceof TokenParam tp)) {
			throw new ForbiddenOperationException("Invalid category parameter format for " + resourceType);
		}

		String system = tp.getSystem();
		String code = tp.getValue();

		if (code == null || code.isBlank()) {
			throw new ForbiddenOperationException("category must include a code for " + resourceType);
		}

		// FHIR token search allows code-only => treat as "|code"
		if (!StringUtils.hasText(system)) {
			return "|" + code;
		}

		return system + "|" + code;
	}

	private static boolean isRequestedAllowed(String requestedToken, Set<String> allowed) {
		// requestedToken is either "system|code" or "|code"
		if (!requestedToken.startsWith("|")) {
			// system+code must match exactly
			return allowed.contains(requestedToken);
		}

		String reqCode = requestedToken.substring(1);
		for (String a : allowed) {
			int idx = a.indexOf('|');
			if (idx > -1) {
				String allowedCode = a.substring(idx + 1);
				if (reqCode.equals(allowedCode)) return true;
			}
		}
		return false;
	}

}