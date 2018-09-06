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

import com.blackducksoftware.integration.hub.api.generated.enumeration.PolicyStatusSummaryStatusType
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryPropertyService
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty
import com.blackducksoftware.integration.hub.artifactory.DateTimeManager
import com.blackducksoftware.integration.hub.artifactory.inspect.InspectionStatus
import com.blackducksoftware.integration.hub.artifactory.scan.ScanPluginProperty
import groovy.transform.Field
import org.artifactory.exception.CancelException
import org.artifactory.repo.RepoPath
import org.artifactory.request.Request

// propertiesFilePathOverride allows you to specify an absolute path to the blackDuckPolicyEnforcer.properties file.
// If this is empty, we will default to ${ARTIFACTORY_HOME}/etc/plugins/lib/blackDuckPolicyEnforcer.properties
@Field String propertiesFilePathOverride = ""
@Field DateTimeManager dateTimeManager
@Field ArtifactoryPropertyService artifactoryPropertyService
@Field BlackDuckArtifactoryConfig blackDuckArtifactoryConfig


initialize()

executions {
    /**
     * This will attempt to reload the properties file and initialize the scanner with the new values.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReloadPolicyEnforcer"
     */
    blackDuckReloadPolicyEnforcer(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckReloadScanner REST request...')

        initialize()

        log.info('...completed blackDuckReloadScanner REST request.')
    }
}

void initialize() {
    blackDuckArtifactoryConfig = new BlackDuckArtifactoryConfig()
    blackDuckArtifactoryConfig.setEtcDirectory(ctx.artifactoryHome.etcDir.toString())
    blackDuckArtifactoryConfig.setHomeDirectory(ctx.artifactoryHome.homeDir.toString())
    blackDuckArtifactoryConfig.setPluginsDirectory(ctx.artifactoryHome.pluginsDir.toString())
    blackDuckArtifactoryConfig.setThirdPartyVersion(ctx?.versionProvider?.running?.versionName?.toString())
    blackDuckArtifactoryConfig.setPluginName(this.getClass().getSimpleName())
    blackDuckArtifactoryConfig.loadProperties(propertiesFilePathOverride)

    dateTimeManager = new DateTimeManager(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.DATE_TIME_PATTERN))
    artifactoryPropertyService = new ArtifactoryPropertyService(repositories, searches, dateTimeManager)
}

download {
    beforeDownload { Request request, RepoPath repoPath ->
        def policyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS)
        if (PolicyStatusSummaryStatusType.IN_VIOLATION.name().equals(policyStatus)) {
            throw new CancelException("Black Duck Policy Enforcer has prevented the download of ${repoPath.toPath()} because it violates a policy in your Black Duck Hub.", 403)
        }

        final String metadataBlockProperty = blackDuckArtifactoryConfig.getProperty(PolicyPluginProperty.METADATA_BLOCK)
        if (metadataBlockProperty && metadataBlockProperty.toBoolean()) {
            final String inspectionStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS)
            if (inspectionStatus && (InspectionStatus.PENDING.name().equals(inspectionStatus) || InspectionStatus.FAILURE.name().equals(inspectionStatus))) {
                throw new CancelException("Black Duck Policy Enforcer has prevented the download of ${repoPath.toPath()} because of the inspection status: ${inspectionStatus}. Please try again later", 403)
            }
        }

    }
}
