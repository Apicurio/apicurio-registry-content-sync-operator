package io.apicurio.sync.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Buildable(
        builderPackage = "io.fabric8.kubernetes.api.builder",
        editableEnabled = false
)
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtifactSpec {

    private String groupId;
    private String artifactId;
    private String version;

    private String name;
    private String description;

    private String modifiedBy;
    //FIXME remove modifiedOn, is not too useful and it triggers the control loop twice because of a tiny change in the date every time an artifact is created
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ", timezone = "UTC")
    private Date modifiedOn;

    private Long globalId;
    private Long contentId;

    /**
     * These are enums in apicurio-registry-datamodel but dekorate fails with a weird error if we use enums
     */
    private String type;
    private String state;

    private List<String> labels = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();

    private String content;
    private String externalContent;
    private List<ArtifactReference> references = new ArrayList<>();

    public String getGroupId() {
        return groupId;
    }
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    public String getArtifactId() {
        return artifactId;
    }
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Long getGlobalId() {
        return globalId;
    }
    public void setGlobalId(Long globalId) {
        this.globalId = globalId;
    }
    public Long getContentId() {
        return contentId;
    }
    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public List<String> getLabels() {
        return labels;
    }
    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
    public Map<String, String> getProperties() {
        return properties;
    }
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getExternalContent() {
        return externalContent;
    }
    public void setExternalContent(String externalContent) {
        this.externalContent = externalContent;
    }
    public List<ArtifactReference> getReferences() {
       return references;
    }
    public void setReferences(List<ArtifactReference> references) {
        this.references = references;
    }
    public String getModifiedBy() {
        return modifiedBy;
    }
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
    public Date getModifiedOn() {
        return modifiedOn;
    }
    public void setModifiedOn(Date modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

}
