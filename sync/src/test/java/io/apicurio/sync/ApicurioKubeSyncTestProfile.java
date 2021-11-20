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

package io.apicurio.sync;

import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;

/**
 * @author Fabian Martinez
 */
public class ApicurioKubeSyncTestProfile implements QuarkusTestProfile {

    public static final String NAMESPACE = "test-namespace";

    List<TestResourceEntry> resources;

    /**
     * @see io.quarkus.test.junit.QuarkusTestProfile#testResources()
     */
    @Override
    public List<TestResourceEntry> testResources() {
        if (resources == null) {
            resources = List.of(new TestResourceEntry(KubernetesServerTestResource.class));
        }
        return resources;
    }

    /**
     * @see io.quarkus.test.junit.QuarkusTestProfile#getConfigOverrides()
     */
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "apicurio.registry.url", "http://localhost:8181",
                "quarkus.operator-sdk.namespaces", NAMESPACE,
                "apicurio.sync.delete.artifacts", "true"
                );
    }

}
