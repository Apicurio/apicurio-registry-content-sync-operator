/*
 * Copyright 2021 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.sync.controller;

import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;

/**
 * @author Fabian Martinez
 */
public class ArtifactContext {

    private ArtifactMetaData metadata;

    private String error;

    public ArtifactContext(String error) {
        this.error = error;
    }

    public ArtifactContext(ArtifactMetaData metadata) {
        this.metadata = metadata;
    }

    public static ArtifactContext metadata(ArtifactMetaData meta) {
        return new ArtifactContext(meta);
    }

    public static ArtifactContext error(String err) {
        return new ArtifactContext(err);
    }

    /**
     * @return the metadata
     */
    public ArtifactMetaData getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(ArtifactMetaData metadata) {
        this.metadata = metadata;
    }

    /**
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(String error) {
        this.error = error;
    }

}
