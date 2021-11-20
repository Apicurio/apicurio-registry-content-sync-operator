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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

/**
 * @author Fabian Martinez
 */
public class ApicurioRegistryKubeSyncOperator implements QuarkusApplication {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    Operator operator;

//    @Inject
//    Configuration config;

    @Override
    public int run(String... args) throws Exception {
        log.info("Starting Apicurio Registry Kube Sync");

        printConfiguration();

        operator.start();
        Quarkus.waitForExit();
        return 0;
    }

    private void printConfiguration() {
//        List<String> config = StreamSupport.stream(ConfigProvider.getConfig().getPropertyNames().spliterator(), false)
//            .sorted()
//            .collect(Collectors.toList());
//        config.forEach( e -> {
//            try {
//                String value = ConfigProvider.getConfig().getValue(e, String.class);
//                log.info("{}={}", e, value);
//            } catch (NoSuchElementException ex) {
//                // ignore as some property values are not available
//            }
//        });
        //TODO use Configuration to print operator config values
    }

}