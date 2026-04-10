package com.keycloak.security.extensions.mappers;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class FormParamMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {
    private static final Logger LOG = Logger.getLogger(FormParamMapper.class);

    public static final String PROVIDER_ID = "form-param-mapper";
    public static final String CFG_FORM_PARAM = "form.param.name";

    private static final List<ProviderConfigProperty> CONFIG_PROPS = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(CONFIG_PROPS);
        OIDCAttributeMapperHelper.addJsonTypeConfig(CONFIG_PROPS);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(CONFIG_PROPS, FormParamMapper.class);

        ProviderConfigProperty formParam = new ProviderConfigProperty();
        formParam.setName(CFG_FORM_PARAM);
        formParam.setLabel("Form Parameter Name");
        formParam.setType(ProviderConfigProperty.STRING_TYPE);
        formParam.setHelpText("Name of the form parameter read at the token endpoint.");
        formParam.setRequired(true);
        CONFIG_PROPS.add(formParam);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getProtocol() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayCategory() {
        return "Token mapper";
    }

    @Override
    public String getDisplayType() {
        return "Form Parameter Mapper";
    }

    @Override
    public String getHelpText() {
        return "Reads a form parameter and maps it into the access token claim.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPS;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token,
                                            ProtocolMapperModel mappingModel,
                                            KeycloakSession session,
                                            UserSessionModel userSession,
                                            ClientSessionContext clientSessionCtx) {

        if (mappingModel.getConfig() == null) {
            LOG.warnf("FormParamMapper[%s]: no config present, skipping.", mappingModel.getName());
            return token;
        }

        String paramName = mappingModel.getConfig().getOrDefault(CFG_FORM_PARAM, "").trim();
        String claimName = mappingModel.getConfig().getOrDefault(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "").trim();

        if (StringUtil.isNullOrEmpty(paramName)) {
            LOG.warnf("FormParamMapper[%s]: Form Parameter Name (%s) not configured, skipping.", mappingModel.getName(), CFG_FORM_PARAM);
            return token;
        }
        if (StringUtil.isNullOrEmpty(claimName)) {
            LOG.warnf("FormParamMapper[%s]: Token Claim Name not configured, skipping.", mappingModel.getName());
            return token;
        }

        String paramVal = null;
        var httpReq = session.getContext().getHttpRequest();
        if (httpReq != null) {
            MultivaluedMap<String, String> form = httpReq.getDecodedFormParameters();
            if (form != null) {
                paramVal = form.getFirst(paramName);
            }
        }

        if (!StringUtil.isNullOrEmpty(paramVal)) {
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, paramVal);
            LOG.debugf("FormParamMapper[%s]: Mapped form param '%s'='%s' into claim '%s'.", mappingModel.getName(), paramName, paramVal, claimName);
        } else {
            LOG.debugf("FormParamMapper[%s]: No value found for form param '%s', nothing mapped.", mappingModel.getName(), paramName);
        }

        return token;
    }

}
