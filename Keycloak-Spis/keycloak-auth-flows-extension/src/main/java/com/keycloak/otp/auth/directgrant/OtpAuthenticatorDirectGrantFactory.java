package com.keycloak.otp.auth.directgrant;

import com.keycloak.otp.util.Constants;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Author: Yasir Rehman
 * <p>
 * Factory class for creating instances of {@link OtpAuthenticatorDirectGrant}.
 * This factory registers the authenticator in Keycloak under the provider ID "email-authenticator-direct-grant".
 * It also defines configurable properties such as OTP code length, TTL, and max resend attempts.
 */
public class OtpAuthenticatorDirectGrantFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "otp-email-authenticator-direct-grant";
    public static final OtpAuthenticatorDirectGrant SINGLETON = new OtpAuthenticatorDirectGrant();

    /**
     * Returns the unique provider ID used to register this authenticator in Keycloak.
     */
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * Returns the display name shown in the admin console for this authenticator.
     */
    @Override
    public String getDisplayType() {
        return "Email OTP Direct Grant";
    }

    /**
     * Returns the credential type this authenticator is associated with (OTP).
     */
    @Override
    public String getReferenceCategory() {
        return OTPCredentialModel.TYPE;
    }

    /**
     * Indicates whether the authenticator has configurable properties.
     */
    @Override
    public boolean isConfigurable() {
        return true;
    }

    /**
     * Defines the available requirement choices for this authenticator.
     */
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    /**
     * Indicates whether the user is allowed to configure this authenticator individually.
     */
    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    /**
     * Returns help text describing this authenticator.
     */
    @Override
    public String getHelpText() {
        return "Email OTP Direct Grant Authenticator";
    }

    /**
     * Returns a list of configurable properties for this authenticator.
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                new ProviderConfigProperty(
                        Constants.CODE_LENGTH,
                        "Code length",
                        "The number of digits of the generated code.",
                        ProviderConfigProperty.STRING_TYPE,
                        String.valueOf(Constants.DEFAULT_LENGTH)
                ),
                new ProviderConfigProperty(
                        Constants.CODE_TTL,
                        "Time-to-live",
                        "The time to live in seconds for the code to be valid.",
                        ProviderConfigProperty.STRING_TYPE,
                        String.valueOf(Constants.DEFAULT_TTL)
                ),
                new ProviderConfigProperty(
                        Constants.MAX_RESEND_ATTEMPTS,
                        "Max Resend Attempts",
                        "The maximum number of times a user can request to resend the OTP before blocking further resends.",
                        ProviderConfigProperty.STRING_TYPE,
                        Constants.DEFAULT_MAX_RESEND_ATTEMPTS
                ),
                new ProviderConfigProperty(
                        Constants.NOVU_URI,
                        "Novu URI",
                        "The URI endpoint to trigger the Novu email workflow.",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty(
                        Constants.NOVU_API_KEY,
                        "API Key",
                        "The API key used for Novu authorization.",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty(
                        Constants.WORK_FLOW_NAME,
                        "Novu Workflow Name",
                        "The name of the Novu workflow to trigger.",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty("tokenUrl",
                        Constants.TOKEN_URL,
                        "The token endpoint to retrieve the bearer token for SMS service.",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty(
                        Constants.TOKEN_REQUEST_BODY,
                        "Token Request Body",
                        "The request body to retrieve the token (e.g., client credentials form data).",
                        ProviderConfigProperty.PASSWORD,
                        ""
                ),
                new ProviderConfigProperty(
                        Constants.SMS_URL,
                        "SMS URL",
                        "The endpoint to send the SMS with OTP.",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty(
                        Constants.MESSAGE_TEXT,
                        "SMS Message Template",
                        "The template for the SMS message (e.g., 'Hi {otpModel.FirstName} your code is {otpModel.OTP}').",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty(
                        Constants.MESSAGE_TYPE,
                        "SMS Message Type",
                        "The type of message being sent.",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                )
        );
    }

    /**
     * Cleanup logic when the factory is closed (no operation in this case).
     */
    @Override
    public void close() {
        // NOOP
    }

    /**
     * Returns the singleton instance of the authenticator.
     */
    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    /**
     * Initializes the factory from configuration (no operation in this case).
     */
    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    /**
     * Hook for post-initialization logic after Keycloak starts (no operation in this case).
     */
    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }
}
