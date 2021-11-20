package io.apicurio.sync.clients;

import javax.enterprise.context.ApplicationScoped;

import io.apicurio.sync.api.Artifact;
import io.apicurio.sync.api.ArtifactList;

@ApplicationScoped
public class ArtifactResourceClient extends AbstractCustomResourceClient<Artifact, ArtifactList> {

    @Override
    protected Class<Artifact> getCustomResourceClass() {
        return Artifact.class;
    }

    @Override
    protected Class<ArtifactList> getCustomResourceListClass() {
        return ArtifactList.class;
    }

    public ArtifactList find(String groupId, String artifactId, String version) {

        var query = resourceClient.inNamespace(getNamespace())
            .withField("status.artifactId", artifactId);

        if (version != null) {
            query.withField("status.version", version);
        }

        if (groupId != null) {
            query.withField("status.groupId", groupId);
        }

        return query.list();
    }

}
