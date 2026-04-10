package com.keycloak.otp.auth.condition;

import com.keycloak.otp.util.Utils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.keycloak.otp.auth.condition.ConditionalClientAuthenticatorFactory.CFG_ALLOWED_CLIENT_IDS;
import static com.keycloak.otp.auth.condition.ConditionalClientAuthenticatorFactory.CFG_SESSION_NOTE_KEY;

public class ConditionalClientAuthenticator implements ConditionalAuthenticator {

    public static final ConditionalClientAuthenticator SINGLETON = new ConditionalClientAuthenticator();
    private static final Logger LOG = Logger.getLogger(ConditionalClientAuthenticator.class);

    private ConditionalClientAuthenticator() {
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        boolean result = evaluateCondition(context);
        final String sessionNoteKey = Utils.getConfigString(context.getAuthenticatorConfig(), CFG_SESSION_NOTE_KEY, null);

        if (!StringUtil.isNullOrEmpty(sessionNoteKey)) {
            LOG.infof("Condition – Client (list): session note key configured, setting auth note '%s'", sessionNoteKey);
            context.getAuthenticationSession().setAuthNote(sessionNoteKey, Boolean.toString(result));
        }

        return result;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // no-op
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    private boolean evaluateCondition(AuthenticationFlowContext context) {
        final RealmModel realm = context.getRealm();
        final ClientModel client = context.getAuthenticationSession().getClient();

        if (client == null) {
            LOG.info("Condition – Client (list): no client in context; returning FALSE");
            return false;
        }

        final String cfg = Utils.getConfigString(context.getAuthenticatorConfig(), CFG_ALLOWED_CLIENT_IDS, "");

        if (StringUtil.isNullOrEmpty(cfg)) {
            LOG.infof("Condition – Client (list): no '%s' configured in realm '%s'; returning FALSE", CFG_ALLOWED_CLIENT_IDS, realm != null ? realm.getName() : "unknown");
            return false;
        }

        final Set<String> allowed = Arrays.stream(cfg.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        final String current = client.getClientId();
        final boolean match = allowed.contains(current);

        LOG.infof("Condition – Client (list): allowed=%s current=%s match=%s", allowed, current, match);
        return match;
    }
}
