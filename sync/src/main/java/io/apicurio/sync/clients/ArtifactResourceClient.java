package io.apicurio.sync.clients;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.apicurio.sync.api.Artifact;
import io.apicurio.sync.api.ArtifactList;
import io.apicurio.sync.api.labels.ArtifactLabelsHandler;

@ApplicationScoped
public class ArtifactResourceClient extends AbstractCustomResourceClient<Artifact, ArtifactList> {

    @Inject
    ArtifactLabelsHandler labelsHandler;

    @Override
    protected Class<Artifact> getCustomResourceClass() {
        return Artifact.class;
    }

    @Override
    protected Class<ArtifactList> getCustomResourceListClass() {
        return ArtifactList.class;
    }

    public ArtifactList listVersions(Artifact artifact) {
        return resourceClient.inNamespace(getNamespace())
                .withLabelSelector(labelsHandler.getLabelSelectorAllVersions(artifact))
                .list();
    }

    // public ArtifactList find(String groupId, String artifactId, String version) {

    //     var query = resourceClient.inNamespace(getNamespace())
    //         .withField("status.artifactId", artifactId);

    //     if (version != null) {
    //         query.withField("status.version", version);
    //     }

    //     if (groupId != null) {
    //         query.withField("status.groupId", groupId);
    //     }

    //     return query.list();
    // }

}
