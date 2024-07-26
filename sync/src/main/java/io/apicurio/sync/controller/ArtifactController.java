package io.apicurio.sync.controller;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.exception.ArtifactAlreadyExistsException;
import io.apicurio.registry.rest.client.exception.ArtifactNotFoundException;
import io.apicurio.registry.rest.client.exception.VersionNotFoundException;
import io.apicurio.registry.rest.v2.beans.ArtifactContent;
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import io.apicurio.registry.rest.v2.beans.ArtifactReference;
import io.apicurio.registry.rest.v2.beans.EditableMetaData;
import io.apicurio.registry.rest.v2.beans.IfExists;
import io.apicurio.registry.rest.v2.beans.UpdateState;
import io.apicurio.registry.rest.v2.beans.VersionMetaData;
import io.apicurio.registry.rest.v2.beans.VersionSearchResults;
import io.apicurio.registry.types.ArtifactState;
import io.apicurio.sync.Configuration;
import io.apicurio.sync.api.Artifact;
import io.apicurio.sync.api.ArtifactSpec;
import io.apicurio.sync.api.ArtifactStatus;
import io.apicurio.sync.api.labels.ArtifactLabelsHandler;
import io.apicurio.sync.clients.ArtifactResourceClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.vertx.mutiny.ext.web.client.WebClient;

@Controller
public class ArtifactController implements ResourceController<Artifact> {

    private static final String STATUS_READY = "ready";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    RegistryClient registryClient;

    @Inject
    WebClient webClient;

    @Inject
    Configuration config;

    @Inject
    ArtifactResourceClient artifactResourceClient;

    @Inject
    ArtifactLabelsHandler labelsHandler;

