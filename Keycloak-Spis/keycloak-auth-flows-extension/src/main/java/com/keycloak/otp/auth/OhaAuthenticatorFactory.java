package com.keycloak.otp.auth;


import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;

public class OhaAuthenticatorFactory implements AuthenticatorFactory {

    public static final String ID = "oha-authenticator";
    private static final OhaAuthenticator SINGLETON = new OhaAuthenticator();

    private static ProviderConfigProperty prop(String name, String label, String help, String type, String def) {
        var p = new ProviderConfigProperty();
        p.setName(name); p.setLabel(label); p.setHelpText(help); p.setType(type); p.setDefaultValue(def);
        return p;
    }


    @Override public String getId() { return ID; }
    @Override public String getDisplayType() { return "Oha Screen"; }
    @Override public String getHelpText() { return "Shows a Terms & Conditions checkbox page."; }
    @Override public String getReferenceCategory() { return "terms"; }
    @Override public Authenticator create(KeycloakSession session) { return SINGLETON; }
    @Override public void init(Config.Scope config) {}
    @Override public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {}
    @Override public void close() {}
    @Override public boolean isUserSetupAllowed() { return false; }
    @Override public boolean isConfigurable() { return true; }
    @Override public java.util.List<ProviderConfigProperty> getConfigProperties() { return new ArrayList<>(); }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }
}
