package com.spi.smart_on_fhir.authentication;

import com.spi.smart_on_fhir.factory.AudienceValidatorFactory;
import com.spi.smart_on_fhir.utils.Constants;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import jakarta.ws.rs.core.Response;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Author: Yasir Rehman
 * Description:
 * Validate an incoming "aud" query parameter against a configured list of acceptable audiences.
 */
public class AudienceValidator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(AudienceValidator.class);

    public AudienceValidator(KeycloakSession session) {
        // NOOP
    }

    /**
     * Authenticates the request by validating the "aud" parameter against the allowed audiences.
     *
     * @param context the authentication flow context
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {

        String launchParameter = getParam(context, Constants.SMART_LAUNCH_PARAM);   //launch=patient:3352
        LOG.info("LAUNCH parameters is : " + launchParameter);
        if (!StringUtil.isNullOrEmpty(launchParameter)) {
            context.getAuthenticationSession().setUserSessionNote(Constants.PATIENT_ID_NOTE, launchParameter);
            LOG.info("setting launch value in user session note");
        }

        boolean validateAlways = Boolean.parseBoolean(context.getAuthenticatorConfig().getConfig().getOrDefault(AudienceValidatorFactory.VALIDATE_ALWAYS_PROP_NAME, "false"));
        String requestedAudience = getParam(context, Constants.SMART_AUDIENCE_PARAM);
        LOG.info("all query parameters are : " + context.getUriInfo().getQueryParameters());

        if (!validateAlways && (requestedAudience == null || requestedAudience.trim().isEmpty())) {
            LOG.info("No 'aud' parameter found in the request and 'validateAlways' is false. Skipping audience validation.");
            context.success();
            return;
        }

        if (context.getAuthenticatorConfig() == null ||
                !context.getAuthenticatorConfig().getConfig().containsKey(AudienceValidatorFactory.AUDIENCES_PROP_NAME)) {
            String msg = "The Keycloak Audience Validation Extension must be configured with one or more allowed audiences";
            context.failure(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED,
                    Response.status(302)
                            .header("Location", context.getAuthenticationSession().getRedirectUri() +
                                    "?error=server_error" +
                                    "&error_description=" + msg)
                            .build());
            return;  // early exit
        }

        String audiencesString = context.getAuthenticatorConfig().getConfig().get(AudienceValidatorFactory.AUDIENCES_PROP_NAME);
        LOG.info("Requested audience: " + requestedAudience);
        LOG.info("Allowed audiences: " + audiencesString);

        List<String> audiences = Arrays.asList(audiencesString.split("##"));
        if (audiences.contains(requestedAudience)) {
            context.success();
        } else {
            String msg = "Requested audience '" + requestedAudience +
                    "' must match one of the configured Resource Server URLs: " + audiences;
            context.failure(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED,
                    Response.status(302)
                            .header("Location", context.getAuthenticationSession().getRedirectUri() +
                                    "?error=invalid_request" +
                                    "&error_description=" + msg)
                            .build());
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    private static String getParam(AuthenticationFlowContext ctx, String name) {
        // 1) query string
        String v = ctx.getUriInfo().getQueryParameters().getFirst(name);
        if (!StringUtil.isBlank(v)) return v;
        // 2) form-encoded body (authorize POST)
        MultivaluedMap<String, String> form = safeFormParams(ctx);
        return form != null ? form.getFirst(name) : null;
    }

    private static MultivaluedMap<String, String> safeFormParams(AuthenticationFlowContext ctx) {
        try {
            return ctx.getHttpRequest() != null ? ctx.getHttpRequest().getDecodedFormParameters() : null;
        } catch (Exception e) {
            // if request is GET or not form-encoded, this can be empty; log at debug level
            LOG.info("No form parameters available (likely GET or non-form content).", e);
            return null;
        }
    }
}
