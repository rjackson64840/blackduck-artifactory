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

import java.io.File;

public class DirectoryConfig {
    private final File homeDirectory;
    private final File etcDirectory;
    private final File pluginsLibDirectory;
    private final File versionFile;
    private final String thirdPartyVersion;
    private final String propertiesFilePathOverride;

    public DirectoryConfig(final File homeDirectory, final File etcDirectory, final File pluginsLibDirectory, final File versionFile, final String thirdPartyVersion, final String propertiesFilePathOverride) {
        this.homeDirectory = homeDirectory;
        this.etcDirectory = etcDirectory;
        this.pluginsLibDirectory = pluginsLibDirectory;
        this.versionFile = versionFile;
        this.thirdPartyVersion = thirdPartyVersion;
        this.propertiesFilePathOverride = propertiesFilePathOverride;
    }

    public static DirectoryConfig createDefault(final File homeDirectory, final File etcDirectory, final File pluginsDirectory, final String thirdPartyVersion, final String propertiesFilePathOverride) {
        final File pluginsLibDirectory = new File(pluginsDirectory, "lib");
        final File versionFile = new File(pluginsLibDirectory, "version.txt");

        return new DirectoryConfig(homeDirectory, etcDirectory, pluginsLibDirectory, versionFile, thirdPartyVersion, propertiesFilePathOverride);
    }

    public File getHomeDirectory() {
        return homeDirectory;
    }

    public File getEtcDirectory() {
        return etcDirectory;
    }

    public File getPluginsLibDirectory() {
        return pluginsLibDirectory;
    }

    public File getVersionFile() {
        return versionFile;
    }

    public String getThirdPartyVersion() {
        return thirdPartyVersion;
    }

    public String getPropertiesFilePathOverride() {
        return propertiesFilePathOverride;
    }
}
