package io.apicurio.sync;

import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;

public class RegistryClientProducer {
    
    @ConfigProperty(name = "apicurio.registry.url")
    String registryUrl;

    @Produces
    public RegistryClient registryClient() {
        return RegistryClientFactory.create(registryUrl);
    }

}
