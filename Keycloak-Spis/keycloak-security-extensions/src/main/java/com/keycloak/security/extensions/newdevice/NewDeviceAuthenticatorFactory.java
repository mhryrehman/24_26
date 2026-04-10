package com.keycloak.security.extensions.newdevice;

import com.keycloak.security.extensions.common.Constant;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Author: Yasir Rehman
 * <p>
 * Factory class for the {@link NewDeviceAuthenticator}.
 * Provides metadata, configuration options, and registers the authenticator so it can be added
 * to Keycloak authentication flows.
 */
public class NewDeviceAuthenticatorFactory implements AuthenticatorFactory {

    /** Provider ID shown in export JSON and used for SPI wiring */
    public static final String ID = "new-device-login-notifier";

    private static final NewDeviceAuthenticator SINGLETON = new NewDeviceAuthenticator();

    /** Returns the unique provider ID for this authenticator. */
    @Override
    public String getId() {
        return ID;
    }

    /** Creates an authenticator instance (singleton in this case). */
    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    /** Returns the display name shown in the Admin Console. */
    @Override
    public String getDisplayType() {
        return "New Device Login Notifier";
    }

    /** Returns help text describing what this authenticator does. */
    @Override
    public String getHelpText() {
        return "Notifies the user by email when a login is detected from a new or unrecognized device.";
    }

    /** Returns the logical reference category for grouping (here, 'device'). */
    @Override
    public String getReferenceCategory() {
        return "device";
    }

    /** Indicates whether this authenticator has configurable properties. */
    @Override
    public boolean isConfigurable() {
        return true;
    }

    /** Indicates whether end users can configure this authenticator themselves (never for SPI). */
    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    /** Defines which requirement choices (REQUIRED, ALTERNATIVE, DISABLED) are supported. */
    @Override
    public Requirement[] getRequirementChoices() {
        return new Requirement[] { Requirement.REQUIRED, Requirement.ALTERNATIVE, Requirement.DISABLED };
    }

    /** Returns the list of configurable properties shown in the Admin Console. */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                new ProviderConfigProperty(
                        Constant.USER_ATTRIBUTE_NAME,
                        "User Attribute (CSV)",
                        "Name of the user attribute that stores a comma-separated list of known device IDs.",
                        ProviderConfigProperty.STRING_TYPE,
                        "device_addresses"
                ),
                new ProviderConfigProperty(
                        Constant.DEVICE_QUERY_PARAM_NAME,
                        "Device Parameter Name",
                        "Name of the OIDC authorization request parameter (stored as a client note) that carries the device ID.",
                        ProviderConfigProperty.STRING_TYPE,
                        "device_id"
                ),
                new ProviderConfigProperty(
                        Constant.NOVU_URI,
                        "Novu Trigger URI",
                        "Novu events trigger endpoint (e.g., https://api.novu.co/v1/events/trigger).",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty(
                        Constant.NOVU_API_KEY,
                        "Novu API Key",
                        "Project API key used to authorize calls to Novu.",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty(
                        Constant.WORK_FLOW_NAME,
                        "Novu Workflow Name",
                        "Workflow (trigger identifier) to invoke for new-device notifications (e.g., new-device-login).",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                )
        );
    }

    /** Initializes this factory from global config (unused here). */
    @Override
    public void init(Config.Scope config) { /* no-op */ }

    /** Called after the factory is initialized; used for setup across cluster nodes (unused). */
    @Override
    public void postInit(KeycloakSessionFactory factory) { /* no-op */ }

    /** Called on shutdown/cleanup of this factory; no-op here. */
    @Override
    public void close() { /* no-op */ }
}
