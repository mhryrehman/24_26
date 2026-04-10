package com.spi.smart_on_fhir.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.spi.smart_on_fhir.authentication.PatientSelectionForm;
import com.spi.smart_on_fhir.models.PatientDTO;
import com.spi.smart_on_fhir.utils.Constants;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ExternalApiService {

    private static final Logger LOG = Logger.getLogger(PatientSelectionForm.class);

    // cached token
    private static volatile String cachedToken;
    private static final AtomicLong tokenExpiryEpochSec = new AtomicLong(0);

    public String ensureToken(KeycloakSession session, String url, String clientId, String clientSecret) throws Exception {
        long now = Instant.now().getEpochSecond();
        if (cachedToken == null || now >= tokenExpiryEpochSec.get()) {
            LOG.info("Token missing or expired, refreshing…");
            return refreshToken(session, url, clientId, clientSecret);
        }
        return cachedToken;
    }

    public String refreshToken(KeycloakSession session, String url, String clientId, String clientSecret) throws Exception {
        try {
            // Build request
            SimpleHttp request = SimpleHttp.doPost(url, session)
                    .param("grant_type", "client_credentials")
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret)
                    .connectionRequestTimeoutMillis(10000);

            // Execute and ensure response is always closed
            try (SimpleHttp.Response response = request.asResponse()) {
                int status = response.getStatus();

                if (status != 200) {
                    // Read body once for logging/exception
                    String body = response.asString();
                    LOG.errorf("Failed to get access token from %s: HTTP %d - %s", url, status, body);
                    return null;
                }

                JsonNode json = response.asJson();
                if (json == null || json.get("access_token") == null || json.get("access_token").isNull()) {
                    String body = response.asString();
                    LOG.errorf("No access_token in response from %s. Body: %s", url, body);
                    return null;
                }

                String token = json.get("access_token").asText();
                long expiresIn = json.has("expires_in") ? json.get("expires_in").asLong() : 60L;
                tokenExpiryEpochSec.set(Instant.now().getEpochSecond() + expiresIn - 60); // refresh 60s early
                cachedToken = token;
                return token;
            }

        } catch (Exception e) {
            LOG.warnf("Exception while fetching access token from %s: %s", url, e.getMessage());
            throw e;
        }
    }

    /**
     * Fetches multiple patients in a single FHIR API call, requesting only ID, name and birthdate fields.
     *
     * @param patientIds  List of patient IDs to fetch (required, minimum 1 ID)
     * @param accessToken Valid OAuth2 bearer token for FHIR API access
     * @return List of parsed patient data objects
     * @throws Exception if request fails (network error, invalid response, or non-200 status code)
     */
    public List<PatientDTO> fetchAllPatients(KeycloakSession session, List<String> patientIds, String accessToken, String fhirBaseUrl) throws Exception {
        String idList = String.join(",", patientIds);
        String searchUrl = String.format("%s/Patient?_id=%s&_elements=%s&_count=%d",
                fhirBaseUrl, idList, Constants.REQUIRED_FIELDS, patientIds.size());

        LOG.info("Fetching all patients with URL: " + searchUrl);
        if (patientIds.isEmpty()) {
            LOG.info("No patient IDs provided; returning empty list.");
            return Collections.emptyList();
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken is null/blank");
        }
        if (fhirBaseUrl == null || fhirBaseUrl.isBlank()) {
            throw new IllegalArgumentException("fhirBaseUrl is null/blank");
        }

        SimpleHttp request = SimpleHttp.doGet(searchUrl, session)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/fhir+json")
                .connectionRequestTimeoutMillis(10000);


        try (SimpleHttp.Response response = request.asResponse()) {
            int status = response.getStatus();
            if (status != 200) {
                LOG.errorf("Failed to fetch patients. HTTP status: %d, Body: %s", status, response.asString());
                throw new Exception("Failed to fetch patients. HTTP status: " + status);
            }
            LOG.infof("Successfully fetched patients. HTTP status: %d", status);
            LOG.debug("Response Body: " + response.asString());

            return parseBundleResponse(response.asJson());
        } catch (Exception e) {
            LOG.errorf("Exception while fetching patients: %s", e.getMessage());
            throw e;
        }

    }

    /**
     * Parses a FHIR Bundle response into a list of {@link PatientDTO} objects.
     *
     * @param bundleNode the JSON string representing a FHIR Bundle
     * @return a list of {@link PatientDTO} objects
     */
    private List<PatientDTO> parseBundleResponse(JsonNode bundleNode) {
//        JsonNode bundleNode = objectMapper.readTree(bundleJson);
        JsonNode entries = bundleNode.path("entry");

        List<PatientDTO> patients = new ArrayList<>();

        if (entries.isArray()) {
            for (JsonNode entry : entries) {
                JsonNode resource = entry.path("resource");
                patients.add(parsePatient(resource));
            }
        }

        return patients;
    }

    /**
     * Parses an individual Patient resource JSON node into a {@link PatientDTO} object.
     *
     * @param patientNode the JSON node representing a single FHIR Patient resource
     * @return a {@link PatientDTO} containing the parsed patient data
     */
    private PatientDTO parsePatient(JsonNode patientNode) {
        String id = patientNode.path("id").asText();

        JsonNode nameNode = patientNode.path("name");
        String fullName = "";
        if (nameNode.isArray() && !nameNode.isEmpty()) {
            JsonNode firstNameEntry = nameNode.get(0);
            String familyName = firstNameEntry.path("family").asText();
            String givenName = firstNameEntry.path("given").isArray()
                    ? firstNameEntry.path("given").get(0).asText()
                    : "";
            fullName = givenName + " " + familyName;
        }

        String dateOfBirth = patientNode.path("birthDate").asText();
        return new PatientDTO(id, fullName, dateOfBirth);
    }
}
