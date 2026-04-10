package com.keycloak.health;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class HealthCheckProviderFactory implements RealmResourceProviderFactory {
    public static final String ID = "health";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new HealthCheckProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        ConfigLoader.init(config);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }
}