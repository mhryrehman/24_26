package com.keycloak.health;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.util.LinkedHashSet;
import java.util.Set;

public class HealthCheckEndpoint {

    private static final Logger LOG = Logger.getLogger(HealthCheckEndpoint.class);
    private final KeycloakSession session;

    public HealthCheckEndpoint(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck(@QueryParam("checks") String checksCsv) {
        final Set<String> allowed = java.util.Set.of("password", "rpt", "exchange");

        // Parse CSV → requested checks
        Set<String> requested = parseCsv(checksCsv);
        if (requested.isEmpty() || requested.contains("all")) requested = allowed;

        // Validate
        Set<String> invalid = new LinkedHashSet<>(requested);
        invalid.removeAll(allowed);
        if (!invalid.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Unknown check(s): " + String.join(", ", invalid))
                    .build();
        }

        // Run only what’s needed; fetch password token once if required
        final boolean needPassword = requested.contains("password") || requested.contains("rpt");
        final String accessToken = needPassword ? obtainUserAccessTokenViaPasswordGrant() : null;

        HealthCheckResponse result = new HealthCheckResponse();
        if (requested.contains("password")) result.setPasswordGrant(accessToken != null);
        if (requested.contains("rpt")) result.setRpt(requestRptUsingUma(accessToken));
        if (requested.contains("exchange")) result.setTokenExchange(checkTokenExchange());

        // Status only depends on requested subset
        boolean ok =
                (!requested.contains("password") || result.isPasswordGrant()) &&
                        (!requested.contains("rpt") || result.isRpt()) &&
                        (!requested.contains("exchange") || result.isTokenExchange());

        return ok ? Response.ok(result).build()
                : Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(result).build();
    }

    private static java.util.Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return java.util.Collections.emptySet();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }


    private String obtainUserAccessTokenViaPasswordGrant() {
        final String tokenUrl = ConfigLoader.keycloakTokenUri();
        final String clientId = ConfigLoader.clientId();
        final String clientSecret = ConfigLoader.clientSecret();
        final String username = ConfigLoader.testUsername();
        final String password = ConfigLoader.testPassword();

        if (isBlank(tokenUrl) || isBlank(clientId) || isBlank(username) || isBlank(password)) {
            LOG.warn("Password grant skipped due to missing configuration.");
            return null;
        }

        try {
            JsonNode json = SimpleHttp
                    .doPost(tokenUrl, session)
                    .param("grant_type", "password")
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret)
                    .param("username", username)
                    .param("password", password)
                    .asJson();

            final String tok = json.path("access_token").asText(null);
            if (tok != null) {
                LOG.info("Password grant successful (token masked).");
            } else {
                LOG.errorf("Password grant failed: %s", json.toString());
            }
            return tok;
        } catch (Exception e) {
            LOG.error("Password grant request failed", e);
            return null;
        }
    }

    private boolean requestRptUsingUma(String userAccessToken) {
        if (isBlank(userAccessToken)) return false;

        final String tokenUrl = ConfigLoader.keycloakTokenUri();
        final String clientId = ConfigLoader.clientId();
        if (isBlank(tokenUrl) || isBlank(clientId)) {
            LOG.warn("RPT check skipped due to missing configuration.");
            return false;
        }

        try {
            JsonNode json = SimpleHttp
                    .doPost(tokenUrl, session)
                    .header("Authorization", "Bearer " + userAccessToken)
                    .param("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket")
                    .param("audience", clientId)
                    // Add explicit permissions if your policies require:
                    // .param("permission", "resource-id#scope")
                    .asJson();

            final boolean ok = json.hasNonNull("access_token") && !isBlank(json.get("access_token").asText());
            if (ok) {
                LOG.info("UMA RPT issued (token masked).");
            } else {
                LOG.errorf("UMA RPT not issued: %s", json.toString());
            }
            return ok;
        } catch (Exception e) {
            LOG.error("UMA (RPT) request failed", e);
            return false;
        }
    }

    private boolean checkTokenExchange() {
        final String tokenUrl = ConfigLoader.keycloakTokenUri();
        final String clientId = ConfigLoader.clientId();
        final String clientSecret = ConfigLoader.clientSecret();
        final String subjectToken = ConfigLoader.subjectToken();
        final String subjectIssuer = ConfigLoader.subjectIssuer();

        if (isBlank(tokenUrl) || isBlank(clientId) || isBlank(subjectToken)) {
            LOG.warn("Token exchange skipped due to missing configuration.");
            return false;
        }

        try {
            JsonNode json = SimpleHttp
                    .doPost(tokenUrl, session)
                    .param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret)
                    .param("subject_token", subjectToken)
                    .param("subject_issuer", subjectIssuer)
                    .param("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
                    .param("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
                    .asJson();

            final boolean ok = json.hasNonNull("access_token");
            if (!ok) LOG.errorf("Token exchange failed: %s", json.toString());
            return ok;
        } catch (Exception e) {
            LOG.error("Token exchange request failed", e);
            return false;
        }
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
}
