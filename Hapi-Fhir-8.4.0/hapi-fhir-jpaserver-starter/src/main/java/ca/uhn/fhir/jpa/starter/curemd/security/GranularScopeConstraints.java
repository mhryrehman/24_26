package ca.uhn.fhir.jpa.starter.curemd.security;

import org.jboss.logging.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds granular constraints extracted from scope strings like:
 * <p>
 * patient/Condition.rs?category=http://...|encounter-diagnosis
 * Condition.rs?category=http://...|encounter-diagnosis
 * user/Observation.rs?category=http://...|laboratory
 * <p>
 * Stores: resourceType -> paramName -> allowed values (system|code)
 */
public final class GranularScopeConstraints {

	public static final String ATTR_NAME = "granularScopeConstraints";
	private static final Logger LGR = Logger.getLogger(GranularScopeConstraints.class);

	// resourceType -> param -> allowed values (e.g. "http://system|code")
	private final Map<String, Map<String, Set<String>>> constraints = new HashMap<>();
	private final Set<String> globalReadResources = new HashSet<>();

	/**
	 * Matches scopes with ANY optional prefix path before the resource type:
	 * Observation.rs?category=...
	 * patient/Observation.rs?category=...
	 * user/Observation.rs?category=...
	 * system/Observation.rs?category=...
	 * something/else/patient/Observation.rs?category=...
	 */
	private static final Pattern CATEGORY_SCOPE_PATTERN = Pattern.compile("^(?:.*/)?(Condition|Observation)\\.rs\\?category=(.+)$");
	private static final Pattern GLOBAL_RS_PATTERN = Pattern.compile("^(?:.*/)?(\\*|[A-Z][A-Za-z]*)\\.([^.]+)$");





	private GranularScopeConstraints() {
		// use factory method
	}

	public static GranularScopeConstraints fromScopes(List<String> scopes) {
		GranularScopeConstraints out = new GranularScopeConstraints();

		if (scopes == null || scopes.isEmpty()) {
			return out;
		}
		LGR.info("Processing scopes for granular constraints: " + scopes);
		for (String raw : scopes) {
			if (raw == null || raw.isBlank()) {
				continue;
			}

			Matcher mg = GLOBAL_RS_PATTERN.matcher(raw);
			if (mg.matches()) {
				out.globalReadResources.add(mg.group(1));
				continue;
			}

			// Normalize common formats (quotes/commas from string splits), then decode
			String s = raw.trim()
				.replace("\"", "")
				.replace(",", "");

			s = URLDecoder.decode(s, StandardCharsets.UTF_8);

			Matcher m = CATEGORY_SCOPE_PATTERN.matcher(s);
			if (!m.matches()) {
				continue;
			}

			String resourceType = m.group(1); // Condition or Observation
			String token = m.group(2);        // system|code (may contain :// and |)

			// Basic sanity: must look like system|code
			if (!token.contains("|")) {
				continue;
			}

			out.constraints
				.computeIfAbsent(resourceType, k -> new HashMap<>())
				.computeIfAbsent("category", k -> new HashSet<>())
				.add(token);
		}
		LGR.info("Extracted granular scope constraints: " + out.constraints);
		return out;
	}

	public Set<String> allowedCategoryTokens(String resourceType) {
		if (resourceType == null) {
			return Collections.emptySet();
		}
		return Optional.ofNullable(constraints.get(resourceType))
			.map(m -> m.get("category"))
			.orElse(Collections.emptySet());
	}

	public boolean hasCategoryConstraints(String resourceType) {
		return !allowedCategoryTokens(resourceType).isEmpty();
	}

	public boolean hasGlobalRead(String resourceType) {
		LGR.info("Checking global read for resourceType=" + resourceType + ": " + globalReadResources);
		return globalReadResources.contains(resourceType) || globalReadResources.contains("*");
	}


	// Optional helper for debugging
	@Override
	public String toString() {
		return "GranularScopeConstraints{" + "constraints=" + constraints + '}';
	}
}
