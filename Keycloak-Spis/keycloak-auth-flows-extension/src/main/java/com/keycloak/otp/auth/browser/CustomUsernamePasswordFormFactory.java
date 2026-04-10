package com.keycloak.otp.auth.browser;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class CustomUsernamePasswordFormFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "custom-username-password-form";

    @Override
    public Authenticator create(KeycloakSession session) {
        return new CustomUsernamePasswordForm(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getReferenceCategory() {
        return PasswordCredentialModel.TYPE;
    }

    @Override
    public Set<String> getOptionalReferenceCategories() {
        return Profile.isFeatureEnabled(Profile.Feature.PASSKEYS)
                ? Collections.singleton(WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                : AuthenticatorFactory.super.getOptionalReferenceCategories();
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "Custom Username Password Form";
    }

    @Override
    public String getHelpText() {
        return "Custom username and password from used to validates username and password.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

}
