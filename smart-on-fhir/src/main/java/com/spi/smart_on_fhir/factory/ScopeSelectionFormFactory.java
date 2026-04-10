package com.spi.smart_on_fhir.factory;

import com.spi.smart_on_fhir.authentication.ScopeSelectionForm;
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
 * Factory for creating ScopeSelectionForm instances.
 */
public class ScopeSelectionFormFactory implements AuthenticatorFactory {

    private static final String PROVIDER_ID = "auth-select-scope";

    public static final String CFG_ENABLE_GRANULAR = "enableGranularScopes";
    public static final String CFG_CONDITION_SUBSCOPES = "conditionSubScopes";
    public static final String CFG_OBSERVATION_SUBSCOPES = "observationSubScopes";

    @Override
    public String getDisplayType() {
        return "Scope Selection Authenticator";
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
        return "A scope selection picker for supporting custom scope selection during authentication.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> props = new ArrayList<>();

        // Toggle for enabling granular scope expansion
        ProviderConfigProperty enableGranular = new ProviderConfigProperty();
        enableGranular.setName(CFG_ENABLE_GRANULAR);
        enableGranular.setLabel("Enable SMART Granular Scope Selection");
        enableGranular.setHelpText("If enabled, resource-level Condition/Observation scopes will be replaced by sub-resource (granular) scopes.");
        enableGranular.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        enableGranular.setDefaultValue("false");
        props.add(enableGranular);

//        // Condition sub-scope mapping (comma-separated)
//        ProviderConfigProperty conditionMap = new ProviderConfigProperty();
//        conditionMap.setName(CFG_CONDITION_SUBSCOPES);
//        conditionMap.setLabel("Condition Sub-Resource Scopes (CSV)");
//        conditionMap.setHelpText(
//                "Comma-separated list of sub-resource scopes to offer when a resource-level Condition scope is requested.\n" +
//                        "Example: patient/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis"
//        );
//        conditionMap.setType(ProviderConfigProperty.STRING_TYPE);
//        conditionMap.setDefaultValue(
//                "patient/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis," +
//                        "patient/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item," +
//                        "patient/Condition.rs?category=http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern"
//        );
//        props.add(conditionMap);
//
//        // Observation sub-scope mapping (comma-separated)
//        ProviderConfigProperty observationMap = new ProviderConfigProperty();
//        observationMap.setName(CFG_OBSERVATION_SUBSCOPES);
//        observationMap.setLabel("Observation Sub-Resource Scopes (CSV)");
//        observationMap.setHelpText(
//                "Comma-separated list of sub-resource scopes to offer when a resource-level Observation scope is requested.\n" +
//                        "Example: patient/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|vital-signs"
//        );
//        observationMap.setType(ProviderConfigProperty.STRING_TYPE);
//        observationMap.setDefaultValue(
//                "patient/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|vital-signs," +
//                        "patient/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory," +
//                        "patient/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|social-history," +
//                        "patient/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|survey," +
//                        "patient/Observation.rs?category=http://hl7.org/fhir/us/core/CodeSystem/us-core-category|sdoh"
//        );
//        props.add(observationMap);

        return props;
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new ScopeSelectionForm();
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
