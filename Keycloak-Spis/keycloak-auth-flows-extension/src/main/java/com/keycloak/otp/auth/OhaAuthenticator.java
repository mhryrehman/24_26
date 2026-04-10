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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

@JBossLog
public class OhaAuthenticator implements Authenticator {
    private static final Logger LOG = Logger.getLogger(OhaAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        if (user == null) {
            context.failure(AuthenticationFlowError.UNKNOWN_USER);
            return;
        }

        String userType = user.getFirstAttribute(Constants.USER_TYPE);
        String ohaAccepted = user.getFirstAttribute(Constants.OHA_ACCEPTED);
        LOG.infof("kcId=%s User type: %s, OHA accepted: %s", user.getId(), userType, ohaAccepted);

        if (Constants.PRACTICE_ADMINS.contains(userType) || !StringUtil.isNullOrEmpty(ohaAccepted)) {
            context.success();
            return;
        }

        Response r = context.form()
                .setExecution(context.getExecution().getId())
                .createForm("oha.ftl");
        context.challenge(r);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        LeapIntegrationService uals = new LeapIntegrationService(context.getSession());
        uals.logActivity(context.getAuthenticationSession(), Constants.OHA_CONSENT, context.getFlowPath());

        var form = context.getHttpRequest().getDecodedFormParameters();
        String value = form.getFirst(Constants.FORM_PARAM_OHA_ACCEPTED);
        LOG.infof("kcId=%s OHA accepted value: %s", context.getUser().getId(), value);

        boolean accepted = "true".equalsIgnoreCase(value);
        context.getUser().setSingleAttribute(Constants.OHA_ACCEPTED, Boolean.toString(accepted));

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