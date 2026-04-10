package cc.coopersoft.keycloak.phone.providers.sender;

import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.credential.dto.PasswordSecretData;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.Config.Scope;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

public class SHA256PasswordHashProvider implements PasswordHashProvider {

    private static final String ID = "sha-256";

    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        return ID.equals(credential.getPasswordCredentialData().getAlgorithm());
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        String encodedPassword = encode(rawPassword, iterations);
        PasswordCredentialData credentialData = new PasswordCredentialData(iterations, null);
        PasswordSecretData secretData = null;
        try {
            secretData = new PasswordSecretData(encodedPassword, null, Collections.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return PasswordCredentialModel.createFromValues(credentialData, secretData);
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        String encodedPassword = encode(rawPassword, credential.getPasswordCredentialData().getHashIterations());
        return encodedPassword.equals(credential.getPasswordSecretData().getValue());
    }

    @Override
    public String encode(String rawPassword, int iterations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = rawPassword.getBytes();
            for (int i = 0; i < iterations; i++) {
                hash = digest.digest(hash);
            }
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public void close() {
    }

    public static class Factory implements PasswordHashProviderFactory {
        @Override
        public PasswordHashProvider create(KeycloakSession session) {
            return new SHA256PasswordHashProvider();
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public void init(Scope config) {
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
        }

        @Override
        public void close() {
        }
    }
}
