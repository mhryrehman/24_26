package com.spi.smart_on_fhir.factory;

import com.spi.smart_on_fhir.authentication.PatientSelectionForm;
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
 * Factory for creating PatientSelectionForm instances.
 */
public class PatientSelectionFormFactory implements AuthenticatorFactory {

    private static final String PROVIDER_ID = "auth-select-patient";

    public static final String INTERNAL_FHIR_URL_PROP_NAME = "internalFhirUrl";
    private static final String INTERNAL_FHIR_URL_PROP_LABEL = "FHIR Base URL";
    private static final String INTERNAL_FHIR_URL_PROP_DESCRIPTION = "The internal base URL of the FHIR resource server"
            + " for retrieving Patient resources. This can differ from the external base URL used by the client in"
            + " the 'aud' parameter.";
    public static final String CLIENT_ID_PROP_NAME = "clientId";
    private static final String CLIENT_ID_PROP_LABEL = "Client ID";
    private static final String CLIENT_ID_PROP_DESCRIPTION =
            "The client ID for authenticating with the FHIR server.";

    public static final String CLIENT_SECRET_PROP_NAME = "clientSecret";
    private static final String CLIENT_SECRET_PROP_LABEL = "Client Secret";
    private static final String CLIENT_SECRET_PROP_DESCRIPTION =
            "The client secret for authenticating with the FHIR server.";
    public static final String TOKEN_ENDPOINT_URL_PROP_NAME = "tokenEndpointUrl";
    private static final String TOKEN_ENDPOINT_URL_PROP_LABEL = "Token Endpoint URL";
    private static final String TOKEN_ENDPOINT_URL_PROP_DESCRIPTION =
            "The URL of the token endpoint for obtaining access tokens.";

    @Override
    public String getDisplayType() {
        return "Patient Selection Authenticator";
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
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
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
        return "A patient context picker for supporting the launch/patient scope.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();

        configProperties.add(new ProviderConfigProperty(
                INTERNAL_FHIR_URL_PROP_NAME,
                INTERNAL_FHIR_URL_PROP_LABEL,
                INTERNAL_FHIR_URL_PROP_DESCRIPTION,
                ProviderConfigProperty.STRING_TYPE,
                null
        ));
        configProperties.add(new ProviderConfigProperty(
                CLIENT_ID_PROP_NAME,
                CLIENT_ID_PROP_LABEL,
                CLIENT_ID_PROP_DESCRIPTION,
                ProviderConfigProperty.STRING_TYPE,
                null
        ));
        configProperties.add(new ProviderConfigProperty(
                CLIENT_SECRET_PROP_NAME,
                CLIENT_SECRET_PROP_LABEL,
                CLIENT_SECRET_PROP_DESCRIPTION,
                ProviderConfigProperty.PASSWORD,
                null
        ));
        configProperties.add(new ProviderConfigProperty(
                TOKEN_ENDPOINT_URL_PROP_NAME,
                TOKEN_ENDPOINT_URL_PROP_LABEL,
                TOKEN_ENDPOINT_URL_PROP_DESCRIPTION,
                ProviderConfigProperty.STRING_TYPE,
                null
        ));
        return configProperties;
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new PatientSelectionForm();
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
