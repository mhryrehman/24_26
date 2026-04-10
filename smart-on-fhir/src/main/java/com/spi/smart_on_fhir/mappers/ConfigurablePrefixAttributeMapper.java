package com.spi.smart_on_fhir.mappers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

/**
 * Author: Yasir Rehman
 * Description:
 * Prepend configured prefix to the user attribute.
 */
public class ConfigurablePrefixAttributeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    public static final String PROVIDER_ID = "oidc-prefix-usermodel-attribute-mapper";

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName("user.attribute");
        property.setLabel("usermodel.attr.label");
        property.setHelpText("usermodel.attr.tooltip");
        property.setType("String");
        configProperties.add(property);
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, ConfigurablePrefixAttributeMapper.class);
        property = new ProviderConfigProperty();
        property.setName("prefixKey");
        property.setLabel("Prefix");
        property.setHelpText("The prefix to prepend to the user attribute. Leave empty to not append any prefix.");
        property.setType("String");
        configProperties.add(property);
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

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    public String getId() {
        return PROVIDER_ID;
    }

    public String getDisplayType() {
        return "Configurable Prefix Attribute Mapper";
    }

    public String getDisplayCategory() {
        return "Token mapper";
    }

    public String getHelpText() {
        return "Adds a configurable prefix to a user attribute and maps it to a token claim.";
    }

    /**
     * Sets a claim in the token based on the user attribute with an optional configurable prefix.
     *
     * @param token        the token where the claim will be set
     * @param mappingModel the protocol mapper model containing configuration
     * @param userSession  the user session model
     */
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        UserModel user = userSession.getUser();
        String attributeName = mappingModel.getConfig().get("user.attribute");
        String prefix = mappingModel.getConfig().get("prefixKey");
        boolean aggregateAttrs = Boolean.parseBoolean(mappingModel.getConfig().get("aggregate.attrs"));
        Collection<String> attributeValue = KeycloakModelUtils.resolveAttribute(user, attributeName, aggregateAttrs);
        if (attributeValue == null)
            return;
        if (prefix != null && !prefix.isEmpty())
            attributeValue = attributeValue.stream().map(prefix::concat).collect(Collectors.toList());
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeValue);
    }
}
