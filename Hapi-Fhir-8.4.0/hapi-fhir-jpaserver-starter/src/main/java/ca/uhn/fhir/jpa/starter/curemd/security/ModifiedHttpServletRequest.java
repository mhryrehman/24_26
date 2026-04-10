package ca.uhn.fhir.jpa.starter.curemd.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.logging.Logger;

/**
 * Author: Yasir Rehman
 * Description:
 * A custom HttpServletRequestWrapper that modifies the query string
 * to append the "_type" parameter with valid FHIR resource types.
 */
public class ModifiedHttpServletRequest extends HttpServletRequestWrapper {
	private static final Logger LGR = Logger.getLogger(ModifiedHttpServletRequest.class.getName());

	private static final String TYPE_QUERY_PARAM = "_type=";
	private final String allowedTypeParam;
	private String cachedQueryString;

	/**
	 * Constructs a ModifiedRequest by wrapping the original HttpServletRequest
	 * and storing a list of FHIR scopes for processing.
	 *
	 * @param request The original HttpServletRequest.
	 * @param allowedTypeParam  The list of requested FHIR scopes.
	 */
	public ModifiedHttpServletRequest(HttpServletRequest request, String allowedTypeParam) {
		super(request);
		this.allowedTypeParam = allowedTypeParam;
	}

	/**
	 * Modifies the query string by appending the "_type" parameter with
	 * a list of valid FHIR resource types if it is not already present.
	 *
	 * @return The modified query string including the "_type" parameter if necessary.
	 */
	@Override
	public String getQueryString() {
		if (cachedQueryString != null) {
			LGR.info("returning cachedQueryString : " + cachedQueryString);
			return cachedQueryString; // Return cached result if already modified
		}
		String originalQuery = super.getQueryString();
		LGR.info("allowed typeParams are : " + allowedTypeParam);

		if ((originalQuery != null && originalQuery.contains(TYPE_QUERY_PARAM)) || allowedTypeParam.isEmpty()) {
			cachedQueryString = originalQuery;
			return originalQuery;
		}

		StringBuilder modifiedQuery = new StringBuilder();
		if (originalQuery != null) {
			modifiedQuery.append(originalQuery).append("&");
		}
		modifiedQuery.append(TYPE_QUERY_PARAM).append(allowedTypeParam);
		LGR.info("modifiedQuery is : " + modifiedQuery);
		cachedQueryString = modifiedQuery.toString();

		return cachedQueryString;
	}

	/**
	 * Returns the original request URI without any modifications.
	 *
	 * @return The original request URI.
	 */
	@Override
	public String getRequestURI() {
		return super.getRequestURI();
	}
}
