package com.spi.smart_on_fhir.authentication;

import com.spi.smart_on_fhir.factory.PatientSelectionFormFactory;
import com.spi.smart_on_fhir.models.PatientDTO;
import com.spi.smart_on_fhir.service.ExternalApiService;
import com.spi.smart_on_fhir.utils.Constants;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Author: Yasir Rehman
 * Description:
 * Present a patient context picker when the client requests the launch/patient scope and the
 * * user record has multiple resourceId attributes. The selection is stored in a UserSessionNote
 * * with name "patient_id".
 */
public class PatientSelectionForm implements Authenticator {

    private static final Logger LOG = Logger.getLogger(PatientSelectionForm.class);

    /**
     * Authenticates the user by presenting a patient selection form if the user has multiple
     * resourceId attributes. If only one patient is associated, the process completes without
     * requiring user input.
     *
     * @param context the authentication flow context
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        LOG.info("PatientSelectionForm's authenticate method called.");
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        ClientModel client = authSession.getClient();
        String requestedScopesString = authSession.getClientNote(OIDCLoginProtocol.SCOPE_PARAM);
        Stream<ClientScopeModel> clientScopes = TokenManager.getRequestedClientScopes(context.getSession(), requestedScopesString, client, authSession.getAuthenticatedUser());

        if (clientScopes.noneMatch(s -> Constants.SMART_SCOPE_LAUNCH_PATIENT.equals(s.getName()))) {
            // no launch/patient scope == no-op
            context.success();
            return;
        }

        if (context.getUser() == null) {
            fail(context, "Expected a user but found null");
            return;
        }

        List<String> resourceIds = getResourceIdsForUser(context);
        LOG.info("ResourceId found in user attributes : " + resourceIds);

        if (resourceIds.isEmpty()) {
            fail(context, "Expected user to have one or more resourceId attributes, but found none");
            return;
        }
        if (resourceIds.size() == 1) {
            succeed(context, resourceIds.get(0));
            return;
        }
        //validate if all required configurations present.
        if (!validateConfig(context)) return;

        List<PatientDTO> patients = null;
        try {
            ExternalApiService service = new ExternalApiService();
            Map<String, String> cfg = context.getAuthenticatorConfig().getConfig();
            String accessToken = service.ensureToken(context.getSession(),
                    cfg.get(PatientSelectionFormFactory.TOKEN_ENDPOINT_URL_PROP_NAME),
                    cfg.get(PatientSelectionFormFactory.CLIENT_ID_PROP_NAME),
                    cfg.get(PatientSelectionFormFactory.CLIENT_SECRET_PROP_NAME));
            LOG.debug("Access token obtained: " + accessToken);

            if (accessToken == null || accessToken.isEmpty()) {
                LOG.error("Could not obtain access token from configured token endpoint");
                fail(context, "Could not obtain access token from configured token endpoint");
                return;
            }

            String fhirUrl = buildFhirUrl(context.getUser(), cfg.get(PatientSelectionFormFactory.INTERNAL_FHIR_URL_PROP_NAME));
            patients = service.fetchAllPatients(context.getSession(),
                    resourceIds, accessToken,
                    fhirUrl);
            LOG.info("total patients fetched: " + patients.size());
        } catch (Exception e) {
            LOG.error("Error fetching patient data", e);
            succeed(context, resourceIds.get(0));
            return;
        }

        if (patients.isEmpty()) {
            succeed(context, resourceIds.get(0));
            return;
        }

        if (patients.size() == 1) {
            succeed(context, patients.get(0).getId());
        } else {
            Response response = context.form().setAttribute("patients", patients).createForm("patient-select-form.ftl");

            context.challenge(response);
        }

    }

    /**
     * Retrieves the resource IDs associated with the authenticated user.
     *
     * @param context the authentication flow context
     * @return a list of resource IDs
     */
    private List<String> getResourceIdsForUser(AuthenticationFlowContext context) {
        return context.getUser().getAttributeStream(Constants.ATTRIBUTE_RESOURCE_ID).flatMap(a -> Arrays.stream(a.split(" "))).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    /**
     * Marks the authentication flow as failed and sends an error response.
     *
     * @param context the authentication flow context
     * @param msg     the error message
     */
    private void fail(AuthenticationFlowContext context, String msg) {
        LOG.error(msg);
        context.failure(AuthenticationFlowError.INTERNAL_ERROR, Response.status(302).header("Location", context.getAuthenticationSession().getRedirectUri() + "?error=server_error" + "&error_description=" + msg).build());
    }

    /**
     * Marks the authentication flow as successful and sets the selected patient ID.
     *
     * @param context the authentication flow context
     * @param patient the selected patient ID
     */
    private void succeed(AuthenticationFlowContext context, String patient) {
        // Add selected information to authentication session
        LOG.info("patient selected : " + patient);
        context.getAuthenticationSession().setUserSessionNote(Constants.PATIENT_ID_NOTE, patient);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    /**
     * Handles form submissions in the authentication flow.
     *
     * @param context the authentication flow context
     */
    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String patient = formData.getFirst("patient");
        LOG.info("User selected patient " + patient);
        if (patient == null || patient.trim().isEmpty() || !getResourceIdsForUser(context).contains(patient.trim())) {
            LOG.warnf("The patient selection '%s' is not valid for the authenticated user.", patient);
            context.cancelLogin();
            // reauthenticate...
            authenticate(context);
            return;
        }
        succeed(context, patient.trim());
    }

    @Override
    public void close() {
    }

    private boolean validateConfig(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        if (config == null || config.getConfig() == null) {
            fail(context, "Missing authenticator configuration.");
            return false;
        }

        Map<String, String> cfg = config.getConfig();

        List<String> requiredKeys = List.of(PatientSelectionFormFactory.INTERNAL_FHIR_URL_PROP_NAME, PatientSelectionFormFactory.CLIENT_ID_PROP_NAME, PatientSelectionFormFactory.CLIENT_SECRET_PROP_NAME, PatientSelectionFormFactory.TOKEN_ENDPOINT_URL_PROP_NAME);

        for (String key : requiredKeys) {
            String value = cfg.get(key);
            if (value == null || value.isBlank()) {
                fail(context, "Missing or blank configuration: " + key);
                return false;
            }
        }

        return true;
    }

    /**
     * Builds FHIR base URL. If a tenantId attribute exists on the user, it appends it to the base FHIR URL.
     *
     * @param user    the Keycloak user model
     * @param baseUrl the configured internal FHIR base URL
     * @return the tenant-specific FHIR URL
     */
    public static String buildFhirUrl(UserModel user, String baseUrl) {
        if (user == null || baseUrl == null) {
            return baseUrl;
        }

        String tenantId = user.getFirstAttribute(Constants.ATTRIBUTE_TENANT_ID);

        if (StringUtil.isNotBlank(tenantId)) {
            LOG.info("Using tenant ID: " + tenantId);
            return baseUrl.endsWith("/") ? baseUrl + tenantId : baseUrl + "/" + tenantId;
        }

        return baseUrl;
    }

}
