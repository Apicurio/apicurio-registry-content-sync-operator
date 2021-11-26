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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.rest.v2.beans.VersionMetaData;
import io.apicurio.sync.api.ArtifactBuilder;
import io.apicurio.sync.api.ArtifactSpecBuilder;

/**
 * @author Fabian Martinez
 */
public class OperatorTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeEach
    public void prepare() {
        cleanup();
    }

    @AfterEach
    public void cleanup() {
//        logger.info("cleaning up env");
//        var artifactClient = E2ETestsuiteManager.getInstance().artifactClient();
//        artifactClient.delete(artifactClient.list().getItems());
//
//        RegistryClient registryClient = E2ETestsuiteManager.getInstance().registryClient();
//        var all = registryClient.searchArtifacts(null, null, null, null, null, null, null, 0, 500);
//        all.getArtifacts().forEach(a -> {
//            registryClient.deleteArtifact(a.getGroupId(), a.getId());
//        });
//        all = registryClient.searchArtifacts(null, null, null, null, null, null, null, 0, 500);
//        assertEquals(0, all.getCount().intValue());
    }

    @Test
    public void testOperator() {

        RegistryClient registryClient = E2ETestsuiteManager.getInstance().registryClient();
        var artifactClient = E2ETestsuiteManager.getInstance().artifactClient();

        assertEquals(0, registryClient.listArtifactsInGroup(null).getCount().intValue());

        var artifactv1 = new ArtifactBuilder()
                .withNewMetadata()
                    .withNamespace(E2ETestsuiteManager.NAMESPACE)
                    .withName("foo-v1")
                    .endMetadata()
                .withSpec(new ArtifactSpecBuilder()
                        .withArtifactId("person")
                        .withContent(TestUtils.resourceToString("artifactTypes/jsonSchema/person_v1.json"))
                        .build())
                .build();

        {


            artifactClient.create(artifactv1);

            TestUtils.await(() -> registryClient.listArtifactsInGroup(null).getCount().intValue() == 1);

            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            assertEquals(1, registryClient.listArtifactVersions(null, "person", 0, 100).getCount());

            //update metadata
            var artifactget = artifactClient.inNamespace(E2ETestsuiteManager.NAMESPACE).withName("foo-v1").get();
            artifactget.getSpec().setName("person foo");
            assertEquals(E2ETestsuiteManager.NAMESPACE, artifactget.getMetadata().getNamespace());
            assertEquals("person foo", artifactget.getSpec().getName());
            artifactClient.replace(artifactget);

            try {
                TestUtils.await(() -> {
                    VersionMetaData vmetadata = registryClient.getArtifactVersionMetaData(null, "person", "1");
                    logger.debug("Awaiting for metadata update {}", vmetadata);
                    return "person foo".equals(vmetadata.getName());
                });
            } finally {
                logger.warn("Versions of artifact {}", registryClient.listArtifactVersions(null, "person", 0, 100).getCount());
            }

            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            assertEquals(1, registryClient.listArtifactVersions(null, "person", 0, 100).getCount());
            assertEquals("person foo", registryClient.getArtifactVersionMetaData(null, "person", "1").getName());
        }

        var artifactv2 = new ArtifactBuilder()
                .withNewMetadata()
                    .withNamespace(E2ETestsuiteManager.NAMESPACE)
                    .withName("foo-v2")
                    .endMetadata()
                .withSpec(new ArtifactSpecBuilder()
                        .withArtifactId("person")
                        .withContent(TestUtils.resourceToString("artifactTypes/jsonSchema/person_v2.json"))
                        .build())
                .build();

        {

            artifactClient.create(artifactv2);

            TestUtils.await(() -> registryClient.listArtifactVersions(null, "person", 0, 100).getCount() == 2);

            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            var versions = registryClient.listArtifactVersions(null, "person", 0, 100);
            assertEquals(2, versions.getCount());

        }

        artifactClient.delete(artifactv1, artifactv2);
        TestUtils.await(() -> {
            return 0 == registryClient.listArtifactsInGroup(null).getCount().intValue();
        });
        assertEquals(0, registryClient.listArtifactsInGroup(null).getCount().intValue());

    }

}
