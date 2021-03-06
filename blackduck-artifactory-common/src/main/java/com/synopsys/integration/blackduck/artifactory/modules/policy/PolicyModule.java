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
package com.synopsys.integration.blackduck.artifactory.modules.policy;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.FeatureAnalyticsCollector;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class PolicyModule implements Module {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final PolicyModuleConfig policyModuleConfig;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final FeatureAnalyticsCollector featureAnalyticsCollector;

    public PolicyModule(final PolicyModuleConfig policyModuleConfig, final ArtifactoryPropertyService artifactoryPropertyService, final FeatureAnalyticsCollector featureAnalyticsCollector) {
        this.policyModuleConfig = policyModuleConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.featureAnalyticsCollector = featureAnalyticsCollector;
    }

    public void handleBeforeDownloadEvent(final RepoPath repoPath) throws CancelException {
        String reason = null;
        BlockReason blockReason = BlockReason.NO_BLOCK;
        if (shouldCancelOnPolicyViolation(repoPath)) {
            reason = "because it violates a policy in your Black Duck Hub.";
            blockReason = BlockReason.IN_VIOLATION;
        } else if (shouldCancelOnMetadataBlock(repoPath)) {
            reason = "because it lacks BlackDuck metadata";
            blockReason = BlockReason.METADATA_BLOCK;
        }

        featureAnalyticsCollector.logFeatureHit("handleBeforeDownloadEvent", blockReason.toString());

        if (reason != null) {
            throw new CancelException(String.format("The Black Duck %s has prevented the download of %s %s", PolicyModule.class.getSimpleName(), repoPath.toPath(), reason), 403);
        }
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Arrays.asList(featureAnalyticsCollector);
    }

    @Override
    public PolicyModuleConfig getModuleConfig() {
        return policyModuleConfig;
    }

    private boolean shouldCancelOnPolicyViolation(final RepoPath repoPath) {
        final Optional<String> policyStatusProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, logger);

        return policyStatusProperty
                   .filter(policyStatus -> policyStatus.equalsIgnoreCase(PolicySummaryStatusType.IN_VIOLATION.name()))
                   .isPresent();
    }

    // TODO: DISABLED: This should aways return false due to validation. Add the blackduck.artifactory.policy.metadata.block=false property to blackDuckPlugin.properties
    private boolean shouldCancelOnMetadataBlock(final RepoPath repoPath) {
        final boolean missingMetadata = !artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, logger).isPresent();
        final boolean shouldBlock = policyModuleConfig.isMetadataBlockEnabled();

        return shouldBlock && missingMetadata;
    }

    private enum BlockReason {
        IN_VIOLATION,
        METADATA_BLOCK,
        NO_BLOCK
    }
}
