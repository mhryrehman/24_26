package com.keycloak.otp.logut;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ForceLogoutResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "force-logout"; // endpoint = /realms/{realm}/force-logout

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new ForceLogoutResourceProvider(session);
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public String getId() {
        return ID;
    }
}
