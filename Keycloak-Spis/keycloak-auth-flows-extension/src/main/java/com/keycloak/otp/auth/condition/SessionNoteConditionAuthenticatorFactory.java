package com.keycloak.otp.auth.condition;

import org.keycloak.Config;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class SessionNoteConditionAuthenticatorFactory implements ConditionalAuthenticatorFactory {

    public static final String PROVIDER_ID = "condition-session-note";
    public static final String CFG_NOTE_KEY = "note-key";
    public static final String CFG_TRUTHY_VALUES = "truthy-values";
    public static final String CFG_MATCH_ON_TRUTHY = "match-on-truthy";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Condition – Session Note";
    }

    @Override
    public String getHelpText() {
        return "Evaluates the authentication session note against a configurable set of truthy values. "
                + "Set 'match-on-truthy' to true to match when the note is truthy; set it to false to match when the note is NOT truthy.";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
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
                .name(CFG_NOTE_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Session note key")
                .helpText("Name of the authentication session note to evaluate.")
                .defaultValue("client-condition-match")
                .required(true)
                .add()
                .property()
                .name(CFG_TRUTHY_VALUES)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Accepted values (CSV)")
                .helpText("Comma-separated list of values considered valid. The session note value must exactly match one of these entries.")
                .defaultValue("true")
                .required(true)
                .add()
                .property()
                .name(CFG_MATCH_ON_TRUTHY)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Match when value is in accepted list")
                .helpText("If enabled, this condition matches when the session note value is one of the accepted values. If disabled, it matches when the session note value is not in the accepted list.")
                .defaultValue("false")
                .required(false)
                .add()
                .build();
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return SessionNoteConditionAuthenticator.SINGLETON;
    }

    @Override public void init(Config.Scope config) { }
    @Override public void postInit(KeycloakSessionFactory factory) { }
    @Override public void close() { }
}
