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

    private final OperationOutcome outcome;

    /**
     * Constructor.
     * @param data
     */
    public ArtifactContext(OperationOutcome outcome, ArtifactMetaData data) {
        super(data);
        this.outcome = outcome;
    }

    public ArtifactContext(String error) {
        super(error);
        this.outcome = OperationOutcome.ERROR;
    }

    public static ArtifactContext metadata(OperationOutcome outcome, ArtifactMetaData data) {
        return new ArtifactContext(outcome, data);
    }

    public static ArtifactContext error(String err) {
        return new ArtifactContext(err);
    }

    /**
     * @return the outcome
     */
    public OperationOutcome getOutcome() {
        return outcome;
    }

}
