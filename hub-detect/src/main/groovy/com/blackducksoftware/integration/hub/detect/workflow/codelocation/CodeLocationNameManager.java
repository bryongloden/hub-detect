/**
 * hub-detect
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
package com.blackducksoftware.integration.hub.detect.workflow.codelocation;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.hub.detect.configuration.DetectConfiguration;
import com.blackducksoftware.integration.hub.detect.configuration.DetectProperty;
import com.blackducksoftware.integration.hub.detect.configuration.PropertyAuthority;
import com.synopsys.integration.util.NameVersion;

public class CodeLocationNameManager {
    private final DetectConfiguration detectConfiguration;
    private final CodeLocationNameGenerator codeLocationNameGenerator;

    private final Set<String> codeLocationNames = new HashSet<>();
    private int givenCodeLocationOverrideCount = 0;

    public CodeLocationNameManager(final DetectConfiguration detectConfiguration, final CodeLocationNameGenerator codeLocationNameGenerator) {
        this.detectConfiguration = detectConfiguration;
        this.codeLocationNameGenerator = codeLocationNameGenerator;
    }

    public String createAggregateCodeLocationName(NameVersion projectNameVersion) {
        final String aggregateCodeLocationName;
        if (useCodeLocationOverride()) {
            // The aggregate is exclusively used for the bdio and not the scans
            aggregateCodeLocationName = getNextCodeLocationOverrideName(CodeLocationNameType.BOM);
        } else {
            aggregateCodeLocationName = String.format("%s/%s Black Duck I/O Export", projectNameVersion.getName(), projectNameVersion.getVersion());
        }
        codeLocationNames.add(aggregateCodeLocationName);
        return aggregateCodeLocationName;
    }

    public String createCodeLocationName(final DetectCodeLocation detectCodeLocation, final String detectSourcePath, final String projectName, final String projectVersionName, final String prefix, final String suffix) {
        final String codeLocationName;
        if (useCodeLocationOverride() && DetectCodeLocationType.DOCKER.equals(detectCodeLocation.getCodeLocationType())) {
            codeLocationName = getNextCodeLocationOverrideName(CodeLocationNameType.DOCKER);
        } else if (useCodeLocationOverride()) {
            codeLocationName = getNextCodeLocationOverrideName(CodeLocationNameType.BOM);
        } else if (DetectCodeLocationType.DOCKER.equals(detectCodeLocation.getCodeLocationType())) {
            codeLocationName = codeLocationNameGenerator
                                   .createDockerCodeLocationName(detectCodeLocation.getSourcePath(), projectName, projectVersionName, detectCodeLocation.getDockerImage(), detectCodeLocation.getCodeLocationType(), prefix,
                                       suffix);
        } else {
            codeLocationName = codeLocationNameGenerator.createBomCodeLocationName(detectSourcePath, detectCodeLocation.getSourcePath(), detectCodeLocation.getExternalId(), detectCodeLocation.getCodeLocationType(), prefix, suffix);
        }
        codeLocationNames.add(codeLocationName);
        return codeLocationName;
    }

    public String createScanCodeLocationName(final String sourcePath, final String scanTargetPath, final String dockerTarFilename, final String projectName, final String projectVersionName, final String prefix, final String suffix) {
        final String scanCodeLocationName;

        if (useCodeLocationOverride()) {
            scanCodeLocationName = getNextCodeLocationOverrideName(CodeLocationNameType.SCAN);
        } else if (StringUtils.isNotBlank(dockerTarFilename)) {
            scanCodeLocationName = codeLocationNameGenerator.createDockerScanCodeLocationName(dockerTarFilename, projectName, projectVersionName, prefix, suffix);
        } else {
            scanCodeLocationName = codeLocationNameGenerator.createScanCodeLocationName(sourcePath, scanTargetPath, projectName, projectVersionName, prefix, suffix);
        }
        codeLocationNames.add(scanCodeLocationName);
        return scanCodeLocationName;
    }

    public String createBinaryScanCodeLocationName(final String filename, final String projectName, final String projectVersionName, final String prefix, final String suffix) {
        final String scanCodeLocationName;

        if (useCodeLocationOverride()) {
            scanCodeLocationName = getNextCodeLocationOverrideName(CodeLocationNameType.SCAN);
        } else {
            scanCodeLocationName = codeLocationNameGenerator.createBinaryScanCodeLocationName(filename, projectName, projectVersionName, prefix, suffix);
        }
        codeLocationNames.add(scanCodeLocationName);
        return scanCodeLocationName;
    }

    private boolean useCodeLocationOverride() {
        return StringUtils.isNotBlank(detectConfiguration.getProperty(DetectProperty.DETECT_CODE_LOCATION_NAME, PropertyAuthority.None));
    }

    private String getNextCodeLocationOverrideName(final CodeLocationNameType codeLocationNameType) { // returns "override", then "override 2", then "override 3", etc
        givenCodeLocationOverrideCount++;
        final String base = detectConfiguration.getProperty(DetectProperty.DETECT_CODE_LOCATION_NAME, PropertyAuthority.None) + " " + codeLocationNameType.name();
        if (givenCodeLocationOverrideCount == 1) {
            return base;
        } else {
            final String codeLocationName = base + " " + Integer.toString(givenCodeLocationOverrideCount);
            return codeLocationName;
        }
    }

    public Set<String> getCodeLocationNames() {
        return codeLocationNames;
    }
}
