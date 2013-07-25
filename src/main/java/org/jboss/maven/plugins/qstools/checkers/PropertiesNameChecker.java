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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.maven.MavenDependency;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "propertiesNameChecker")
public class PropertiesNameChecker extends AbstractProjectChecker {

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Checks if POM properties are using standard names";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.checkers.AbstractPomChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        Properties recommendedPropertiesNames = getConfigurationProvider().getQuickstartsRules(project.getGroupId()).getPropertiesNames();
        NodeList dependencies = (NodeList) getxPath().evaluate("//dependencies/dependency| //plugins/plugin ", doc, XPathConstants.NODESET);
        // Iterate over all Declared Dependencies
        for (int x = 0; x < dependencies.getLength(); x++) {
            Node dependency = dependencies.item(x);
            MavenDependency mavenDependency = getDependencyProvider().getDependencyFromNode(project, dependency);
            String groupId = mavenDependency.getGroupId();
            String artifactId = mavenDependency.getArtifactId();
            String version = mavenDependency.getDeclaredVersion() == null ? null : mavenDependency.getDeclaredVersion().replaceAll("[${}]", "");
            String groupArtifactId = groupId + "|" + artifactId;

            if (groupId != null && artifactId != null && version != null // If the dependency has a GAV
                // that we manage
                && (recommendedPropertiesNames.containsKey(groupArtifactId) || recommendedPropertiesNames.containsKey(groupId))) {
                String recommendedNameGA = (String) recommendedPropertiesNames.get(groupArtifactId);
                String recommendedNameG = (String) recommendedPropertiesNames.get(groupId);
                boolean wrongVersionName = false;
                if (recommendedNameGA != null && !recommendedNameGA.equals(version)) {
                    wrongVersionName = true;
                }
                // Just check GroupId only if don't find GroupId+ArtifactId
                if (recommendedNameGA == null && recommendedNameG != null && !recommendedNameG.equals(version)) {
                    wrongVersionName = true;
                }
                if (wrongVersionName) {
                    int lineNumber = getLineNumberFromNode(dependency);
                    String msg = "Version for [%s:%s:%s] isn't using the recommended property name: %s";
                    // GroupId + ArtifacIt has precedence
                    String recommendedName = recommendedNameGA != null ? recommendedNameGA : recommendedNameG;
                    addViolation(project.getFile(), results, lineNumber, String.format(msg, groupId, artifactId, mavenDependency.getDeclaredVersion(), recommendedName));
                }
            }
        }
    }
}
