package com.keycloak.mappers;


import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.utils.StringUtil;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Keycloak OIDC Protocol Mapper that reads a single value from the incoming token request and maps it
 * into a token claim.
 * </p>
 *
 * <p>
 * The value can be read from either the request body (form parameter) or from a request header,
 * </p>
 */
public class RequestParamToClaimMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final Logger LOG = Logger.getLogger(RequestParamToClaimMapper.class);

    public static final String PROVIDER_ID = "request-param-to-claim-mapper";

    // Custom configs
    public static final String CONF_REQUEST_PARAM_NAME = "requestParamName";
    public static final String CONF_VALUE_SOURCE = "valueSource";

    public static final String SOURCE_BODY = "body";
    public static final String SOURCE_HEADER = "header";


    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        // --- Standard/default mapper configs
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(CONFIG_PROPERTIES);
        OIDCAttributeMapperHelper.addJsonTypeConfig(CONFIG_PROPERTIES);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(CONFIG_PROPERTIES, RequestParamToClaimMapper.class);

        ProviderConfigProperty source = new ProviderConfigProperty();
        source.setName(CONF_VALUE_SOURCE);
        source.setLabel("Read value from");
        source.setHelpText("Select where to read the value from. Default is request body.");
        source.setType(ProviderConfigProperty.LIST_TYPE);
        source.setOptions(List.of(SOURCE_BODY, SOURCE_HEADER));
        source.setDefaultValue(SOURCE_BODY);
        CONFIG_PROPERTIES.add(source);

        // --- Custom: request param name ---
        ProviderConfigProperty reqParam = new ProviderConfigProperty();
        reqParam.setName(CONF_REQUEST_PARAM_NAME);
        reqParam.setLabel("Request parameter name");
        reqParam.setHelpText("Form/Header parameter name to read from the token endpoint (e.g. tenant-id, patientId).");
        reqParam.setType(ProviderConfigProperty.STRING_TYPE);
        reqParam.setRequired(true);
        CONFIG_PROPERTIES.add(reqParam);
    }

    /**
     * Unique provider ID for this mapper (used for SPI registration and identification).
     *
     * @return provider ID string
     */
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * Display name shown in the Admin Console when selecting mappers.
     *
     * @return display type
     */
    @Override
    public String getDisplayType() {
        return "Request Param To Token Claim";
    }

    /**
     * Category under which this mapper appears in the Admin Console.
     *
     * @return display category
     */
    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    /**
     * Help text shown in the Admin Console describing what this mapper does.
     *
     * @return help text
     */
    @Override
    public String getHelpText() {
        return "Read a value from the request and adds it in token claim.";
    }

    /**
     * Configuration properties rendered in the Admin Console for this mapper.
     *
     * @return list of config properties
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    /**
     * Adds a claim to the access token by reading a configured parameter name from either the request body or HTTP headers
     *
     * @param token            current access token being built
     * @param mappingModel     mapper configuration model
     * @param session          Keycloak session
     * @param userSession      user session model
     * @param clientSessionCtx client session context
     * @return access token (possibly with an additional claim)
     */
    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        String paramName = mappingModel.getConfig().get(CONF_REQUEST_PARAM_NAME);
        if (StringUtil.isNullOrEmpty(paramName)) {
            // If not configured, do nothing (safe behavior)
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Mapper '%s' (%s): '%s' is missing/empty; skipping claim mapping.",
                        mappingModel.getName(), PROVIDER_ID, CONF_REQUEST_PARAM_NAME);
            }
            return token;
        }

        String source = mappingModel.getConfig().getOrDefault(CONF_VALUE_SOURCE, SOURCE_BODY);
        String value;
        if (SOURCE_HEADER.equalsIgnoreCase(source)) {
            value = getHeaderValue(session, paramName);
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Mapper '%s' (%s): reading from HEADER '%s' -> %s",
                        mappingModel.getName(), PROVIDER_ID, paramName, value == null ? "<null>" : "<present>");
            }
        } else {
            value = getFormParam(session, paramName);
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Mapper '%s' (%s): reading from BODY param '%s' -> %s",
                        mappingModel.getName(), PROVIDER_ID, paramName, value == null ? "<null>" : "<present>");
            }
        }


        if (value == null || value.isBlank()) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Mapper '%s' (%s): no value found for '%s' (source=%s); skipping claim mapping.",
                        mappingModel.getName(), PROVIDER_ID, paramName, source);
            }
            return token;
        }

        // Uses standard helper behavior: claim name, JSON type, etc. are configurable
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, value);

        if (LOG.isDebugEnabled()) {
            String claimName = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
            LOG.debugf("Mapper '%s' (%s): mapped claim '%s' from %s '%s'.",
                    mappingModel.getName(), PROVIDER_ID, claimName, source, paramName);
        }

        return token;
    }

    /**
     * Reads a single value from decoded form parameters of the current HTTP request.
     *
     * @param session Keycloak session used to access request context
     * @param name    form parameter name
     * @return first value of the form parameter, or {@code null} if unavailable
     */
    private String getFormParam(KeycloakSession session, String name) {
        if (session == null || session.getContext() == null || session.getContext().getHttpRequest() == null) {
            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Request context is not available while reading form parameters; returning null.");
        }

        MultivaluedMap<String, String> formParams;
        try {
            formParams = session.getContext().getHttpRequest().getDecodedFormParameters();
        } catch (Exception e) {
            LOG.debugf(e, "Failed to decode form parameters; returning null.");
            return null;
        }

        if (formParams == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Decoded form parameters are null; returning null.");
            }
            return null;
        }

        // Prefer first value if multiple
        return formParams.getFirst(name);
    }

    /**
     * Reads a header value from the current HTTP request.
     *
     * @param session    Keycloak session used to access request context
     * @param headerName header name
     * @return header value, or {@code null} if unavailable
     */
    private String getHeaderValue(KeycloakSession session, String headerName) {
        if (session == null || session.getContext() == null || session.getContext().getHttpRequest() == null) {
            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Request context is not available while reading header; returning null.");
        }

        try {
            return session.getContext()
                    .getHttpRequest().getHttpHeaders()
                    .getHeaderString(headerName);
        } catch (Exception e) {
            LOG.debugf(e, "Failed to read header '%s'; returning null.", headerName);
            return null;
        }
    }
}

