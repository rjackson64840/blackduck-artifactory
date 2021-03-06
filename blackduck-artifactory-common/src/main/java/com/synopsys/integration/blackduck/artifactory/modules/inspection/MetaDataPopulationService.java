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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.ArtifactMetaData;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.ArtifactMetaDataService;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class MetaDataPopulationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(MetaDataPopulationService.class));

    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final CacheInspectorService cacheInspectorService;
    private final ArtifactMetaDataService artifactMetaDataService;

    public MetaDataPopulationService(final ArtifactoryPropertyService artifactoryPropertyService, final CacheInspectorService cacheInspectorService, final ArtifactMetaDataService artifactMetaDataService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.artifactMetaDataService = artifactMetaDataService;
        this.cacheInspectorService = cacheInspectorService;
    }

    public void populateMetadata(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        final Optional<InspectionStatus> inspectionStatus = cacheInspectorService.getInspectionStatus(repoKeyPath);

        if (inspectionStatus.isPresent() && inspectionStatus.get().equals(InspectionStatus.PENDING)) {
            logger.debug(String.format("Populating metadata on repoKey: %s", repoKey));
            try {
                final String projectName = cacheInspectorService.getRepoProjectName(repoKey);
                final String projectVersionName = cacheInspectorService.getRepoProjectVersionName(repoKey);

                final List<ArtifactMetaData> artifactMetaDataList = artifactMetaDataService.getArtifactMetadataOfRepository(repoKey, projectName, projectVersionName);
                populateBlackDuckMetadataFromIdMetadata(repoKey, artifactMetaDataList);

                cacheInspectorService.setInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS);
            } catch (final Exception e) {
                logger.error(String.format("The Black Duck %s encountered a problem while populating artifact metadata in repository %s", InspectionModule.class.getSimpleName(), repoKey));
                logger.debug(e.getMessage(), e);
                cacheInspectorService.setInspectionStatus(repoKeyPath, InspectionStatus.FAILURE);
            }
        }
    }

    public void populateBlackDuckMetadata(final RepoPath repoPath, final VulnerabilityAggregate vulnerabilityAggregate, final PolicySummaryStatusType policySummaryStatusType, final String componentVersionUrl) {
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, Integer.toString(vulnerabilityAggregate.getHighSeverityCount()), logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, Integer.toString(vulnerabilityAggregate.getMediumSeverityCount()), logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, Integer.toString(vulnerabilityAggregate.getLowSeverityCount()), logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, policySummaryStatusType.toString(), logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL, componentVersionUrl, logger);
        cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.SUCCESS);
    }

    public void populateBlackDuckMetadataFromIdMetadata(final String repoKey, final List<ArtifactMetaData> artifactMetaDataList) {
        for (final ArtifactMetaData artifactMetaData : artifactMetaDataList) {
            if (StringUtils.isNoneBlank(artifactMetaData.originId, artifactMetaData.forge)) {
                final SetMultimap<String, String> setMultimap = HashMultimap.create();
                setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.getName(), artifactMetaData.originId);
                setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.getName(), artifactMetaData.forge);
                final List<RepoPath> artifactsWithOriginId = artifactoryPropertyService.getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKey);
                for (final RepoPath repoPath : artifactsWithOriginId) {
                    final VulnerabilityAggregate vulnerabilityAggregate = new VulnerabilityAggregate(artifactMetaData.highSeverityCount, artifactMetaData.mediumSeverityCount, artifactMetaData.lowSeverityCount);
                    populateBlackDuckMetadata(repoPath, vulnerabilityAggregate, artifactMetaData.policyStatus, artifactMetaData.componentVersionLink);
                }
            }
        }
    }

}
