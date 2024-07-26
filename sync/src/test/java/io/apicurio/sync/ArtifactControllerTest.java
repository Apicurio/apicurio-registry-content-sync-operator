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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.sync.api.ArtifactBuilder;
import io.apicurio.sync.api.ArtifactReferenceBuilder;
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
                        .withLabels("foo", "baz")
                        .withProperties(Map.of("prop", "test"))
                        .build())
                .build();

        {

            controller.createOrUpdateResource(artifactv1, null);

            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            assertEquals(1, registryClient.listArtifactVersions(null, "person", 0, 100).getCount());

            controller.createOrUpdateResource(artifactv1, null);
            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            var versions = registryClient.listArtifactVersions(null, "person", 0, 100);
            assertEquals(1, versions.getCount());

            {
                var version1 = versions.getVersions().get(0);
                assertEquals(2, version1.getLabels().size());
                assertTrue(version1.getLabels().contains("foo"));
                assertTrue(version1.getLabels().contains("baz"));
                assertEquals(1, version1.getProperties().size());
                assertTrue(version1.getProperties().containsKey("prop"));
                assertEquals("test", version1.getProperties().get("prop"));
            }

            //update metadata
            artifactv1.getSpec().setName("person foo");
            controller.createOrUpdateResource(artifactv1, null);
            assertEquals(1, registryClient.listArtifactsInGroup(null).getCount().intValue());
            assertEquals(1, registryClient.listArtifactVersions(null, "person", 0, 100).getCount());
            var version1updated = registryClient.getArtifactVersionMetaData(null, "person", "1");
            assertEquals("person foo", version1updated.getName());
            assertEquals(2, version1updated.getLabels().size());
            assertTrue(version1updated.getLabels().contains("foo"));
            assertTrue(version1updated.getLabels().contains("baz"));
            assertEquals(1, version1updated.getProperties().size());
            assertTrue(version1updated.getProperties().containsKey("prop"));
            assertEquals("test", version1updated.getProperties().get("prop"));
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


        controller.deleteResource(artifactv1, null);
        controller.deleteResource(artifactv2, null);
        assertEquals(0, registryClient.listArtifactsInGroup(null).getCount().intValue());

    }

    @Test
    public void testAddingReferences() {
        var groupId = "references";
        var phoneNumberId = "phone-number";
        var personId = "person";
        assertEquals(0, registryClient.listArtifactsInGroup(groupId).getCount().intValue());

        var phoneNumberProtoArtifact = new ArtifactBuilder()
                .withNewMetadata()
                .withNamespace(ApicurioKubeSyncTestProfile.NAMESPACE)
                .withName(phoneNumberId)
                .endMetadata()
                .withSpec(new ArtifactSpecBuilder()
                        .withGroupId(groupId)
                        .withArtifactId(phoneNumberId)
                        .withContent(TestUtils.resourceToString("artifactTypes/protobuf/phone_number.proto"))
                        .build())
                .build();

        controller.createOrUpdateResource(phoneNumberProtoArtifact, null);

        assertEquals(1, registryClient.listArtifactsInGroup(groupId).getCount().intValue());
        assertEquals(1, registryClient.listArtifactVersions(groupId, phoneNumberId, 0, 100).getCount());

        var personProtoArtifact = new ArtifactBuilder()
                .withNewMetadata()
                .withNamespace(ApicurioKubeSyncTestProfile.NAMESPACE)
                .withName(personId)
                .endMetadata()
                .withSpec(new ArtifactSpecBuilder()
                        .withGroupId(groupId)
                        .withArtifactId(personId)
                        .withContent(TestUtils.resourceToString("artifactTypes/protobuf/person_v1.proto"))
                        .withReferences(new ArtifactReferenceBuilder()
                                .withName(phoneNumberId)
                                .withArtifactId(phoneNumberId)
                                .withGroupId(groupId)
                                .withVersion(phoneNumberProtoArtifact.getSpec().getVersion())
                                .build())
                        .build())
                .build();

        controller.createOrUpdateResource(personProtoArtifact, null);

        assertEquals(2, registryClient.listArtifactsInGroup(groupId).getCount().intValue());
        assertEquals(1, registryClient.listArtifactVersions(groupId, personId, 0, 100).getCount());

        personProtoArtifact = new ArtifactBuilder()
                .withNewMetadata()
                .withNamespace(ApicurioKubeSyncTestProfile.NAMESPACE)
                .withName(personId)
                .endMetadata()
                .withSpec(new ArtifactSpecBuilder()
                        .withGroupId(groupId)
                        .withArtifactId(personId)
                        .withContent(TestUtils.resourceToString("artifactTypes/protobuf/person_v2.proto"))
                        .withReferences(new ArtifactReferenceBuilder()
                                .withName(phoneNumberId)
                                .withArtifactId(phoneNumberId)
                                .withGroupId(groupId)
                                .withVersion(phoneNumberProtoArtifact.getSpec().getVersion())
                                .build())
                        .build())
                .build();

        controller.createOrUpdateResource(personProtoArtifact, null);

        assertEquals(2, registryClient.listArtifactsInGroup(groupId).getCount().intValue());
        assertEquals(2, registryClient.listArtifactVersions(groupId, personId, 0, 100).getCount());

        controller.deleteResource(phoneNumberProtoArtifact, null);
        controller.deleteResource(personProtoArtifact, null);

        assertEquals(0, registryClient.listArtifactsInGroup(null).getCount().intValue());

    }
}
