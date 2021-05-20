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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author Fabian Martinez
 */
@ApplicationScoped
public class Configuration {

    private final static long KB_FACTOR = 1000;
    private final static long KIB_FACTOR = 1024;
    private final static long MB_FACTOR = 1000 * KB_FACTOR;
    private final static long MIB_FACTOR = 1024 * KIB_FACTOR;
    private final static long GB_FACTOR = 1000 * MB_FACTOR;
    private final static long GIB_FACTOR = 1024 * MIB_FACTOR;

    @Inject
    @ConfigProperty(name = "apicurio.max.content-length", defaultValue = "5 MiB")
    public String maxContentLength;

    public Long maxContentLengthBytes;

    @PostConstruct
    public void init() {
        maxContentLengthBytes = parseBytes(maxContentLength);
    }

    public Long getMaxContentLengthBytes() {
        return maxContentLengthBytes;
    }

    private Long parseBytes(String q) {
        if (q.split(" ").length != 2) {
            throw new IllegalArgumentException("Incorrect quantity format, it must be a number followed by the unit of measurement");
        }
        int spaceNdx = q.indexOf(" ");
        long ret = Long.parseLong(q.substring(0, spaceNdx));
        switch (q.substring(spaceNdx + 1)) {
            case "GB":
                return ret * GB_FACTOR;
            case "GiB":
                return ret * GIB_FACTOR;
            case "MB":
                return ret * MB_FACTOR;
            case "MiB":
                return ret * MIB_FACTOR;
            case "KB":
                return ret * KB_FACTOR;
            case "KiB":
                return ret * KIB_FACTOR;
        }
        throw new IllegalArgumentException("Unknown unit of measurement");
    }

}
