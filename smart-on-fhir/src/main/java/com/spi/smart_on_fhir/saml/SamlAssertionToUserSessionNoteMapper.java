package com.spi.smart_on_fhir.saml;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;

/**
 * Simple SAML -> User Session Note mapper.
 *
 * Configuration:
 * - "mappings" : MAP where key = incoming assertion attribute name (or "NAMEID")
 *                and value = session-note-key to set.
 *
 * Behavior:
 * - For each mapping, the mapper looks up the first value of the named assertion attribute
 *   (or the NameID if mapping key is "NAMEID") and sets it as a user session note using
 *   the configured session-note-key.
 *
 * - No regex or validation. If attribute missing -> nothing set for that mapping.
 *
 * Provider id: saml-assertion-to-session-note
 */
public class SamlAssertionToUserSessionNoteMapper extends AbstractIdentityProviderMapper {

    private static final Logger LOG = Logger.getLogger(SamlAssertionToUserSessionNoteMapper.class);

    // config property name containing map of incomingAttr -> sessionNoteKey
    private static final String MAPPINGS_CONFIG = "mappings";

    public static final String[] COMPATIBLE_PROVIDERS = { SAMLIdentityProviderFactory.PROVIDER_ID };

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES =
            new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty mappings = new ProviderConfigProperty();
        mappings.setName(MAPPINGS_CONFIG);
        mappings.setLabel("Assertion attribute -> Session note key");
        mappings.setHelpText("Map incoming assertion attribute name (or NAMEID) to session note key. Example: key='employeeId', value='curemd.employeeId'");
        mappings.setType(ProviderConfigProperty.MAP_TYPE);
        CONFIG_PROPERTIES.add(mappings);
    }

    public static final String PROVIDER_ID = "saml-assertion-to-session-note";

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "User Session";
    }

    @Override
    public String getDisplayType() {
        return "SAML Assertion -> User Session Note";
    }

    @Override
    public String getHelpText() {
        return "Reads assertion attribute (or NAMEID) and stores its value in a user session note (configured key). No validation or regex.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
                              IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        LOG.infof("importNewUser: mapper=%s for broker user=%s", mapperModel.getId(), context == null ? "unknown" : context.getUsername());
        applyMappings(mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
                                   IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        LOG.infof("updateBrokeredUser: mapper=%s for broker user=%s", mapperModel.getId(), context == null ? "unknown" : context.getUsername());
        applyMappings(mapperModel, context);
    }

    @SuppressWarnings("unchecked")
    private void applyMappings(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        try {
            Map<String, List<String>> mappings = mapperModel.getConfigMap(MAPPINGS_CONFIG);
            if (mappings == null || mappings.isEmpty()) {
                LOG.infof("No mappings configured for mapper %s", mapperModel.getId());
                return;
            }

            Object assertionObj = context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
            if (!(assertionObj instanceof AssertionType)) {
                LOG.infof("No SAML Assertion present in broker context for user '%s'. Keys: %s", context.getUsername(), context.getContextData().keySet());
                return;
            }

            AssertionType assertion = (AssertionType) assertionObj;

            for (Map.Entry<String, List<String>> e : mappings.entrySet()) {
                String incoming = e.getKey();       // incoming attribute name or "NAMEID"
                String sessionNoteKey = e.getValue().get(0); // where to store value

                if (incoming == null || incoming.trim().isEmpty() || sessionNoteKey == null || sessionNoteKey.trim().isEmpty()) {
                    LOG.warnf("Ignoring invalid mapping entry (incoming='%s', sessionNoteKey='%s')", incoming, sessionNoteKey);
                    continue;
                }

                // Normal attribute: find attribute statements and first matching attribute by name or friendlyName
                boolean found = false;
                Set<AttributeStatementType> stmts = assertion.getAttributeStatements();
                if (stmts != null) {
                    for (AttributeStatementType stmt : stmts) {
                        for (AttributeStatementType.ASTChoiceType choice : stmt.getAttributes()) {
                            AttributeType attr = choice.getAttribute();
                            if (attr == null) continue;
                            String name = attr.getName();
                            String friendly = attr.getFriendlyName();
                            if (incoming.equals(name) || incoming.equals(friendly)) {
                                List<Object> values = attr.getAttributeValue();
                                if (values != null && !values.isEmpty()) {
                                    Object valObj = values.get(0);
                                    if (valObj != null) {
                                        String value = valObj.toString();
                                        context.setSessionNote(sessionNoteKey, value);
                                        LOG.infof("Set session note '%s'='%s' from attribute '%s' for broker user '%s'", sessionNoteKey, value, incoming, context.getBrokerUserId());
                                        found = true;
                                        break;
                                    }
                                } else {
                                    LOG.infof("Attribute '%s' found but has no values", incoming);
                                }
                            }
                        }
                        if (found) break;
                    }
                }

                if (!found) {
                    LOG.infof("No attribute '%s' found in assertion; no session note set for key '%s'", incoming, sessionNoteKey);
                }
            }

            LOG.infof("Completed applying mappings; total session-notes now: ");
        } catch (Exception ex) {
            LOG.errorf(ex, "Error while applying SAML -> session note mappings for broker user '%s'", context == null ? "unknown" : context.getUsername());
        }
    }
}
