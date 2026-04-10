package com.keycloak.security.extensions.mappers;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ExternalUserAccessProtocolMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "external-user-access-mapper";

    // Config keys
    public static final String CFG_TOKEN_URL = "clientTokenUrl";
    public static final String CFG_CLIENT_ID = "clientId";
    public static final String CFG_CLIENT_SECRET = "clientSecret";
    public static final String CFG_EXTERNAL_API = "leapApiUrl";
    public static final String CFG_CLAIM_NAME = "claimName";           // default: user_access
    public static final String CFG_HTTP_TIMEOUT = "httpTimeoutMs";       // default: 5000
    public static final String CFG_EXP_SKEW = "expirySkewSeconds";   // default: 30

    private static final Logger LOG = Logger.getLogger(ExternalUserAccessService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getDisplayCategory() {
        return "Token Mapper";
    }

    @Override
    public String getDisplayType() {
        return "External User Access (client token + API)";
    }

    @Override
    public String getHelpText() {
        return "Fetch client-credentials token, call external user-access API, add JSON to a claim.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> props = new ArrayList<>();
        props.add(prop(CFG_TOKEN_URL, "Client Token URL", "Realm token endpoint for client-credentials.", "", true));
        props.add(prop(CFG_CLIENT_ID, "Client ID", "Client for client-credentials.", "", true));
        props.add(pwd(CFG_CLIENT_SECRET, "Client Secret", "Secret for client-credentials.", "", true));
        props.add(prop(CFG_EXTERNAL_API, "External API Base URL",
                "Without query params, e.g. https://stagingapi.leaphealth.ai/leap-administration/api/v1/user-access", "", true));
        props.add(prop(CFG_CLAIM_NAME, "Claim Name", "JSON claim to store API response.", "user_access", false));
        props.add(prop(CFG_HTTP_TIMEOUT, "HTTP Timeout (ms)", "Per-request timeout.", "5000", false));
        props.add(prop(CFG_EXP_SKEW, "Expiry Skew (s)", "Refresh client token this many seconds early.", "30", false));

        OIDCAttributeMapperHelper.addIncludeInTokensConfig(props, ExternalUserAccessProtocolMapper.class);


        return props;
    }

    private static ProviderConfigProperty prop(String name, String label, String help, String def, boolean req) {
        ProviderConfigProperty p = new ProviderConfigProperty();
        p.setName(name);
        p.setLabel(label);
        p.setHelpText(help);
        p.setType(ProviderConfigProperty.STRING_TYPE);
        p.setDefaultValue(def);
        p.setRequired(req);
        return p;
    }

    private static ProviderConfigProperty pwd(String name, String label, String help, String def, boolean req) {
        ProviderConfigProperty p = new ProviderConfigProperty();
        p.setName(name);
        p.setLabel(label);
        p.setHelpText(help);
        p.setType(ProviderConfigProperty.PASSWORD);
        p.setDefaultValue(def);
        p.setRequired(req);
        return p;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel cfg,
                                            KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        setClaim(token, cfg, session, userSession, clientSessionCtx);
        return token;
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel cfg,
                                              KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        setClaim(token, cfg, session, userSession, clientSessionCtx);
        return token;
    }

    protected void setClaim(AccessToken token, ProtocolMapperModel cfg,
                            KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

        final String tokenUrl = require(cfg, CFG_TOKEN_URL);
        final String clientId = require(cfg, CFG_CLIENT_ID);
        final String clientSecret = require(cfg, CFG_CLIENT_SECRET);
        final String apiBase = require(cfg, CFG_EXTERNAL_API);

        if (StringUtil.isNullOrEmpty(tokenUrl) || StringUtil.isNullOrEmpty(clientId) || StringUtil.isNullOrEmpty(clientSecret) || StringUtil.isNullOrEmpty(apiBase)) {
            return;
        }

        final String claimName = cfg.getConfig().getOrDefault(CFG_CLAIM_NAME, "user_access");
        final int timeoutMs = parseInt(cfg.getConfig().getOrDefault(CFG_HTTP_TIMEOUT, "5000"), 5000);
        final int skewSec = parseInt(cfg.getConfig().getOrDefault(CFG_EXP_SKEW, "30"), 30);

        final UserModel user = userSession.getUser();
        final String identifier = user.getId();
        final String userType = user.getFirstAttribute("user_type");

        if (StringUtil.isNullOrEmpty(identifier) || StringUtil.isNullOrEmpty(userType)) {
            return;
        }

        try {
            LOG.info("Fetching user access for userId=" + identifier + ", userType=" + userType);
            ExternalUserAccessService.ApiResult api =
                    ExternalUserAccessService.fetchUserAccess(
                            session, apiBase, identifier, userType,
                            tokenUrl, clientId, clientSecret, timeoutMs, skewSec
                    );

            if (api.body() != null) {
                // Respect mapper UI "Token Claim Name"/"JSON Type" settings:
                OIDCAttributeMapperHelper.mapClaim(token, cfg, api.body());
                // Also mirror under configured claimName to guarantee presence:
                token.getOtherClaims().put(claimName, MAPPER.convertValue(api.body(), java.util.Map.class));
            }
        } catch (Exception e) {
            // Fail-soft: do not block token issuance
            e.printStackTrace();
        }
    }

    private static String require(ProtocolMapperModel cfg, String key) {
        String v = cfg.getConfig().get(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing required config: " + key);
        return v.trim();
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static String firstNonBlank(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
