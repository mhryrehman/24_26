package com.keycloak.security.extensions.profilecookie;

import com.keycloak.security.extensions.common.Constant;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import com.keycloak.security.extensions.profilecookie.ProfileCookieAuthenticator;

import java.util.Arrays;
import java.util.List;

public class ProfileCookieAuthenticatorFactory implements AuthenticatorFactory {

    private static final ProfileCookieAuthenticator SINGLETON = new ProfileCookieAuthenticator();

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = Arrays.asList(
            new ProviderConfigProperty(Constant.PROP_COOKIE_NAME, "Cookie Name", "Name of the cookie to store user profiles", ProviderConfigProperty.STRING_TYPE, Constant.DEFAULT_COOKIE_NAME),
            new ProviderConfigProperty(Constant.PROP_COOKIE_MAX_AGE, "Cookie Max Age (Seconds)", "Lifespan of the cookie in seconds", ProviderConfigProperty.STRING_TYPE, Constant.DEFAULT_COOKIE_MAX_AGE),
            new ProviderConfigProperty(Constant.PROP_COOKIE_PATH, "Cookie Path", "Path scope for the cookie", ProviderConfigProperty.STRING_TYPE, Constant.DEFAULT_COOKIE_PATH),
            new ProviderConfigProperty(Constant.PROP_HTTP_ONLY, "HttpOnly", "Set HttpOnly flag", ProviderConfigProperty.BOOLEAN_TYPE, Constant.DEFAULT_HTTP_ONLY),
            new ProviderConfigProperty(Constant.PROP_COOKIE_COMMENT, "Cookie Comment", "Comment for the cookie", ProviderConfigProperty.STRING_TYPE, Constant.DEFAULT_COOKIE_COMMENT)
    );

    @Override
    public String getId() {
        return Constant.PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return Constant.DISPLAY_TYPE;
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return Constant.HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}
}
