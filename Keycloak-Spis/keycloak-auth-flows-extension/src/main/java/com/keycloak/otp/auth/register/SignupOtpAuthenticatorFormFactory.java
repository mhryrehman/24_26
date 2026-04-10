package com.keycloak.otp.auth.register;

import com.keycloak.otp.auth.browser.OtpAuthenticatorForm;
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
 * Factory class for the {@link SignupOtpAuthenticatorForm}.
 * Registers the form-based OTP authenticator and its configurable properties in Keycloak.
 */
public class SignupOtpAuthenticatorFormFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "signup-otp-authenticator";
    public static final SignupOtpAuthenticatorForm SINGLETON = new SignupOtpAuthenticatorForm();

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
        return "Signup OTP Authenticator";
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
        return "Verifies the sign-up email with a one-time code sent after registration.";
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
                ),
                new ProviderConfigProperty(
                        Constants.CFG_PARAM_NAME,
                        "Query parameter name",
                        "Query param name to skipOtp",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty(
                        Constants.CFG_PARAM_VALUE,
                        "Value to skip (optional)",
                        "If blank, presence of the param skips OTP",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
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
