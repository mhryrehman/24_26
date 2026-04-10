package org.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

public class InvalidateSessionOnPasswordChangeListener implements EventListenerProvider {

    private final KeycloakSession session;
    private static final Logger logger = Logger.getLogger(InvalidateSessionOnPasswordChangeListener.class);


    public InvalidateSessionOnPasswordChangeListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {

        if (event.getType() == EventType.RESET_PASSWORD || event.getType() == EventType.UPDATE_PASSWORD) {

            logger.info("Sessions for: " + event.getType());

            UserModel user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());
            if (user != null) {
                session.sessions().getUserSessionsStream(session.getContext().getRealm(), user)
                        .forEach(userSession -> session.sessions().removeUserSession(session.getContext().getRealm(), userSession));
               logger.info("Sessions invalidated for: " + user.getUsername());
            }
        }
    }
    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        if (adminEvent.getOperationType() == OperationType.ACTION && adminEvent.getResourceTypeAsString().equals("USER")) {
            if (adminEvent.getResourcePath().endsWith("reset-password")) {
                String userId = adminEvent.getResourcePath().split("/")[1];
                UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);

                if (user != null) {
                    session.sessions().getUserSessionsStream(session.getContext().getRealm(), user)
                            .forEach(userSession -> session.sessions().removeUserSession(session.getContext().getRealm(), userSession));

                    logger.info("Sessions invalidated for user: " + user.getUsername());
                }
            }
        }
    }

    @Override
    public void close() {
    }
}
