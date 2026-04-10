package com.curemd.samlsessionnotemapper.keycloak;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

public class SamlSessionNoteIdpMapper extends AbstractIdentityProviderMapper  {

    public static final String PROVIDER_ID = "saml-session-note-idp-mapper";

    private static final String CONF_ATTRIBUTE_NAME = "saml.attribute.name";
    private static final String CONF_SESSION_NOTE_NAME = "session.note.name";

    private static final Logger LOG = Logger.getLogger(SamlSessionNoteIdpMapper.class);

    private static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};
    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        CONFIG_PROPERTIES = new ArrayList<>();

        ProviderConfigProperty attributeName = new ProviderConfigProperty();
        attributeName.setName(CONF_ATTRIBUTE_NAME);
        attributeName.setLabel("SAML Attribute Name");
        attributeName.setType(ProviderConfigProperty.STRING_TYPE);
        attributeName.setHelpText(
                "Name of the SAML attribute whose value will be copied " +
                "into the user session note."
        );

        ProviderConfigProperty sessionNoteName = new ProviderConfigProperty();
        sessionNoteName.setName(CONF_SESSION_NOTE_NAME);
        sessionNoteName.setLabel("User Session Note Name");
        sessionNoteName.setType(ProviderConfigProperty.STRING_TYPE);
        sessionNoteName.setHelpText(
                "Name of the user session note where the attribute value will be stored."
        );

        CONFIG_PROPERTIES.add(attributeName);
        CONFIG_PROPERTIES.add(sessionNoteName);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "SAML Attribute to User Session Note";
    }

    @Override
    public String getHelpText() {
        return "Copies the value of a SAML attribute directly into a user session note.";
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public void preprocessFederatedIdentity(
            KeycloakSession session,
            RealmModel realm,
            IdentityProviderMapperModel mapperModel,
            BrokeredIdentityContext context) {

        String attributeName = mapperModel.getConfig().get(CONF_ATTRIBUTE_NAME);
        String sessionNoteName = mapperModel.getConfig().get(CONF_SESSION_NOTE_NAME);
        LOG.debugf("Processing SAML attributes %s to %s", attributeName,sessionNoteName);
        if (attributeName == null || attributeName.isBlank()) {
            return;
        }
        if (sessionNoteName == null || sessionNoteName.isBlank()) {
            return;
        }

        List<String> values = findAttributeValuesInContext(attributeName, context);

        LOG.debugf("Values found : %s", values);

        if (values == null || values.isEmpty()) {
            return;
        }

        // If multiple values exist, use the first one.
        // (You can change this to String.join(",", values) if needed.)
        String value = values.get(0);
        context.getAuthenticationSession().setUserSessionNote(sessionNoteName, value);
        LOG.debugf("Current Session : %s",context.getAuthenticationSession().getUserSessionNotes());
    }

    @Override
    public String getDisplayCategory() {
        return "SAML Attribute to User Session Note";
    }
    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return true;
    }


    private List<String> findAttributeValuesInContext(String attributeName, BrokeredIdentityContext context) {
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

        return assertion.getAttributeStatements().stream()
                .flatMap(statement -> statement.getAttributes().stream())
                .filter(elementWith(attributeName))
                .flatMap(attributeType -> attributeType.getAttribute().getAttributeValue().stream())
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }
    private Predicate<AttributeStatementType.ASTChoiceType> elementWith(String attributeName) {
        return attributeType -> {
            AttributeType attribute = attributeType.getAttribute();
            return Objects.equals(attribute.getName(), attributeName)
                    || Objects.equals(attribute.getFriendlyName(), attributeName);
        };
    }
}
