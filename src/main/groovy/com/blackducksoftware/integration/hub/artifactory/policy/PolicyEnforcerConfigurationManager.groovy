/*
 * hub-artifactory
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.artifactory.policy

import com.blackducksoftware.integration.hub.artifactory.CommonConfigurationManager
import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import embedded.org.apache.commons.lang3.StringUtils
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.fluent.Configurations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils

import javax.annotation.PostConstruct

@Component
class PolicyEnforcerConfigurationManager {
    @Autowired
    CommonConfigurationManager commonConfigurationManager

    @Autowired
    ConfigurationProperties configurationProperties

    File policyEnforcerPropertiesFile
    PropertiesConfiguration policyEnforcerConfig

    @PostConstruct
    void init() {
        def pluginsDirectory = new File(configurationProperties.currentUserDirectory, 'plugins')
        def libDirectory = new File(pluginsDirectory, 'lib')
        def configs = new Configurations()

        policyEnforcerPropertiesFile = new File(libDirectory, 'blackDuckPolicyEnforcer.properties')
        if (!policyEnforcerPropertiesFile.exists()) {
            policyEnforcerConfig = configs.properties(ResourceUtils.getFile('blackDuckPolicyEnforcer.properties'))
            libDirectory.mkdirs()
            persistPolicyEnforcerProperties()
        }
        policyEnforcerConfig = configs.properties(policyEnforcerPropertiesFile)
    }

    boolean needsUpdate() {
        return StringUtils.isBlank(configurationProperties.blackduckMetadataBlock);
    }

    void configure(Console console, PrintStream out) {
        updateValues(console, out)
        persistPolicyEnforcerProperties()
    }

    void updateValues(Console console, PrintStream out) {
        commonConfigurationManager.updateBaseConfigValues(policyEnforcerConfig, policyEnforcerPropertiesFile, console, out)

        commonConfigurationManager.setValueFromInput(console, out, 'Allow Black Duck to block artifacts that do not contain metadata from a Black Duck plugin', policyEnforcerConfig, PolicyPluginProperty.METADATA_BLOCK)
    }

    void persistPolicyEnforcerProperties() {
        policyEnforcerConfig.setProperty(PolicyPluginProperty.METADATA_BLOCK.getKey(), configurationProperties.blackduckMetadataBlock)

        commonConfigurationManager.persistConfigToFile(policyEnforcerConfig, policyEnforcerPropertiesFile)
    }
}
