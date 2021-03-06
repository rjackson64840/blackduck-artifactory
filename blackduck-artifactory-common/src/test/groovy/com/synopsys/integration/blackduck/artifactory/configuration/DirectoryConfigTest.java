/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DirectoryConfigTest {
    private DirectoryConfig directoryConfig;

    @BeforeEach
    void setUp() {
        final File homeDirectory = new File("home");
        final File etcDirectory = new File("etc");
        final File pluginsDirectory = new File("plugins");

        directoryConfig = DirectoryConfig.createDefault(homeDirectory, etcDirectory, pluginsDirectory, "1.2.3", "path/to/properties/file");
    }

    @Test
    void getHomeDirectory() {
        assertEquals(new File("home"), directoryConfig.getHomeDirectory());
    }

    @Test
    void getEtcDirectory() {
        assertEquals(new File("etc"), directoryConfig.getEtcDirectory());
    }

    @Test
    void getPluginsLibDirectory() {
        assertEquals(new File("plugins", "lib"), directoryConfig.getPluginsLibDirectory());
    }

    @Test
    void getVersionFile() {
        assertEquals(new File(directoryConfig.getPluginsLibDirectory(), "version.txt"), directoryConfig.getVersionFile());
    }

    @Test
    void getThirdPartyVersion() {
        assertEquals("1.2.3", directoryConfig.getThirdPartyVersion());
    }

    @Test
    void getPropertiesFilePathOverride() {
        assertEquals("path/to/properties/file", directoryConfig.getPropertiesFilePathOverride());
    }
}