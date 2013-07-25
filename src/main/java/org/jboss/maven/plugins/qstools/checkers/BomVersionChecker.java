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
import org.jboss.jdf.stacks.model.Bom;
import org.jboss.jdf.stacks.model.Stacks;
import org.jboss.maven.plugins.qstools.Constants;
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
@Component(role = QSChecker.class, hint = "bomVersionChecker")
public class BomVersionChecker extends AbstractProjectChecker {

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.checkers.AbstractPomChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        Properties expectedBomVersions = getConfigurationProvider().getQuickstartsRules(project.getGroupId()).getExpectedBomVersion();
        NodeList dependencies = (NodeList) getxPath().evaluate("/project/dependencyManagement/dependencies/dependency", doc, XPathConstants.NODESET);
        // Iterate over all Declared Managed Dependencies
        for (int x = 0; x < dependencies.getLength(); x++) {
            Node dependency = dependencies.item(x);
            MavenDependency mavenDependency = getDependencyProvider().getDependencyFromNode(project, dependency);
            // use stacks to find if the project is using a jdf bom
            Bom bomUsed = null;
            Stacks stacks = (Stacks) getContext().get(Constants.STACKS_CONTEXT);
            for (Bom bom : stacks.getAvailableBoms()) {
                if (bom.getGroupId().equals(mavenDependency.getGroupId()) && bom.getArtifactId().equals(mavenDependency.getArtifactId())) {
                    bomUsed = bom;
                }
            }
            int lineNumber = getLineNumberFromNode(dependency);
            if (bomUsed == null // No JDF Bom used
                && !mavenDependency.getGroupId().startsWith("org.jboss") // Escape jboss boms
                && !mavenDependency.getGroupId().startsWith(project.getGroupId()) // Escape projects with same groupId
                                                                                  // (subprojects)
            ) {
                addViolation(project.getFile(), results, lineNumber, mavenDependency + " isn't a JBoss/JDF BOM");
            } else if (bomUsed != null) {
                String expectedBomVersion = expectedBomVersions.getProperty(bomUsed.getGroupId());
                if (expectedBomVersion != null && !mavenDependency.getInterpoledVersion().equals(expectedBomVersion)) {
                    String violationMsg = String.format("BOM %s isn't using the expected version %s", mavenDependency, expectedBomVersion);
                    addViolation(project.getFile(), results, lineNumber, violationMsg);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Check and verify if all quickstarts are using the recommended BOM version";
    }

}
