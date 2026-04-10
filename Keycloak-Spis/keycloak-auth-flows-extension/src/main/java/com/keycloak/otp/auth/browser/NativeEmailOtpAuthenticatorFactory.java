package com.keycloak.otp.auth.browser;

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
 * Factory class for the {@link OtpAuthenticatorForm}.
 * Registers the form-based OTP authenticator and its configurable properties in Keycloak.
 */
public class NativeEmailOtpAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "native-email-otp-authenticator";
    public static final NativeEmailOtpAuthenticator SINGLETON = new NativeEmailOtpAuthenticator();

    /**
     * Returns the unique provider ID for this authenticator.
     */
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * Returns the display name shown in the Keycloak admin console.
     */
    @Override
    public String getDisplayType() {
        return "Native Email Provider OTP Authenticator";
    }

    /**
     * Returns the reference category for this authenticator (OTP credential).
     */
    @Override
    public String getReferenceCategory() {
        return OTPCredentialModel.TYPE;
    }

    /**
     * Indicates that this authenticator has configurable properties.
     */
    @Override
    public boolean isConfigurable() {
        return true;
    }

    /**
     * Returns the set of requirement choices for this authenticator.
     */
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    /**
     * Indicates whether this authenticator allows per-user setup (it does not).
     */
    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    /**
     * Returns help text for the admin console describing this authenticator.
     */
    @Override
    public String getHelpText() {
        return "Generates a one-time password (OTP) and delivers it using the built-in email provider or configured SMS service. Supports configurable code length, expiry, resend limits, and optional forced execution.";
    }

    /**
     * Returns the configurable properties for this authenticator such as code length and TTL.
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
                        Constants.RESEND_TIME,
                        "Resend Timer (seconds)",
                        "The time in seconds that a user must wait before they can request to resend the OTP.",
                        ProviderConfigProperty.STRING_TYPE,
                        Constants.DEFAULT_RESEND_TIME
                ),
                new ProviderConfigProperty(
                        "emailSubjectTemplate",
                        "Email subject (template)",
                        "Subject template. Use placeholders like ${code}, ${realmName}, ${user}. Example: 'Your verification code: ${code}'",
                        ProviderConfigProperty.STRING_TYPE,
                        "Your verification code"
                ),
                new ProviderConfigProperty(
                        "emailTextBodyTemplate",
                        "Email text body (template)",
                        "Plain text body template. Use placeholders like ${code}, ${expiresMinutes}.",
                        ProviderConfigProperty.STRING_TYPE,
                        "Your verification code is ${code}. It expires in ${expiresMinutes} minutes."
                ),
                new ProviderConfigProperty(
                        "emailHtmlBodyTemplate",
                        "Email HTML body (template)",
                        "HTML body template. Use placeholders like ${code}, ${expiresMinutes}, ${realmName}.",
                        ProviderConfigProperty.STRING_TYPE,
                        "<p>Your verification code is <strong>${code}</strong>. It will expire in ${expiresMinutes} minutes.</p>"
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
                ),
                new ProviderConfigProperty(
                        Constants.ALWAYS_EXECUTE,
                        "Always Execute",
                        "If enabled, this OTP step is executed every time, regardless of conditional flow logic.",
                        ProviderConfigProperty.BOOLEAN_TYPE,
                        "false"
                )
        );

    }

    /**
     * Cleanup method called when the factory is closed (no operation).
     */
    @Override
    public void close() {
        // NOOP
    }

    /**
     * Returns the singleton instance of {@link OtpAuthenticatorForm}.
     */
    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    /**
     * Initialization method for loading configuration (not used).
     */
    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    /**
     * Post-initialization hook after Keycloak startup (not used).
     */
    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }
}
