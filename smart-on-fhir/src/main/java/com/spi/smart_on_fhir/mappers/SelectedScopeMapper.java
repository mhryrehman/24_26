package com.spi.smart_on_fhir.mappers;

import com.spi.smart_on_fhir.utils.Constants;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Author: Yasir Rehman
 * Description:
 * A mapper to override the scope claim in the token with selected and default scopes.
 */
public class SelectedScopeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {
    private static final Logger LOG = Logger.getLogger(SelectedScopeMapper.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    public static final String PROVIDER_ID = "oidc-selected-scope-mapper";

    static {
        ProviderConfigProperty property;
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, ConfigurablePrefixAttributeMapper.class);
        property = new ProviderConfigProperty();
        property.setName("multivalued");
        property.setLabel("multivalued.label");
        property.setHelpText("multivalued.tooltip");
        property.setType("boolean");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName("aggregate.attrs");
        property.setLabel("aggregate.attrs.label");
        property.setHelpText("aggregate.attrs.tooltip");
        property.setType("boolean");
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Selected Scope Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Overrides the scope claim in the token with selected and default scopes stored in session notes.";
    }

    /**
     * Transforms the access token by overriding its scope claim with selected and default scopes.
     *
     * @param token            the access token being transformed
     * @param mappingModel     the protocol mapper model
     * @param session          the Keycloak session
     * @param userSession      the user session model
     * @param clientSessionCtx the client session context
     * @return the transformed access token
     */
    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // Retrieve selected and default scopes from session notes
        String selectedScopes = userSession.getNote(Constants.SELECTED_SCOPES_NOTE);
        String defaultScopes = userSession.getNote(Constants.DEFAULT_SCOPES_NOTE);

        LOG.info("Selected scopes: " + selectedScopes);
        LOG.info("Default scopes: " + defaultScopes);
        //if selectedScopes is null or empty then no need to add default scopes.
        if (null != selectedScopes && !selectedScopes.isEmpty()) {
            // Initialize scope lists
            List<String> selectedScopeList = parseScopes(selectedScopes);
            List<String> defaultScopeList = parseScopes(defaultScopes);

            selectedScopeList.addAll(defaultScopeList);
            String finalScopes = String.join(" ", selectedScopeList);
            token.setScope(finalScopes);

            LOG.info("Final scope set in token: " + finalScopes);
        }

        return token;
    }

    /**
     * Parses a whitespace-separated scope string into a list of scopes.
     *
     * @param scopes A string of scopes separated by whitespace.
     * @return A list of individual scopes or an empty list if the input is null or empty.
     */
    private List<String> parseScopes(String scopes) {
        if (scopes == null || scopes.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(scopes.split(" "))
                .map(String::trim)
                .filter(scope -> !scope.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
