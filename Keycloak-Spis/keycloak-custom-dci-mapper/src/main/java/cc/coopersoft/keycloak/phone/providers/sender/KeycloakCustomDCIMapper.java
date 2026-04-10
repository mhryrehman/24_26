package cc.coopersoft.keycloak.phone.providers.sender;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;


public class KeycloakCustomDCIMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(KeycloakCustomDCIMapper.class);

    public static final String PROVIDER_ID = "oidc-custom-token-dci-mapper";

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, KeycloakCustomDCIMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return "Custom Token DCI mapper";
    }

    @Override
    public String getDisplayType() {
        return "Custom Token DCI mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds attributes to claims for DCI/Meeting";
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



        String meetingID = headers.getFirst("meeting-id");
        String AppointmentID = headers.getFirst("appointment-id");
        String userID = headers.getFirst("user-id");

        if (null != AppointmentID) {
            logger.info("Setting claims for DCI");
            token.getOtherClaims().put("appointment-id", AppointmentID);
        }
        if (null != userID) {
            token.getOtherClaims().put("user-id", userID);
        }
        if (null != meetingID) {
            token.getOtherClaims().put("meeting-id", meetingID);
        }

    }
}
