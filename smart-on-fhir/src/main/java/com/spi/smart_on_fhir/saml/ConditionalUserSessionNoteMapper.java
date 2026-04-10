package com.spi.smart_on_fhir.saml;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.MapperTypeSerializer;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.*;

/**
 * <h2>ConditionalUserSessionNoteMapper</h2>
 *
 * <p>
 * A custom OIDC Protocol Mapper that conditionally copies values from
 * {@link UserSessionModel} notes into token claims.
 * </p>
 *
 * <p>
 * Intended for SMART-on-FHIR launch flows or similar conditional claim scenarios.
 * </p>
 */
public class ConditionalUserSessionNoteMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper,
        OIDCIDTokenMapper,
        OIDCAccessTokenResponseMapper,
        UserInfoTokenMapper,
        TokenIntrospectionTokenMapper {

    private static final Logger LOG = Logger.getLogger(ConditionalUserSessionNoteMapper.class);

    public static final String PROVIDER_ID = "conditional-note-to-claim";

    private static final String MAPPINGS_CONFIG = "noteToClaimMap";
    private static final String LAUNCH_NOTE_CONFIG = "launch.note";
    private static final String AUTH_REQUEST_PARAM_CONFIG = "auth.request.param";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty mapProp = new ProviderConfigProperty();
        mapProp.setName(MAPPINGS_CONFIG);
        mapProp.setLabel("Session Note to Token Claim Mapping");
        mapProp.setType(ProviderConfigProperty.MAP_TYPE);
        CONFIG_PROPERTIES.add(mapProp);

        ProviderConfigProperty launchNote = new ProviderConfigProperty();
        launchNote.setName(LAUNCH_NOTE_CONFIG);
        launchNote.setLabel("Launch Session Note Key");
        launchNote.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(launchNote);

        ProviderConfigProperty reqParam = new ProviderConfigProperty();
        reqParam.setName(AUTH_REQUEST_PARAM_CONFIG);
        reqParam.setLabel("Authorization Request Parameter");
        reqParam.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(reqParam);

        OIDCAttributeMapperHelper.addAttributeConfig(CONFIG_PROPERTIES, ConditionalUserSessionNoteMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Conditional Session Note to Token Claim";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Conditionally maps user session notes to token claims when the "
                + "authorization request parameter matches the configured launch session note.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    // =========================================================
    // Token Transformation Methods
    // =========================================================

    /**
     * Called when Keycloak generates an Access Token.
     *
     * @param token the AccessToken being built
     * @param mappingModel mapper configuration
     * @param session Keycloak session
     * @param userSession current user session
     * @param clientSessionCtx client session context
     * @return updated AccessToken
     */
    @Override
    public AccessToken transformAccessToken(AccessToken token,
                                            ProtocolMapperModel mappingModel,
                                            KeycloakSession session,
                                            UserSessionModel userSession,
                                            ClientSessionContext clientSessionCtx) {
        LOG.infof("transformAccessToken: userSession=%s, mapper=%s", userSession == null ? "null" : userSession.getId(),
                mappingModel == null ? "null" : mappingModel.getId());
        return applyConditionalMapping(token, mappingModel, session, userSession, clientSessionCtx);
    }

    /**
     * Called when Keycloak generates an ID Token.
     */
    @Override
    public IDToken transformIDToken(IDToken token,
                                    ProtocolMapperModel mappingModel,
                                    KeycloakSession session,
                                    UserSessionModel userSession,
                                    ClientSessionContext clientSessionCtx) {
        LOG.infof("transformIDToken: userSession=%s, mapper=%s", userSession == null ? "null" : userSession.getId(),
                mappingModel == null ? "null" : mappingModel.getId());
        return applyConditionalMapping(token, mappingModel, session, userSession, clientSessionCtx);
    }

    /**
     * Called when Keycloak builds the token endpoint response.
     */
    @Override
    public AccessTokenResponse transformAccessTokenResponse(AccessTokenResponse response,
                                                            ProtocolMapperModel mappingModel,
                                                            KeycloakSession session,
                                                            UserSessionModel userSession,
                                                            ClientSessionContext clientSessionCtx) {
        LOG.infof("transformAccessTokenResponse: userSession=%s, mapper=%s", userSession == null ? "null" : userSession.getId(),
                mappingModel == null ? "null" : mappingModel.getId());

        if (mappingModel == null) {
            LOG.info("transformAccessTokenResponse: no mappingModel present, skipping.");
            return response;
        }

        if (userSession == null) {
            LOG.info("transformAccessTokenResponse: no userSession present, skipping.");
            return response;
        }

        if (!shouldApplyMappings(mappingModel, session, userSession, clientSessionCtx)) {
            LOG.info("transformAccessTokenResponse: condition not met, skipping mapping.");
            return response;
        }

        Map<String, List<String>> mappings = readMapConfig(mappingModel);
        LOG.infof("transformAccessTokenResponse: applying %d mappings", mappings.size());
        for (Map.Entry<String, List<String>> e : mappings.entrySet()) {
            String noteValue = userSession.getNote(e.getKey());
            if (noteValue == null) {
                LOG.infof("transformAccessTokenResponse: session note '%s' not present, skipping.", e.getKey());
                continue;
            }

            String claimName = e.getValue().get(0);
            ProtocolMapperModel tmp = createTempMapperModel(mappingModel, claimName);

            OIDCAttributeMapperHelper.mapClaim(response, tmp, noteValue);
            LOG.infof("transformAccessTokenResponse: added claim '%s'='%s' from session note '%s'",
                    claimName, noteValue, e.getKey());
        }

        return response;
    }

    /**
     * Called for the UserInfo endpoint.
     */
    @Override
    public AccessToken transformUserInfoToken(AccessToken token,
                                              ProtocolMapperModel mappingModel,
                                              KeycloakSession session,
                                              UserSessionModel userSession,
                                              ClientSessionContext clientSessionCtx) {
        LOG.infof("transformUserInfoToken: userSession=%s, mapper=%s", userSession == null ? "null" : userSession.getId(),
                mappingModel == null ? "null" : mappingModel.getId());
        return applyConditionalMapping(token, mappingModel, session, userSession, clientSessionCtx);
    }

    /**
     * Called for Token Introspection endpoint.
     */
    @Override
    public AccessToken transformIntrospectionToken(AccessToken token,
                                                   ProtocolMapperModel mappingModel,
                                                   KeycloakSession session,
                                                   UserSessionModel userSession,
                                                   ClientSessionContext clientSessionCtx) {
        LOG.infof("transformIntrospectionToken: userSession=%s, mapper=%s", userSession == null ? "null" : userSession.getId(),
                mappingModel == null ? "null" : mappingModel.getId());
        return applyConditionalMapping(token, mappingModel, session, userSession, clientSessionCtx);
    }

    // =========================================================
    // Core Conditional Logic
    // =========================================================

    /**
     * Applies session-note to claim mapping if conditional match succeeds.
     */
    @SuppressWarnings("unchecked")
    private <T> T applyConditionalMapping(T token,
                                          ProtocolMapperModel mappingModel,
                                          KeycloakSession session,
                                          UserSessionModel userSession,
                                          ClientSessionContext clientSessionCtx) {

        LOG.infof("applyConditionalMapping: userSession=%s, mapper=%s", userSession == null ? "null" : userSession.getId(),
                mappingModel == null ? "null" : mappingModel.getId());

        if (mappingModel == null) {
            LOG.info("applyConditionalMapping: no mappingModel present, skipping.");
            return token;
        }

        if (userSession == null) {
            LOG.info("applyConditionalMapping: no userSession present, skipping.");
            return token;
        }
        if (!shouldApplyMappings(mappingModel, session, userSession, clientSessionCtx)) {
            LOG.info("applyConditionalMapping: condition not met, skipping mappings.");
            return token;
        }

        Map<String, List<String>> mappings = readMapConfig(mappingModel);
        LOG.infof("applyConditionalMapping: found %d mapping entries", mappings.size());

        for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
            String sessionNoteKey = entry.getKey();
            List<String> claimNames = entry.getValue();
            if (claimNames == null || claimNames.isEmpty()) {
                LOG.infof("applyConditionalMapping: ignoring invalid mapping for sessionNoteKey='%s'", sessionNoteKey);
                continue;
            }

            String claimName = claimNames.get(0);
            String noteValue = userSession.getNote(sessionNoteKey);
            if (noteValue == null) {
                LOG.infof("applyConditionalMapping: session note '%s' not present; skipping claim '%s'",
                        sessionNoteKey, claimName);
                continue;
            }

            ProtocolMapperModel tmp = createTempMapperModel(mappingModel, claimName);

            // Dispatch to correct overload
            if (token instanceof IDToken) {
                OIDCAttributeMapperHelper.mapClaim((IDToken) token, tmp, noteValue);
                LOG.infof("applyConditionalMapping: added IDToken claim '%s'='%s' from session note '%s'",
                        claimName, noteValue, sessionNoteKey);
            } else if (token instanceof AccessTokenResponse) {
                OIDCAttributeMapperHelper.mapClaim((AccessTokenResponse) token, tmp, noteValue);
                LOG.infof("applyConditionalMapping: added AccessTokenResponse claim '%s'='%s' from session note '%s'",
                        claimName, noteValue, sessionNoteKey);
            } else {
                // Shouldn't happen for Keycloak token types; log and skip
                LOG.warnf("Unsupported token type for mapping: %s", token.getClass().getName());
            }
        }

        return token;
    }


    /**
     * Validates whether the authorization request parameter matches the session launch note.
     */
    private boolean shouldApplyMappings(ProtocolMapperModel mappingModel,
                                        KeycloakSession session,
                                        UserSessionModel userSession,
                                        ClientSessionContext clientSessionCtx) {

        String launchNoteKey = mappingModel.getConfig().getOrDefault(LAUNCH_NOTE_CONFIG, "SMART_launch");
        String authParam = mappingModel.getConfig().getOrDefault(AUTH_REQUEST_PARAM_CONFIG, "launch");

        String sessionLaunch = userSession.getNote(launchNoteKey);
        if (sessionLaunch == null) {
            LOG.infof("shouldApplyMappings: session launch note '%s' not present, skipping.", launchNoteKey);
            return false;
        }

        String requestLaunch = null;

        if (clientSessionCtx != null && clientSessionCtx.getClientSession() != null) {
            requestLaunch = clientSessionCtx.getClientSession()
                    .getNote("client_request_param_" + authParam);
            LOG.infof("shouldApplyMappings: found client session note for param '%s' = '%s'", authParam, requestLaunch);
        }

        if (requestLaunch == null && session != null) {
            AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
            if (authSession != null) {
                requestLaunch = authSession.getClientNote(authParam);
                LOG.infof("shouldApplyMappings: found auth session client note for param '%s' = '%s'", authParam, requestLaunch);
            }
        }

        boolean matches = sessionLaunch.equals(requestLaunch);
        if (matches) {
            LOG.infof("shouldApplyMappings: launch matched (session='%s', request='%s')", sessionLaunch, requestLaunch);
        } else {
            LOG.infof("shouldApplyMappings: launch did NOT match (session='%s', request='%s')", sessionLaunch, requestLaunch);
        }
        return matches;
    }

    /**
     * Reads MAP_TYPE configuration from mapper model.
     */
    private Map<String, List<String>> readMapConfig(ProtocolMapperModel mappingModel) {
        String raw = mappingModel.getConfig().get(MAPPINGS_CONFIG);
        LOG.infof("readMapConfig: raw config length=%d for mapper=%s", raw == null ? 0 : raw.length(),
                mappingModel.getId());
        return raw != null ? MapperTypeSerializer.deserialize(raw) : Collections.emptyMap();
    }

    /**
     * Creates a temporary mapper model for a specific claim.
     * Used to preserve include flags and JSON type configuration.
     */
    private ProtocolMapperModel createTempMapperModel(ProtocolMapperModel original, String claimName) {
        ProtocolMapperModel tmp = new ProtocolMapperModel();
        tmp.setProtocol(original.getProtocol());
        tmp.setProtocolMapper(original.getProtocolMapper());

        Map<String, String> cfg = new HashMap<>(original.getConfig());
        cfg.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, claimName);
        tmp.setConfig(cfg);

        LOG.infof("createTempMapperModel: created temp mapper for claim '%s' using original mapper=%s",
                claimName, original.getId());
        return tmp;
    }
}
