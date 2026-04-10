package com.keycloak.otp.auth.resetcredentials;

import com.keycloak.otp.service.LeapIntegrationService;
import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.Utils;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PasswordPolicyNotMetException;
import org.keycloak.services.messages.Messages;

public class PasswordResetNoSessionAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(PasswordResetNoSessionAuthenticator.class);

    // Field names used by login-update-password.ftl
    private static final String FIELD_PASS    = "password-new";
    private static final String FIELD_CONFIRM = "password-confirm";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        LOG.info("PasswordUpdateSignOutAuthenticator.authenticate()");
        context.challenge(context.form().createForm("login-update-password.ftl"));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        LeapIntegrationService uals = new LeapIntegrationService(context.getSession());
        uals.logActivity(context.getAuthenticationSession(), "PASSWORD_UPDATED", context.getFlowPath());

        final MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();
        final String pass    = trim(form.getFirst(FIELD_PASS));
        final String confirm = trim(form.getFirst(FIELD_CONFIRM));

        if (isBlank(pass)) {
            challengeWithError(context, FIELD_PASS, "missingPasswordMessage");
            return;
        }
        if (!pass.equals(confirm)) {
            challengeWithError(context, FIELD_CONFIRM, "passwordConfirmError");
            return;
        }

        final UserModel user = context.getUser();
        final RealmModel realm = context.getRealm();

        try {
            LOG.infof("kcId=%s Updating password for user: " + user.getUsername(), user.getId());
            context.getSession()
                    .getProvider(PasswordPolicyManagerProvider.class)
                    .validate(realm, user, pass);

            user.credentialManager().updateCredential(UserCredentialModel.password(pass, false));
            user.setSingleAttribute(UserModel.LOCALE, form.getFirst(Constants.KC_LOCALE));

            // Emit audit event
            context.getEvent()
                    .event(EventType.UPDATE_PASSWORD)
                    .user(user)
                    .detail(Details.USERNAME, user.getUsername())
                    .success();

            context.forkWithSuccessMessage(new FormMessage(Messages.ACCOUNT_UPDATED));

        } catch (PasswordPolicyNotMetException ppe) {
            LOG.info("Password update failed due to policy violation: " + ppe.getMessage());
            var formResp = context.form();
            formResp.addError(new FormMessage(FIELD_PASS, ppe.getMessage(), ppe.getParameters()));
            context.challenge(formResp.createForm("login-update-password.ftl"));

            context.getEvent()
                    .event(EventType.UPDATE_PASSWORD_ERROR)
                    .user(user)
                    .detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession()))
                    .detail(Details.USERNAME, user.getUsername())
                    .detail(Constants.FAILURE_DETAILS, Constants.PASSWORD_POLICY_VALIDATION_FAILED)
                    .error(Errors.PASSWORD_REJECTED);

        } catch (Exception e) {
            LOG.warn("Password update failed", e);
            context.getEvent()
                    .event(EventType.UPDATE_PASSWORD_ERROR)
                    .user(user)
                    .detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession()))
                    .detail(Details.USERNAME, user.getUsername())
                    .detail(Constants.FAILURE_DETAILS, Constants.INVALID_PASSWORD_EXISTING)
                    .error(Errors.PASSWORD_REJECTED);

            challengeWithError(context, FIELD_PASS, "invalidPasswordExistingMessage");
        }
    }

    @Override
    public boolean requiresUser() { return true; }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) { /* no-op */ }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) { return true; }

    @Override
    public void close() { /* no-op */ }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String trim(String s)     { return s == null ? null : s.trim(); }

    private void challengeWithError(AuthenticationFlowContext ctx, String field, String messageKey) {
        ctx.challenge(
                ctx.form()
                        .addError(new FormMessage(field, messageKey))
                        .createForm("login-update-password.ftl")
        );
    }
}
