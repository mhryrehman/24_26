package com.keycloak.otp.auth;

import com.keycloak.otp.service.LeapIntegrationService;
import com.keycloak.otp.util.Constants;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

@JBossLog
public class EulaAuthenticator implements Authenticator {
    private static final Logger LOG = Logger.getLogger(EulaAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            context.failure(AuthenticationFlowError.UNKNOWN_USER);
            return;
        }
        // ----- EULA gate -----
        String userType = user.getFirstAttribute(Constants.USER_TYPE);
        String eulaAccepted = user.getFirstAttribute(Constants.EULA_ACCEPTED);
        LOG.infof("kcId=%s userType=%s EULA accepted=%s", user.getId(), userType, eulaAccepted);

        if (Constants.PRACTICE_ADMINS.contains(userType) || "true".equalsIgnoreCase(eulaAccepted)) {
            context.success();
            return;
        }

        Response r = context.form().setExecution(context.getExecution().getId()).createForm("terms.ftl");
        context.challenge(r);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Log acceptance
        LeapIntegrationService uals = new LeapIntegrationService(context.getSession());
        uals.logActivity(context.getAuthenticationSession(), Constants.ACTION_EULA_ACCEPTED, context.getFlowPath());

        var form = context.getHttpRequest().getDecodedFormParameters();
        String v = form.getFirst(Constants.FORM_PARAM_EULA_ACCEPTED);
        LOG.infof("kcId=%s EULA accepted value: " + v, context.getUser().getId());

        boolean accepted = "true".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v);
        context.getUser().setSingleAttribute(Constants.EULA_ACCEPTED, Boolean.toString(accepted));

        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession s, RealmModel r, UserModel u) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession s, RealmModel r, UserModel u) {
    }

    @Override
    public void close() {
    }
}
