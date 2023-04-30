package io.apicurio.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.impl.ErrorHandler;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.sync.api.labels.ArtifactLabelsHandler;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

import java.util.Collections;

import io.apicurio.rest.client.JdkHttpClient;
import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.auth.OidcAuth;
import io.apicurio.rest.client.auth.exception.AuthErrorHandler;

@ApplicationScoped
public class RegistryClientProducer {

    @ConfigProperty(name = "apicurio.registry.url")
    String registryUrl;

    @Inject
    Vertx vertx;

    @Produces
    public RegistryClient registryClient() {
        final String tokenEndpoint = System.getenv("AUTH_TOKEN_ENDPOINT");
        if (tokenEndpoint != null) {
            final String authClient = System.getenv("AUTH_CLIENT_ID");
            final String authSecret = System.getenv("AUTH_CLIENT_SECRET");
            Auth auth = new OidcAuth(new JdkHttpClient(tokenEndpoint, Collections.emptyMap(), null, new ErrorHandler()), authClient, authSecret);
            return RegistryClientFactory.create(new JdkHttpClient(registryUrl, Collections.emptyMap(), auth, new ErrorHandler()));
        } else {
            return RegistryClientFactory.create(registryUrl);
        }
    }

    @Produces
    public WebClient webClient() {
        return WebClient.create(vertx);
    }

    @Produces
    public ArtifactLabelsHandler labelsHandler() {
        return new ArtifactLabelsHandler();
    }

}
