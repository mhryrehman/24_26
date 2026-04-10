package cc.coopersoft.keycloak.phone.providers.sender;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TokenExchangeCustomMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(TokenExchangeCustomMapper.class);

    public static final String PROVIDER_ID = "oidc-custom-token-exchange-mapper";

    static {
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, TokenExchangeCustomMapper.class);
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName("userinfo-url");
        property.setLabel("userinfo-url");
        property.setHelpText("User info url to get info from auth server");
        property.setType("String");
        configProperties.add(property);
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

        String userInfoUrl = getUserInfoEndpoint(mappingModel, headers);

        String subjectToken = headers.getFirst("subject_token");
        Map<String, Object> userInfo = callUserInfoEndpoint(subjectToken, keycloakSession, userInfoUrl);

        if (userInfo != null) {
            logger.infov("Setting UserInfo claims: {0}", userInfo);

            if (user.getFirstAttribute("user_type") == null) {
                setUserAttributes(user, userInfo);
                List<String> userRoles = new ArrayList<>();
                userRoles.add(String.valueOf(userInfo.get("EntityType")));
                token.getOtherClaims().put("user_roles", userRoles);
            }
            setTokenClaims(token, userInfo);
        } else {
            logger.error("Failed to retrieve user info from the endpoint.");
        }

    }

    private void setUserAttributes(UserModel user, Map<String, Object> userInfo) {
        user.setSingleAttribute("user_type", "Practitioner");
        user.setSingleAttribute("organization_url", cleanPracticeApiUrl(userInfo));
        user.setSingleAttribute("user_roles", String.valueOf(userInfo.get("EntityType")));
        user.setSingleAttribute("vendor_id", String.valueOf(userInfo.get("EntityID")));
        user.setSingleAttribute("UserId", String.valueOf(userInfo.get("UserId")));
        user.setSingleAttribute("PracticeId", String.valueOf(userInfo.get("PracticeId")));
        user.setSingleAttribute("electronic_claim_id", String.valueOf(userInfo.get("ElectronicClaimId")));
        user.setSingleAttribute("email_verified", "true");
        user.setSingleAttribute("first_name", String.valueOf(userInfo.get("given_name")));
        user.setSingleAttribute("last_name", String.valueOf(userInfo.get("family_name")));
        user.setSingleAttribute("user_name", String.valueOf(userInfo.get("UserName")));
        user.setEmailVerified(true);
    }


    private void setTokenClaims(IDToken token, Map<String, Object> userInfo) {
        token.getOtherClaims().put("user_type", "Practitioner");
        token.getOtherClaims().put("organization_url", cleanPracticeApiUrl(userInfo));

        token.getOtherClaims().put("vendor_id", String.valueOf(userInfo.get("EntityID")));
        token.getOtherClaims().put("UserId", String.valueOf(userInfo.get("UserId")));
        token.getOtherClaims().put("PracticeId", String.valueOf(userInfo.get("PracticeId")));
        token.getOtherClaims().put("electronic_claim_id", String.valueOf(userInfo.get("ElectronicClaimId")));
        token.getOtherClaims().put("email_verified", "true");
        token.getOtherClaims().put("first_name", String.valueOf(userInfo.get("given_name")));
        token.getOtherClaims().put("last_name", String.valueOf(userInfo.get("family_name")));
        token.getOtherClaims().put("user_name", String.valueOf(userInfo.get("UserName")));
    }

    private Map<String, Object> callUserInfoEndpoint(String accessToken, KeycloakSession session, String userInfoURL) {
        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();

        if (userInfoURL == null) {
            logger.error("Unable to fetch the configuration of user info url....");
            return null;
        }

        HttpGet request = new HttpGet(userInfoURL);
        request.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            logger.info("Response received from info end point ..." + response.getStatusLine().getStatusCode());
            return JsonSerialization.readValue(response.getEntity().getContent(), Map.class);
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


    private String getUserInfoEndpoint(ProtocolMapperModel mapperModel, MultivaluedMap<String, String> headers) {
        String url = mapperModel.getConfig().get("userinfo-url");
        String idpID = headers.getFirst("subject_issuer");
        String userInfoURL = null;
        if (StringUtil.isNullOrEmpty(url)) {
            logger.error("URL configuration not found.....");
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode bundleNode = objectMapper.readTree(url);
            if (bundleNode.has(idpID)) {
                userInfoURL = bundleNode.get(idpID).asText();

            }
        } catch (JsonProcessingException e) {
            logger.error("Exception while fetching url " + e.getMessage());
        }
        return userInfoURL;
    }
}
