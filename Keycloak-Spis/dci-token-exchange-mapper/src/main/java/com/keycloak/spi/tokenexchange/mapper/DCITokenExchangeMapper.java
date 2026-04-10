package com.keycloak.spi.tokenexchange.mapper;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Custom protocol mapper to decode an external subject_token during token exchange
 * and add specific claims to the resulting Keycloak-issued token.
 */
public class DCITokenExchangeMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "dci-token-exchange-mapper";
    private static final Logger logger = Logger.getLogger(DCITokenExchangeMapper.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final long DOTNET_EPOCH_TICKS = 621355968000000000L; // 1970-01-01
    private static final long TICKS_PER_MILLI = 10_000L;
    private static final String CFG_EXPIRY_DAYS = "expiry_days";


    static {
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, DCITokenExchangeMapper.class);

        ProviderConfigProperty expiryDays = new ProviderConfigProperty();
        expiryDays.setName(CFG_EXPIRY_DAYS);
        expiryDays.setLabel("Expiry offset (days)");
        expiryDays.setHelpText("If set to a positive integer, the token expiry will be appointment_date + this many days. Leave blank or ≤0 to keep default expiry.");
        expiryDays.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(expiryDays);
    }

    @Override
    public String getDisplayCategory() {
        return "DCI Token Exchange Mapper";
    }

    @Override
    public String getDisplayType() {
        return "DCI Token Exchange Mapper";
    }

    @Override
    public String getHelpText() {
        return "Decodes the subject_token during token exchange and adds specific claims to the token.";
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
    protected void setClaim(IDToken token,
                            ProtocolMapperModel mappingModel,
                            UserSessionModel userSession,
                            KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx) {

        MultivaluedMap<String, String> formParams = keycloakSession.getContext().getHttpRequest().getDecodedFormParameters();
        String grantType = formParams.getFirst(Constants.GRANT_TYPE);

        if (!Constants.TOKEN_EXCHANGE_GRANT_TYPE.equals(grantType)) {
            return;
        }

        String subjectToken = formParams.getFirst(Constants.SUBJECT_TOKEN);
        if (StringUtil.isNullOrEmpty(subjectToken)) {
            logger.warn("Subject token is missing from token exchange request.");
            return;
        }

        logger.info("Decoding external subject_token in DCITokenExchangeMapper...");
        Map<String, Object> claims = decodeTokenClaims(subjectToken);

        if (claims != null) {
            logger.infov("Injecting decoded claims: {0}", claims);
            setTokenClaims(token, mappingModel, claims);
        } else {
            logger.error("Failed to decode subject token or extract claims.");
        }
    }

    private void setTokenClaims(IDToken token, ProtocolMapperModel mappingModel, Map<String, Object> claims) {

        putClaimIfExists(token, claims, Constants.CLAIM_APPOINTMENT_ID);
        putClaimIfExists(token, claims, Constants.CLAIM_PROVIDER_ID);
        putClaimIfExists(token, claims, Constants.CLAIM_CHECKIN_ROUTE);
        putClaimIfExists(token, claims, Constants.CLAIM_CHECKIN_STAGE);
        putClaimIfExists(token, claims, Constants.CLAIM_APPOINTMENT_DATE);
        putClaimIfExists(token, claims, Constants.CLAIM_START_DATE);
        //patient_id mapped to leap_id
        Object value = claims.get(Constants.CLAIM_PATIENT_ID);
        if (value != null) {
            token.getOtherClaims().put(Constants.CLAIM_LEAP_ID, String.valueOf(value));
        }

        int daysToAdd = getPositiveInt(mappingModel.getConfig(), CFG_EXPIRY_DAYS, 0);
        if (daysToAdd > 0) {
            long newExp = calculateExpFromAptDate(claims.get(Constants.CLAIM_APPOINTMENT_DATE), daysToAdd);
            if (newExp > 0) {
                logger.infof("Setting token expiry from appointment_date (+%dd): exp=%d", daysToAdd, newExp);
                token.exp(newExp);
            } else {
                logger.warnf("appointment_date invalid; leaving default expiry.");
            }
        } else {
            logger.debug("expiry_days not set or <= 0; leaving default expiry.");
        }
    }

    private void putClaimIfExists(IDToken token, Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (value != null) {
            token.getOtherClaims().put(claimName, String.valueOf(value));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeTokenClaims(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                logger.warn("Invalid JWT format: less than 2 parts.");
                return null;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return JsonSerialization.readValue(payload, Map.class);

        } catch (Exception e) {
            logger.errorf(e, "Failed to decode JWT claims from subject token.");
            return null;
        }
    }

    private long calculateExpFromAptDate(Object aptDate, int daysToAdd) {
        try {
            long ticks = Long.parseLong(String.valueOf(aptDate));
            long epochMillis = Math.floorDiv(ticks - DOTNET_EPOCH_TICKS, TICKS_PER_MILLI);
            long expSeconds = java.time.Instant.ofEpochMilli(epochMillis)
                    .plus(java.time.Duration.ofDays(daysToAdd))
                    .getEpochSecond();
            // Keycloak stores exp as int seconds
            if (expSeconds > Integer.MAX_VALUE) expSeconds = Integer.MAX_VALUE;
            if (expSeconds < 0) return 0;
            return expSeconds;
        } catch (Exception e) {
            logger.errorf(e, "Failed converting .NET ticks: %s", aptDate);
            return 0;
        }
    }

    private int getPositiveInt(Map<String, String> cfg, String key, int def) {
        if (cfg == null) return def;
        String v = cfg.get(key);
        if (v == null || v.trim().isEmpty()) return def;
        try {
            int n = Integer.parseInt(v.trim());
            return n > 0 ? n : def;
        } catch (NumberFormatException e) {
            logger.warnf("Invalid integer for %s: %s", key, v);
            return def;
        }
    }
}
