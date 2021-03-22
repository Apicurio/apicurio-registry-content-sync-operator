package io.apicurio.sync.controller;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.exception.ArtifactNotFoundException;
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import io.apicurio.registry.rest.v2.beans.EditableMetaData;
import io.apicurio.registry.rest.v2.beans.IfExists;
import io.apicurio.registry.rest.v2.beans.UpdateState;
import io.apicurio.registry.rest.v2.beans.VersionMetaData;
import io.apicurio.registry.types.ArtifactState;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.sync.api.Artifact;
import io.apicurio.sync.api.ArtifactSpec;
import io.apicurio.sync.api.ArtifactStatus;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

@Controller(generationAwareEventProcessing = true, namespaces = Controller.WATCH_CURRENT_NAMESPACE)
//DelayRegistrationUntil
public class ArtifactController implements ResourceController<Artifact> {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    RegistryClient registryClient;

    @Override
    public DeleteControl deleteResource(Artifact resource, Context<Artifact> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<Artifact> createOrUpdateResource(Artifact resource, Context<Artifact> context) {

        log.debug("Handling createOrUpdate {}", resource.getMetadata().getName());

//        Optional<CustomResourceEvent> latestArtifactEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);

        if (resource.getSpec().getContent() == null) {
            updateArtifactStatus(resource, "Content is not provided");
            return UpdateControl.updateStatusSubResource(resource);
        }

        byte[] content = resource.getSpec().getContent().getBytes();
        //TODO get external content


        ArtifactSpec spec = resource.getSpec();
        int beforeHandleHash = spec.hashCode();

        //super workaround!
        //FIXME add hashcode and equals to editablemetadata
        int beforeHandleMetadataHash = 0;
        try {
            beforeHandleMetadataHash = mapper.writeValueAsString(buildMetadata(spec)).hashCode();
        } catch (JsonProcessingException e1) {
            updateArtifactStatus(resource, e1.getMessage());
            return UpdateControl.updateStatusSubResource(resource);
        }

        try {

            ArtifactContext ctx = createOrUpdateArtifact(spec, content);
            if (ctx.getError() != null) {
                updateArtifactStatus(resource, ctx.getError());
                return UpdateControl.updateStatusSubResource(resource);
            }

            ArtifactMetaData meta = ctx.getMetadata();

            spec.setGroupId(meta.getGroupId());
            spec.setArtifactId(meta.getId());
            spec.setVersion(String.valueOf(meta.getVersion()));

            spec.setGlobalId(meta.getGlobalId());
            spec.setContentId(meta.getContentId());

            spec.setModifiedBy(meta.getModifiedBy());
            spec.setModifiedOn(meta.getModifiedOn());

            if (spec.getName() == null) {
                spec.setName(meta.getName());
                spec.setDescription(meta.getDescription());
                spec.setLabels(meta.getLabels());
                spec.setProperties(meta.getProperties());
            } else {

                int afterHandleMetadataHash = 0;
                EditableMetaData newMetadata = buildMetadata(spec);
                try {
                    afterHandleMetadataHash = mapper.writeValueAsString(newMetadata).hashCode();
                } catch (JsonProcessingException e1) {
                    updateArtifactStatus(resource, e1.getMessage());
                    return UpdateControl.updateStatusSubResource(resource);
                }

                if (beforeHandleMetadataHash != afterHandleMetadataHash) {
                    log.debug("Updating metadata {}", resource.getMetadata().getName());
                    registryClient.updateArtifactVersionMetaData(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(), newMetadata);
                } else {
                    log.debug("Metadata was not updated, doing nothing {}", resource.getMetadata().getName());
                }

            }

            spec.setType(meta.getType().value());

            if (spec.getState() == null) {
                spec.setState(meta.getState().value());
            }

            if (!meta.getState().value().equals(spec.getState())) {
                log.debug("Updating state to {} for artifact {}", spec.getState(), resource.getMetadata().getName());
                UpdateState update = new UpdateState();
                update.setState(ArtifactState.fromValue(spec.getState()));
                registryClient.updateArtifactVersionState(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(), update);
            }

            int afterHandleHash = spec.hashCode();

            setArtifactCoordinatesInStatus(resource, meta);
            updateArtifactStatus(resource, null);
            if (beforeHandleHash == afterHandleHash) {
                log.debug("Updating only status {}", resource.getMetadata().getName());
                return UpdateControl.updateStatusSubResource(resource);
            } else {
                log.debug("Updating resource {}", resource.getMetadata().getName());
                return UpdateControl.updateCustomResourceAndStatus(resource);
            }
        } catch (Exception e) {
            log.error("Error createOrUpdate artifact", e);
            updateArtifactStatus(resource, e.getMessage());
            return UpdateControl.updateStatusSubResource(resource);
        }
    }

    private EditableMetaData buildMetadata(ArtifactSpec spec) {
        EditableMetaData artifactMeta = new EditableMetaData();
        artifactMeta.setName(spec.getName());
        artifactMeta.setDescription(spec.getDescription());
        artifactMeta.setLabels(spec.getLabels());
        artifactMeta.setProperties(spec.getProperties());
        return artifactMeta;
    }

    private ArtifactContext createOrUpdateArtifact(ArtifactSpec spec, byte[] content) {
        if (spec.getVersion() == null) {
            log.debug("Creating artifact {}", spec.getArtifactId());
            return ArtifactContext.metadata(registryClient.createArtifact(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(),
                    ArtifactType.fromValue(spec.getType()),
                    IfExists.RETURN,
                    false, new ByteArrayInputStream(content)));
        } else {
            try {
                VersionMetaData vmeta = registryClient.getArtifactVersionMetaDataByContent(spec.getGroupId(), spec.getArtifactId(), false, new ByteArrayInputStream(content));
                if (vmeta.getVersion().equals(spec.getVersion())) {
                    log.debug("This version already exists, doing nothing");
                    return ArtifactContext.metadata(toMeta(vmeta));
                } else {
                    log.debug("This content exists in a different version");
                    return ArtifactContext.error("This content already exists, but in a different version of the artifact");
                }
//                byte[] versionContent = IoUtil.toBytes(registryClient.getArtifactVersion(spec.getGroupId(), spec.getArtifactId(), spec.getVersion()));
//                log.info(new String(versionContent));
//                log.info(new String(content));
//                if (versionContent == content) {
//                    return toMeta(registryClient.getArtifactVersionMetaData(spec.getGroupId(), spec.getArtifactId(), spec.getVersion()));
//                } else {
//                    throw new RuntimeException("Version already exists but with different content. Version updates are not allowed, either remove the version field or choose another version value");
//                }
            } catch (ArtifactNotFoundException e) {
                try {
                    registryClient.getArtifactMetaData(spec.getGroupId(), spec.getArtifactId());
                    //artifact exists create new version
                    log.debug("Creating new version {}", spec.getArtifactId());
                    VersionMetaData vmeta = registryClient.createArtifactVersion(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(), new ByteArrayInputStream(content));
                    return ArtifactContext.metadata(toMeta(vmeta));
                } catch (ArtifactNotFoundException e1) {
                    //artifact does not exists at all
                    log.debug("Creating artifact specific version {}", spec.getArtifactId());
                    return ArtifactContext.metadata(registryClient.createArtifact(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(),
                            ArtifactType.fromValue(spec.getType()),
                            IfExists.RETURN,
                            false, new ByteArrayInputStream(content)));
                }
            }
        }
    }

    private ArtifactStatus getStatus(Artifact artifact) {
        ArtifactStatus status = Optional.ofNullable(artifact.getStatus())
                .orElse(new ArtifactStatus());
        artifact.setStatus(status);
        return status;
    }

    private void setArtifactCoordinatesInStatus(Artifact artifact, ArtifactMetaData meta) {
        ArtifactStatus status = getStatus(artifact);
        status.setGroupId(meta.getGroupId());
        status.setArtifactId(meta.getId());
        status.setVersion(meta.getVersion());
        status.setGlobalId(meta.getGlobalId());
    }

    private void updateArtifactStatus(Artifact artifact, String error) {
        ArtifactStatus status = getStatus(artifact);
        status.setReady(error == null);
        status.setError(error);
    }

    private ArtifactMetaData toMeta(VersionMetaData vmeta) {
        ArtifactMetaData meta = new ArtifactMetaData();
        meta.setGroupId(vmeta.getGroupId());
        meta.setId(vmeta.getId());
        meta.setVersion(vmeta.getVersion());
        meta.setContentId(vmeta.getContentId());
        meta.setGlobalId(vmeta.getGlobalId());
        meta.setName(vmeta.getName());
        meta.setDescription(vmeta.getDescription());
        meta.setLabels(vmeta.getLabels());
        meta.setProperties(vmeta.getProperties());

        meta.setModifiedBy(vmeta.getCreatedBy());
        meta.setModifiedOn(vmeta.getCreatedOn());

        meta.setType(vmeta.getType());
        meta.setState(vmeta.getState());

        return meta;
    }

}
