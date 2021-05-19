package io.apicurio.sync.api;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import lombok.EqualsAndHashCode;

@Buildable(
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs = @BuildableReference(CustomResource.class),
        editableEnabled = false
)
@Group("artifact.apicur.io")
@Version("v1alpha1")
@EqualsAndHashCode(callSuper = true)
public class Artifact extends CustomResource<ArtifactSpec, ArtifactStatus> implements Namespaced {

    private static final long serialVersionUID = 1L;

}
