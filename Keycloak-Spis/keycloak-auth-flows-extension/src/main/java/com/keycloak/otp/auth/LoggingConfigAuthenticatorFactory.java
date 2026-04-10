package com.keycloak.otp.auth;

import com.keycloak.otp.util.Constants;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

public class LoggingConfigAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "logging-config";

    private static final LoggingConfigAuthenticator SINGLETON = new LoggingConfigAuthenticator();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Leap Logging Config";
    }

    @Override
    public String getHelpText() {
        return "Provides configuration (token URL, clientId, secret, API URL) for logging service. " +
                "Add once per realm. Other authenticators can read its config.";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(
                new ProviderConfigProperty(Constants.LOG_TOKEN_URL, "Token URL",
                        "The URL of the Keycloak token endpoint for client_credentials flow",
                        ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(Constants.LOG_CLIENT_ID, "Client ID",
                        "Client ID for service account", ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(Constants.LOG_CLIENT_SECRET, "Client Secret",
                        "Client secret for service account", ProviderConfigProperty.PASSWORD, null),
                new ProviderConfigProperty(Constants.LOG_API_URL, "Logging API URL",
                        "The URL of the Leap service used to record user activity.", ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(Constants.LEAP_REG_API_URL, "Leap Registration API URL",
                        "The URL of the Leap service used to register new users during the Keycloak signup process.", ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(Constants.LEAP_RP_REG_API_URL, "Leap Related Patient API URL",
                        "The URL of the Leap service used to register new related patient users during the Keycloak signup process.", ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(Constants.LEAP_LOG_FAILED_SESSION_URL, "Failed Session Logging API URL",
                        "The endpoint URL used to send failed authentication/session data (login, registration, OTP, reset password) to the Leap service.", ProviderConfigProperty.STRING_TYPE, null)
        );
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
}

