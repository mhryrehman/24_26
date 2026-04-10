package com.keycloak.otp.events;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.*;

public class EventWebhookListenerProviderFactory implements EventListenerProviderFactory {
    private static final Logger logger = Logger.getLogger(EventWebhookListenerProviderFactory.class);

    public static final String PROVIDER_ID = "curemd-event-webhook";
    private String webhookUrl;
    private ExecutorService executor;
    private ObjectMapper mapper;

    @Override
    public void init(Scope config) {
        this.webhookUrl = config.get("webhookUrl", "https://stagingapi.leap.health/leap-useraccountmanagement/api/users/session-logs");
        int threads = Integer.parseInt(config.get("workerThreads", "4"));
        this.executor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "kc-event-http-worker");
            t.setDaemon(true);
            return t;
        });
        this.mapper = new ObjectMapper();

        logger.infof("EventWebhookListenerProviderFactory initialized webhook=%s workerThreads=%d", webhookUrl, threads);
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        // Try to get Keycloak built-in client
        CloseableHttpClient httpClient = null;
        try {
            HttpClientProvider hcProvider = session.getProvider(HttpClientProvider.class);
            if (hcProvider != null) {
                httpClient = hcProvider.getHttpClient();
                logger.info("Using Keycloak built-in HttpClient");
            }
        } catch (Exception e) {
            logger.warn("Unable to obtain Keycloak HttpClientProvider, falling back to local client", e);
        }

        if (httpClient == null) {
            // fallback: create a simple CloseableHttpClient (should be rare)
            httpClient = HttpClients.createDefault();
            logger.warn("Using fallback CloseableHttpClient (not Keycloak-managed)");
        }

        return new EventWebhookListenerProvider(session, httpClient, executor, webhookUrl, mapper);
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdownNow();
        }
        logger.info("EventWebhookListenerProviderFactory closed");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
