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

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.sync.api.ArtifactBuilder;
import io.apicurio.sync.api.ArtifactSpecBuilder;
import io.apicurio.sync.controller.ArtifactController;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(ApicurioKubeSyncTestProfile.class)
public class ArtifactControllerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    RegistryClient registryClient;

    @Inject
    ArtifactController controller;

    @Test
    public void testArtifactCRUD() {

        assertEquals(0, registryClient.listArtifactsInGroup(null).getCount().intValue());

        var artifactv1 = new ArtifactBuilder()
                .withNewMetadata()
                    .withNamespace(ApicurioKubeSyncTestProfile.NAMESPACE)
                    .withName("foo-v1")
                    .endMetadata()
                .withSpec(new ArtifactSpecBuilder()
                        .withArtifactId("person")
                        .withContent(TestUtils.resourceToString("artifactTypes/jsonSchema/person_v1.json"))
                        .build())
                .build();

        {

            controller.createOrUpdateResource(artifactv1, null);

            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            assertEquals(1, registryClient.listArtifactVersions(null, "person", 0, 100).getCount());

            controller.createOrUpdateResource(artifactv1, null);
            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            assertEquals(1, registryClient.listArtifactVersions(null, "person", 0, 100).getCount());

            //update metadata
            artifactv1.getSpec().setName("person foo");
            controller.createOrUpdateResource(artifactv1, null);
            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            assertEquals(1, registryClient.listArtifactVersions(null, "person", 0, 100).getCount());
            assertEquals("person foo", registryClient.getArtifactVersionMetaData(null, "person", "1").getName());
        }

        var artifactv2 = new ArtifactBuilder()
                .withNewMetadata()
                    .withNamespace(ApicurioKubeSyncTestProfile.NAMESPACE)
                    .withName("foo-v2")
                    .endMetadata()
                .withSpec(new ArtifactSpecBuilder()
                        .withArtifactId("person")
                        .withContent(TestUtils.resourceToString("artifactTypes/jsonSchema/person_v2.json"))
                        .build())
                .build();

        {

            controller.createOrUpdateResource(artifactv2, null);

            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            var versions = registryClient.listArtifactVersions(null, "person", 0, 100);
            assertEquals(2, versions.getCount());

            controller.createOrUpdateResource(artifactv2, null);

            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            versions = registryClient.listArtifactVersions(null, "person", 0, 100);
            assertEquals(2, versions.getCount());

        }

        //TODO delete can only be tested in real k8s cluster or if quarkus tests can work properly with mock k8s server
//        controller.deleteResource(artifactv1, null);
//        controller.deleteResource(artifactv2, null);
//        assertEquals(0, registryClient.listArtifactsInGroup(null).getCount().intValue());

    }

}
