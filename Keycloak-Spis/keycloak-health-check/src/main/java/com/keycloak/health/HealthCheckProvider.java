package com.keycloak.health;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class HealthCheckProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public HealthCheckProvider(KeycloakSession session) {
        this.session = session;
    }


    @Override
    public Object getResource() {
        return new HealthCheckEndpoint(session);
    }

    @Override
    public void close() {
    }
}