    @Override
    public DeleteControl deleteResource(Artifact resource, Context<Artifact> context) {
        if (config.getDeleteArtifactsEnabled()) {
            debugLog(resource.getSpec(), "Handling delete " + resource.getMetadata().getName());

            if (resource.getMetadata().getDeletionTimestamp() != null) {
                log.debug("del - Deletion timestamp is not null, this may be a marked for deletion object");
            }

            ArtifactStatus status = resource.getStatus();
            if (status == null || status.getStatus() == null || !STATUS_READY.equals(status.getStatus())) {
                log.debug("status is null or is marked as not ready, skipping delete");
                return DeleteControl.DEFAULT_DELETE;
            }
            ArtifactSpec spec = resource.getSpec();

            var artifactsResources = artifactResourceClient.listVersions(resource);
            if (artifactsResources.getItems().isEmpty()) {
                debugLog(spec, "going to delete artifact");
                try {
                    registryClient.deleteArtifact(spec.getGroupId(), spec.getArtifactId());
                } catch (ArtifactNotFoundException e) {
                    //ignored
                }
            } else {
                debugLog(spec, "Artifact versions resources found " + artifactsResources.getItems().size());
                try {
                    UpdateState update = new UpdateState();
                    update.setState(ArtifactState.DISABLED);
                    registryClient.updateArtifactVersionState(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(), update);


                    VersionSearchResults versions = registryClient.listArtifactVersions(spec.getGroupId(), spec.getArtifactId(), 0, 100);
                    boolean notAllDisabled = versions.getVersions().stream()
                        .anyMatch(ver -> ver.getState() != ArtifactState.DISABLED);
                    if (!notAllDisabled) {
                        debugLog(spec, "Going to delete artifact, all versions disabled");
                        registryClient.deleteArtifact(spec.getGroupId(), spec.getArtifactId());
                    }

                } catch (VersionNotFoundException | ArtifactNotFoundException e) {
                    //ignored
                    return DeleteControl.DEFAULT_DELETE;
                }
            }

        }
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<Artifact> createOrUpdateResource(Artifact resource, Context<Artifact> context) {

        if (resource.getMetadata().getDeletionTimestamp() != null) {
            log.debug("Deletion timestamp is not null, this may be a marked for deletion object");
        }

        debugLog(resource.getSpec(), "Handling createOrUpdate " + resource.getMetadata().getName());

//        Optional<CustomResourceEvent> latestArtifactEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);

        byte[] content;
        if (resource.getSpec().getContent() != null) {
            content = resource.getSpec().getContent().getBytes();
        } else if (resource.getSpec().getExternalContent() != null) {
            OperationContext<byte[]> ctx = getExternalContent(resource.getSpec().getExternalContent());
            if (ctx.getError() != null) {
                updateArtifactStatus(resource, ctx.getError());
                return UpdateControl.updateStatusSubResource(resource);
            } else {
                content = ctx.getData();
            }
        } else {
            updateArtifactStatus(resource, "Content is not provided");
            return UpdateControl.updateStatusSubResource(resource);
        }

        ArtifactSpec spec = resource.getSpec();
        int beforeHandleHash = spec.hashCode();

        try {

            ArtifactContext ctx = createOrUpdateArtifact(spec, content);
            if (ctx.getError() != null) {
                updateArtifactStatus(resource, ctx.getError());
                return UpdateControl.updateStatusSubResource(resource);
            }

            ArtifactMetaData meta = ctx.getData();

            spec.setGroupId(meta.getGroupId());
            spec.setArtifactId(meta.getId());
            spec.setVersion(String.valueOf(meta.getVersion()));
            spec.setType(meta.getType());
            labelsHandler.setLabels(resource);

            spec.setGlobalId(meta.getGlobalId());
            spec.setContentId(meta.getContentId());

            spec.setModifiedBy(meta.getModifiedBy());
            spec.setModifiedOn(meta.getModifiedOn());


            // metadata sync

            if (ctx.getOutcome() != OperationOutcome.ALREADY_EXISTS) {
                if (spec.getName() == null) {
                    spec.setName(meta.getName());
                }
                if (spec.getDescription() == null) {
                    spec.setDescription(meta.getDescription());
                }
                if (spec.getLabels() == null) {
                    spec.setLabels(meta.getLabels());
                }
                if (spec.getProperties() == null) {
                    spec.setProperties(meta.getProperties());
                }
            }

            boolean updateMeta = false;

            if (isMetaDiff(spec.getName(), meta.getName())) {
                updateMeta = true;
            }

            if (isMetaDiff(spec.getDescription(), meta.getDescription())) {
                updateMeta = true;
            }

            if (isMetaDiff(spec.getLabels(), meta.getLabels())) {
                updateMeta = true;
            }

            if (isMetaDiff(spec.getProperties(), meta.getProperties())) {
                updateMeta = true;
            }

            if (updateMeta) {
                debugLog(spec, "updating metadata");
                EditableMetaData newMetadata = buildMetadata(spec);
                registryClient.updateArtifactVersionMetaData(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(), newMetadata);
            }

            //

            if (spec.getState() == null) {
                spec.setState(meta.getState().value());
            }

            if (!meta.getState().value().equals(spec.getState())) {
                log.debug("Updating state to {} for artifact resource {}", spec.getState(), resource.getMetadata().getName());
                UpdateState update = new UpdateState();
                update.setState(ArtifactState.fromValue(spec.getState()));
                registryClient.updateArtifactVersionState(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(), update);
            }

            int afterHandleHash = spec.hashCode();

            setArtifactCoordinatesInStatus(resource, meta);
            updateArtifactStatus(resource, null);
            if (beforeHandleHash == afterHandleHash) {
                log.debug("Updating only status resource = {}", resource.getMetadata().getName());
                return UpdateControl.updateStatusSubResource(resource);
            } else {
                log.debug("Updating resource resource = {}", resource.getMetadata().getName());
                return UpdateControl.updateCustomResourceAndStatus(resource);
            }
        } catch (Exception e) {
            log.error("Error createOrUpdate artifact", e);
            updateArtifactStatus(resource, e.getMessage());
            return UpdateControl.updateStatusSubResource(resource);
        }
    }

    private boolean isMetaDiff(Object source, Object dest) {
        if (dest == null && source != null) {
            return true;
        }
        return dest != null && !dest.equals(source);
    }

    private EditableMetaData buildMetadata(ArtifactSpec spec) {
        EditableMetaData artifactMeta = new EditableMetaData();
        artifactMeta.setName(spec.getName());
        artifactMeta.setDescription(spec.getDescription());
        artifactMeta.setLabels(spec.getLabels());
        artifactMeta.setProperties(spec.getProperties());
        return artifactMeta;
    }

    private synchronized ArtifactContext createOrUpdateArtifact(ArtifactSpec spec, byte[] content) {
        if (spec.getVersion() == null) {
            debugLog(spec, "creating artifact");
            try {
                //try to create artifact, or fail
                return ArtifactContext.metadata(OperationOutcome.CREATED,
                        createArtifact(spec, content, IfExists.FAIL));
            } catch (ArtifactAlreadyExistsException e) {
                try {
                    //check what version is this exactly
                    VersionMetaData vmeta = getArtifactVersionMetaDataByContent(spec, content);
                    debugLog(spec, "artifact version already exists, doing nothing");
                    return ArtifactContext.metadata(OperationOutcome.ALREADY_EXISTS, toMeta(vmeta));
                } catch (ArtifactNotFoundException nfe) {
                    //artifact exists create new version
                    debugLog(spec, "artifact exists, creating new version");
                    ArtifactMetaData meta = updateArtifact(spec, content);
                    return ArtifactContext.metadata(OperationOutcome.UPDATED, meta);
                    //TODO catch possible exception?
                }
            }
        } else {
            try {
                VersionMetaData vmeta = getArtifactVersionMetaDataByContent(spec, content);
                if (vmeta.getVersion().equals(spec.getVersion())) {
                    debugLog(spec, "version already exists, doing nothing");
                    return ArtifactContext.metadata(OperationOutcome.ALREADY_EXISTS, toMeta(vmeta));
                } else {
                    debugLog(spec, "content exists in a different version");
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
                    debugLog(spec, "creating new version");
                    return ArtifactContext.metadata(OperationOutcome.UPDATED, updateArtifact(spec, content));
                } catch (ArtifactNotFoundException e1) {
                    //artifact does not exists at all
                    debugLog(spec, "creating artifact specific version {}");
                    return ArtifactContext.metadata(OperationOutcome.CREATED,
                            createArtifact(spec, content, IfExists.RETURN));
                }
            }
        }
    }

    private ArtifactMetaData createArtifact(ArtifactSpec spec, byte[] content, IfExists ifExists) {
        List<ArtifactReference> references = toRegistryReferences(spec.getReferences());
        return registryClient.createArtifact(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(),
                spec.getType(), ifExists, false, spec.getName(), spec.getDescription(), null, null, null,
                new ByteArrayInputStream(content), references);
    }

    private ArtifactMetaData updateArtifact(ArtifactSpec spec, byte[] content) {
        List<ArtifactReference> references = toRegistryReferences(spec.getReferences());
        return registryClient.updateArtifact(spec.getGroupId(), spec.getArtifactId(), spec.getVersion(), null,
                null, new ByteArrayInputStream(content), references);
    }

    private VersionMetaData getArtifactVersionMetaDataByContent(ArtifactSpec spec, byte[] content) {
        List<ArtifactReference> references = toRegistryReferences(spec.getReferences());

        return registryClient.getArtifactVersionMetaDataByContent(spec.getGroupId(), spec.getArtifactId(),
                false, ArtifactContent.builder().content(new String(content)).references(references).build());
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
        status.setStatus(error == null ? STATUS_READY : "error");
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

    private List<ArtifactReference> toRegistryReferences(
            List<io.apicurio.sync.api.ArtifactReference> references) {
        return references.stream()
                .map(reference -> ArtifactReference.builder()
                        .artifactId(reference.getArtifactId())
                        .groupId(reference.getGroupId())
                        .name(reference.getName())
                        .version(reference.getVersion())
                        .build())
                .collect(Collectors.toList());
    }

    private OperationContext<byte[]> getExternalContent(String url) {
        try {
            Optional<Long> contentLength = webClient.headAbs(url)
                    .send()
                    .map(res -> {
                        if (res.statusCode() == 200) {
                            return Long.valueOf(res.getHeader("Content-Length"));
                        } else {
                            log.debug("Head request, status {}", res.statusMessage());
                        }
                        return null;
                    })
                    .await().asOptional().atMost(Duration.ofSeconds(5));
            if (contentLength.isEmpty()) {
                return OperationContext.error("Error accessing externalContent");
            }
            if (contentLength.get() > config.getMaxContentLengthBytes()) {
                return OperationContext.error("ExternalContent lenght exceeds max length");
            }

            return OperationContext.with(webClient.getAbs(url)
                    .send()
                    .await().atMost(Duration.ofSeconds(5))
                    .body()
                    .getBytes());

        } catch (Exception e) {
            log.error("Unexpected error", e);
            return OperationContext.error("Error accessing externalContent. " + e.getMessage());
        }
    }

    private void debugLog(ArtifactSpec spec, String message) {
        log.debug("[ groupId = {} artifactId = {} ] {}", spec.getGroupId(), spec.getArtifactId(), message);
    }

}
