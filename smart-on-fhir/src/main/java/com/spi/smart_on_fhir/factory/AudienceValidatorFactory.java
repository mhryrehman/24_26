package com.spi.smart_on_fhir.factory;

import com.spi.smart_on_fhir.authentication.AudienceValidator;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Yasir Rehman
 * Description:
 * Factory for creating AudienceValidator instances.
 */
public class AudienceValidatorFactory implements AuthenticatorFactory {

    private static final String PROVIDER_ID = "audience-validator";

    public static final String AUDIENCES_PROP_NAME = "audiences";
    private static final String AUDIENCES_PROP_LABEL = "Allowed Audiences";
    private static final String AUDIENCES_PROP_DESCRIPTION = "Valid audiences for clients to request.";
    public static final String VALIDATE_ALWAYS_PROP_NAME = "validateAlways";
    private static final String VALIDATE_ALWAYS_PROP_LABEL = "Validate Always";
    private static final String VALIDATE_ALWAYS_PROP_DESCRIPTION = "If enabled, audience validation is performed every time. Otherwise, validation is only performed when the 'aud' parameter is present.";


    @Override
    public String getDisplayType() {
        return "Audience Validation";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

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
        return "Verifies that the audience requested by the client (via the 'aud' parameter) "
                + "matches one of the configured audience values.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();

        configProperties.add(new ProviderConfigProperty(AUDIENCES_PROP_NAME, AUDIENCES_PROP_LABEL,
                AUDIENCES_PROP_DESCRIPTION, ProviderConfigProperty.MULTIVALUED_STRING_TYPE, null));
        configProperties.add(new ProviderConfigProperty(VALIDATE_ALWAYS_PROP_NAME, VALIDATE_ALWAYS_PROP_LABEL,
                VALIDATE_ALWAYS_PROP_DESCRIPTION, ProviderConfigProperty.BOOLEAN_TYPE, false));

        return configProperties;
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new AudienceValidator(session);
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
