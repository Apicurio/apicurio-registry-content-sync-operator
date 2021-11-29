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

package io.apicurio.e2e;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.sync.api.Artifact;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

/**
 * @author Fabian Martinez
 */
public class E2ETestsuiteManager {

    public static final String NAMESPACE = "e2e-kube-sync";

    //

    private static E2ETestsuiteManager instance;

    private E2ETestsuiteManager() {

    }

    public synchronized static E2ETestsuiteManager getInstance() {
        if (instance == null) {
            instance = new E2ETestsuiteManager();
        }
        return instance;
    }

    //

    private final KubernetesClient kubernetesClient = new DefaultKubernetesClient();

    public MixedOperation<Artifact, KubernetesResourceList<Artifact>, Resource<Artifact>> artifactClient() {
        return kubernetesClient.resources(Artifact.class);
    }

    private final RegistryClient registryClient = RegistryClientFactory.create("http://apicurio-registry.127.0.0.1.nip.io");

    public RegistryClient registryClient() {
        return registryClient;
    }


}
