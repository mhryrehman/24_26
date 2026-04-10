package com.keycloak.otp.auth;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class LoggingConfigAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // This authenticator never blocks; just immediately succeeds.
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session,
                                 RealmModel realm,
                                 UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session,
                                   RealmModel realm,
                                   UserModel user) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
