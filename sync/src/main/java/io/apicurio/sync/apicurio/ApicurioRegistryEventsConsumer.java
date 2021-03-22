package io.apicurio.sync.apicurio;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.events.dto.ArtifactId;
import io.apicurio.registry.events.dto.RegistryEventType;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.sync.clients.ArtifactResourceClient;

@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
public class ApicurioRegistryEventsConsumer {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    RegistryClient registryClient;

    @Inject
    ArtifactResourceClient artifactClient;

    @POST
    public void consumeEvent(String body, @HeaderParam("ce-type") String eventType) throws Exception {
        //TODO do the processing asynchronous
        log.info("{} -> {}", eventType, body);
        RegistryEventType type = RegistryEventType.valueOf(eventType);

        switch (type) {
            case ARTIFACT_CREATED:
                handleArtifactCreated(mapper.readValue(body.getBytes(), ArtifactId.class));
                break;
            default:
                break;
        }
    }

    private void handleArtifactCreated(ArtifactId event) {

        var list = artifactClient.find(event.getGroupId(), event.getArtifactId(), event.getVersion());

        var cr = list.getItems()
            .stream()
            .findFirst()
            .get();


    }

}
