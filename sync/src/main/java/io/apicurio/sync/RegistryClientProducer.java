package io.apicurio.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.sync.api.labels.ArtifactLabelsHandler;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class RegistryClientProducer {

    @ConfigProperty(name = "apicurio.registry.url")
    String registryUrl;

    @Inject
    Vertx vertx;

    @Produces
    public RegistryClient registryClient() {
        return RegistryClientFactory.create(registryUrl);
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
