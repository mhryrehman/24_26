package cc.coopersoft.keycloak.phone.providers.sender;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class KeycloakCustomTeleMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(KeycloakCustomTeleMapper.class);

    public static final String PROVIDER_ID = "oidc-custom-token-tele-mapper";

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, KeycloakCustomTeleMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return "Custom Token Tele mapper";
    }

    @Override
    public String getDisplayType() {
        return "Custom Token Tele mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds attributes to claims for Tele Meeting";
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

        MultivaluedMap<String, String> headers = keycloakSession.getContext().getHttpRequest().getDecodedFormParameters();

        if(null !=  headers.getFirst("appointment_id")) {

            token.getOtherClaims().put("appointment_id", headers.getFirst("appointment_id"));

            token.getOtherClaims().put("participant_id", headers.getFirst("participant_id"));

            token.getOtherClaims().put("meeting_id", headers.getFirst("meeting_id"));

            String expiryDays = headers.getFirst("expiry_days");

            if (expiryDays != null && !expiryDays.isEmpty()) {
                try {
                    long expiryDaysValue = Long.parseLong(expiryDays);

                    if (expiryDaysValue >= 0) {

                        Calendar calendar = Calendar.getInstance();

                        calendar.add(Calendar.DAY_OF_MONTH, ((int) expiryDaysValue));

                        long newExpiryInSeconds = calendar.getTimeInMillis() / 1000L;

                        token.exp(newExpiryInSeconds);

                        logger.info("New Expiry Date set to: " + calendar.getTime());
                    } else {
                        logger.info("Expiry days value is negative, no action taken.");
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid expiry_days value "+e);
                }

            }

        }


    }
}
