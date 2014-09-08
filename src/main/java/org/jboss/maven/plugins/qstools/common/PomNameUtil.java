/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.maven.plugins.qstools.common;

import static org.jboss.maven.plugins.qstools.Constants.TARGET_PRODUCT_TAG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.config.Rules;

@Component(role = PomNameUtil.class)
public class PomNameUtil {
    
    @Requirement
    private ProjectUtil projectUtil;

    public String getExpectedPattern(MavenProject project, Rules rules) throws IOException {
        String pomNamePattern = rules.getPomNamePattern();
        String pomNamePatternSubmodule = rules.getPomNamePatternForSubmodule();
        String folderName = project.getBasedir().getName();
        String parentFolder = project.getBasedir().getParentFile().getName();
        String pattern;
        if (projectUtil.isSubProjec(project)) {
            // Get Target Product from parent Readme
            File parentReadme = new File(project.getBasedir().getParent(), "README.md");
            String targetProject = getTargetProduct(parentReadme);
            pattern = pomNamePatternSubmodule.replace("<target-product>", targetProject).replace("<project-folder>", parentFolder).replace("<submodule-folder>", folderName);
        } else {
            File readme = new File(project.getBasedir(), "README.md");
            if (readme.exists()) {
                String targetProject = getTargetProduct(readme);
                pattern = pomNamePattern.replace("<target-product>", targetProject).replace("<project-folder>", folderName);
            } else {
                // Not able to get the targetProject. Using the existing name to avoid wrong violations
                pattern = project.getName();
            }
        }
        return pattern;
    }

    /**
     * @param file
     * @return empty string if can't find the target product
     * @throws IOException
     */
    private String getTargetProduct(File readme) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(readme));
        try {
            while (br.ready()) {
                String line = br.readLine();
                if (line.startsWith(TARGET_PRODUCT_TAG)) {
                    return line.substring(TARGET_PRODUCT_TAG.length(), line.length()).trim();
                }
            }
            return "";
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

 
}
