package com.keycloak.otp.auth.condition;

import org.keycloak.Config;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class ConditionalClientAuthenticatorFactory implements ConditionalAuthenticatorFactory {

    public static final String PROVIDER_ID = "condition-client-list";
    public static final String CFG_ALLOWED_CLIENT_IDS = "allowed-client-ids";
    public static final String CFG_SESSION_NOTE_KEY = "session-note-key";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Condition – Client (list)";
    }

    @Override
    public String getHelpText() {
        return "Executes child executions only if the requesting client_id matches one of the configured (comma-separated) client IDs.";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(CFG_ALLOWED_CLIENT_IDS)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Allowed client IDs")
                .helpText("Comma-separated list of client IDs that should match this condition.")
                .required(true)
                .add()
                .property()
                .name(CFG_SESSION_NOTE_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Session Note key")
                .helpText("Name of the authentication session note where the result of this condition (true/false) will be stored. This note can be evaluated later by other authenticators.")
                .defaultValue("client-condition-match")
                .required(false)
                .add()
                .build();
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalClientAuthenticator.SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
