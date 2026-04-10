package com.keycloak.otp.auth.resetcredentials;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class PasswordResetNoSessionFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "password-reset-no-session";

    @Override
    public String getId() { return PROVIDER_ID; }

    @Override
    public String getDisplayType() { return "Password Reset (no login session)"; }

    @Override
    public String getReferenceCategory() { return null; }

    @Override
    public boolean isConfigurable() { return false; }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() { return false; }

    @Override
    public String getHelpText() {
        return "Shows a password reset form, updates the password, then redirects to the login page without creating a session.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() { return List.of(); }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new PasswordResetNoSessionAuthenticator();
    }

    @Override public void init(Config.Scope config) { }
    @Override public void postInit(KeycloakSessionFactory factory) { }
    @Override public void close() { }
}
