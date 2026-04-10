package com.spi.smart_on_fhir.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.utils.StringUtil;

import javax.annotation.Priority;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author: Yasir Rehman
 * Description:
 * A custom JAX-RS response filter that processes the response from the OpenID Connect token endpoint.
 * This filter intercepts responses to the `/protocol/openid-connect/token` endpoint and modifies
 * the `scope` field in the response based on the token's actual content.
 * <p>
 * The filter is executed after all other filters due to its high priority (`Integer.MAX_VALUE`).
 */
@Provider
@Priority(Integer.MAX_VALUE)
public class TokenEndpointResponseFilter implements ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(TokenEndpointResponseFilter.class.getName());

    /**
     * Intercepts and processes the HTTP response for the token endpoint.
     *
     * @param requestContext  The request context.
     * @param responseContext The response context.
     * @throws IOException if an I/O error occurs during processing.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        try {
            if (requestContext.getUriInfo().getPath().contains("/protocol/openid-connect/token")) {
                LOG.info("TokenEndpointResponseFilter has been called.");
                Map<String, String> formParameters = getFormData(requestContext);
                String grantType = formParameters != null ? formParameters.get("grant_type") : null;
                Object entity = responseContext.getEntity();
                if ("authorization_code".equals(grantType) && entity instanceof AccessTokenResponse accessTokenResponse) {
                    String tokenScopes = extractScopeFromToken(accessTokenResponse.getToken());
                    if (!StringUtil.isNullOrEmpty(tokenScopes)) {
                        LOG.info("Original scope in response body: " + accessTokenResponse.getScope());
                        accessTokenResponse.setScope(tokenScopes);
                        LOG.info("Updated scope in response body: " + accessTokenResponse.getScope());
                        responseContext.setEntity(accessTokenResponse);
                    }
                }

                if (entity instanceof OAuth2ErrorRepresentation errorResponse) {
                    if ("invalid_client".equals(errorResponse.getError())) {
                        LOG.info("Error detected: invalid_client. Changing response status to 400.");
                        responseContext.setStatus(Response.Status.BAD_REQUEST.getStatusCode()); // Set HTTP status to 400
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "An error occurred in the response filter: " + e.getMessage(), e);
        }

    }

    /**
     * Extracts the `scope` field from the given token string.
     * The token is parsed and verified as a JWT using Keycloak's `TokenVerifier`.
     *
     * @param token The access token string.
     * @return The extracted `scope` field value, or {@code null} if extraction fails.
     */
    private String extractScopeFromToken(String token) {
        try {
            if (null != token) {
                AccessToken accessToken = TokenVerifier.create(token, AccessToken.class).getToken();
                LOG.info("Scope extracted from token: " + accessToken.getScope());
                return accessToken.getScope();
            }
        } catch (VerificationException e) {
            LOG.log(Level.SEVERE, "Failed to verify token: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Extracts URL-encoded form data from the request body.
     * <p>
     * Reads the entity stream from the provided {@link ContainerRequestContext},
     * parses the body into key-value pairs, and returns them as a {@link Map}.
     * Consumes the entity stream, so resetting may be required for downstream processing.
     *
     * @param requestContext The incoming HTTP request context.
     * @return A map of form data key-value pairs, or {@code null} if an error occurs.
     */
    private Map<String, String> getFormData(ContainerRequestContext requestContext) {
        try {
            InputStream inputStream = requestContext.getEntityStream();
            String body = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .reduce("", (accumulator, actual) -> accumulator + actual);
            return parseFormData(body);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to extract form data from request body: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parses a URL-encoded form data string into a map.
     *
     * @param body The form data string.
     * @return A map of form data key-value pairs.
     */
    private Map<String, String> parseFormData(String body) {
        Map<String, String> formData = new HashMap<>();
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                formData.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
            }
        }
        return formData;
    }
}
