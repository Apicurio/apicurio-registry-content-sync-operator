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
public class ArtifactContext extends OperationContext<ArtifactMetaData> {

    /**
     * Constructor.
     * @param data
     */
    public ArtifactContext(ArtifactMetaData data) {
        super(data);
    }

    public ArtifactContext(String error) {
        super(error);
    }

    public static ArtifactContext metadata(ArtifactMetaData data) {
        return new ArtifactContext(data);
    }

    public static ArtifactContext error(String err) {
        return new ArtifactContext(err);
    }

}
