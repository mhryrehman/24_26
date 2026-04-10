package cc.coopersoft.keycloak.phone.providers.sender;


import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;

public class TokenExchangeCustomMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(TokenExchangeCustomMapper.class);

    public static final String PROVIDER_ID = "oidc-custom-token-exchange-mapper";

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, TokenExchangeCustomMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return "Custom Token Exchange mapper";
    }

    @Override
    public String getDisplayType() {
        return "Custom Token Exchange mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds attributes to claims, retrieves user info, and sets user attributes if they don't exist.";
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
    protected void setClaim(final IDToken token,
                            final ProtocolMapperModel mappingModel,
                            final UserSessionModel userSession,
                            final KeycloakSession keycloakSession,
                            final ClientSessionContext clientSessionCtx) {

        UserModel user = userSession.getUser();
        MultivaluedMap<String, String> headers = keycloakSession.getContext().getHttpRequest().getDecodedFormParameters();
        String grantType = headers.getFirst("grant_type");


        if (!"urn:ietf:params:oauth:grant-type:token-exchange".equals(grantType)) {
            return;
        }
        logger.info("TokenExchangeCustomMapper Going to call info details...");

        String subjectToken = headers.getFirst("subject_token");
        if (subjectToken == null || subjectToken.isBlank()) {
            logger.warn("Missing/blank subject_token header; skipping userinfo mapping");
            return;
        }

        Map<String, Object> userInfo = callUserInfoEndpoint(subjectToken, keycloakSession);
        if (userInfo == null || userInfo.isEmpty()) {
            logger.error("Failed to retrieve user info from the endpoint or it was empty.");
            return;
        }

        logger.infov("Setting UserInfo claims: {0}", userInfo);
        setUserAttributes(user, userInfo);
        setTokenClaims(token, userInfo);
        mergeUserRolesAndSetToken(user, userInfo, token);
    }

    private void mergeUserRolesAndSetToken(UserModel user, Map<String, Object> userInfo, final IDToken token) {
        if (user == null || userInfo == null || token == null) {
            logger.warn("Skipping role merge due to null input(s)");
            return;
        }

        List<String> existingRoles = user.getAttributes().getOrDefault("user_roles", Collections.emptyList());
        Set<String> mergedRoles = new LinkedHashSet<>(existingRoles);

        Object entityType = userInfo.get("EntityType");
        if (entityType != null && !String.valueOf(entityType).isBlank()) {
            mergedRoles.add(String.valueOf(entityType));
        }

        List<String> mergedRolesList = new ArrayList<>(mergedRoles);
        user.setAttribute("user_roles", mergedRolesList);
        token.getOtherClaims().put("user_roles", mergedRolesList);

        logger.infov("Merged user_roles set on user and token: {0}", mergedRolesList);
    }

    private void setUserAttributes(UserModel user, Map<String, Object> userInfo) {
        setUserSingleIfNotBlank(user, "organization_url", cleanPracticeApiUrl(userInfo));
        setUserSingleFromMapIfPresent(user, userInfo, "vendor_id", "EntityID");
        setUserSingleFromMapIfPresent(user, userInfo, "UserId", "UserId");
        setUserSingleFromMapIfPresent(user, userInfo, "PracticeId", "PracticeId");
        setUserSingleFromMapIfPresent(user, userInfo, "electronic_claim_id", "ElectronicClaimId");
        user.setSingleAttribute("email_verified", "true");
        setUserSingleFromMapIfPresent(user, userInfo, "first_name", "given_name");
        setUserSingleFromMapIfPresent(user, userInfo, "last_name", "family_name");
        setUserSingleFromMapIfPresent(user, userInfo, "user_name", "UserName");
        user.setEmailVerified(true);
    }

    private void setTokenClaims(IDToken token, Map<String, Object> userInfo) {
        setClaimIfNotBlank(token, "organization_url", cleanPracticeApiUrl(userInfo));
        setClaimFromMapIfPresent(token, userInfo, "vendor_id", "EntityID");
        setClaimFromMapIfPresent(token, userInfo, "UserId", "UserId");
        setClaimFromMapIfPresent(token, userInfo, "PracticeId", "PracticeId");
        setClaimFromMapIfPresent(token, userInfo, "electronic_claim_id", "ElectronicClaimId");
        token.getOtherClaims().put("email_verified", "true");
        setClaimFromMapIfPresent(token, userInfo, "first_name", "given_name");
        setClaimFromMapIfPresent(token, userInfo, "last_name", "family_name");
        setClaimFromMapIfPresent(token, userInfo, "user_name", "UserName");

    }

    private Map<String, Object> callUserInfoEndpoint(String accessToken, KeycloakSession session) {
        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();

        String userInfoUrl = System.getenv("USERINFO_ENDPOINT_URL");

        if (userInfoUrl == null) {
            logger.warn("USERINFO_ENDPOINT_URL is not set");
            return null;
        }

        HttpGet request = new HttpGet(userInfoUrl);
        request.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            logger.info("Response received from userinfo end point : " + status);
            if (status == 200) {
                return JsonSerialization.readValue(response.getEntity().getContent(), Map.class);
            } else {
                logger.errorf("Failed to retrieve valid json response from UserInfo endpoint : %s", EntityUtils.toString(response.getEntity()));
                return null;
            }
        } catch (IOException e) {
            logger.error("Failed to call userinfo endpoint", e);
            return null;
        }
    }

    private String cleanPracticeApiUrl(Map<String, Object> userInfo) {
        String practiceApiUrl = String.valueOf(userInfo.get("practiceapiurl"));
        if (practiceApiUrl != null && practiceApiUrl.endsWith("/api")) {
            return practiceApiUrl.substring(0, practiceApiUrl.length() - 4);
        }
        return practiceApiUrl;
    }

    private void setUserSingleFromMapIfPresent(UserModel user, Map<String, Object> map, String targetAttr, String sourceKey) {
        if (!map.containsKey(sourceKey)) return;
        setUserSingleIfNotBlank(user, targetAttr, toNonBlankString(map.get(sourceKey)));
    }

    private void setClaimFromMapIfPresent(IDToken token, Map<String, Object> map, String claim, String sourceKey) {
        if (!map.containsKey(sourceKey)) return;
        setClaimIfNotBlank(token, claim, toNonBlankString(map.get(sourceKey)));
    }

    private void setUserSingleIfNotBlank(UserModel user, String key, String value) {
        if (value == null || value.isBlank()) return;
        user.setSingleAttribute(key, value);
    }

    private void setClaimIfNotBlank(IDToken token, String key, String value) {
        if (value == null || value.isBlank()) return;
        token.getOtherClaims().put(key, value);
    }

    /**
     * Converts to String safely but returns null if:
     * - value is null
     * - value stringifies to "null"
     * - trimmed result is blank
     */
    private String toNonBlankString(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        return s;
    }

}
