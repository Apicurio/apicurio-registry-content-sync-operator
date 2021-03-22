package io.apicurio.sync.api;

import io.dekorate.crd.annotation.Crd;
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
@Group("apicur.io")
@Version("v1alpha1")
@Crd(group = "apicur.io", version = "v1alpha1", status = ArtifactStatus.class)
@EqualsAndHashCode(callSuper = true)
public class Artifact extends CustomResource<ArtifactSpec, ArtifactStatus> implements Namespaced {

    private static final long serialVersionUID = 1L;

}
