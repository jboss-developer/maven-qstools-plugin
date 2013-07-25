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
package org.jboss.maven.plugins.qstools.checkers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "pomDescriptionChecker")
public class PomNameChecker extends AbstractProjectChecker {

    private static final String TARGET_PRODUCT_TAG = "Target Product:";

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Check if the POM.xml <name> uses the defined pattern";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jboss.maven.plugins.qstools.checkers.AbstractProjectChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        Rules rules = getConfigurationProvider().getQuickstartsRules(project.getGroupId());
        String pomNamePattern = rules.getPomNamePattern();
        String pomNamePatternSubmodule = rules.getPomNamePatternForSubmodule();
        String folderName = project.getBasedir().getName();
        String parentFolder = project.getBasedir().getParentFile().getName();
        String pattern;
        if (isSubProjec(project)) {
            // Get Target Product from parent Readme
            File parentReadme = new File(project.getBasedir().getParent(), "README.md");
            String targetProject = getTargetProduct(parentReadme);
            pattern = pomNamePatternSubmodule.replace("<target-product>", targetProject).replace("<project-folder>", parentFolder).replace("<submodule-folder>", folderName);
        } else {
            File readme = new File(folderName, "README.md");
            if (readme.exists()) {
                String targetProject = getTargetProduct(readme);
                pattern = pomNamePattern.replace("<target-product>", targetProject).replace("<project-folder>", folderName);
            } else {
                // Not able to get the targetProject. Using the existing name to avoid wrong violations
                pattern = project.getName();
            }
        }
        if (!pattern.equals(project.getName())) {
            Node nameNode = (Node) getxPath().evaluate("/project/name", doc, XPathConstants.NODE);
            int lineNumber = getLineNumberFromNode(nameNode);
            String msg = "Project uses name [%s] but should use the define name: %s";
            addViolation(project.getFile(), results, lineNumber, String.format(msg, project.getName(), pattern));
        }

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

    /**
     * HAcky way to determine if the project is a maven submodule or not. It uses the presense of root README.md to determine if
     * it is a submodule
     * 
     * @param project
     * @return
     */
    private boolean isSubProjec(MavenProject project) {
        return ((!new File(project.getBasedir(), "README.md").exists())
        && (new File(project.getBasedir().getParent(), "README.md").exists()));
    }
}
