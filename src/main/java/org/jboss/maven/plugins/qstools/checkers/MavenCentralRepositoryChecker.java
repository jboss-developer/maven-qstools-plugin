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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "mavenCentralRepositoryChecker")
public class MavenCentralRepositoryChecker extends AbstractProjectChecker {

    @Requirement
    private RepositorySystem repositorySystem;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Checks if all dependencies are in Central Maven repository";
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
        if (!getConfigurationProvider().getQuickstartsRules(project.getGroupId()).isSkipMavenCentralRepositoryChecker()) {
            for (Dependency dependency : project.getDependencies()) {
                Artifact dependencyArtifact = repositorySystem.createProjectArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                ArtifactResolutionRequest arr = new ArtifactResolutionRequest();

                List<ArtifactRepository> remoteRepositories = new ArrayList<ArtifactRepository>();
                remoteRepositories.add(repositorySystem.createDefaultRemoteRepository());

                arr.setArtifact(dependencyArtifact).setRemoteRepositories(remoteRepositories).setLocalRepository(getMavenSession().getLocalRepository());
                ArtifactResolutionResult result = repositorySystem.resolve(arr);
                Node dependencyNode = (Node) getxPath().evaluate("//artifactId[text() ='" + dependency.getArtifactId() + "']", doc, XPathConstants.NODE);
                int lineNumber = getLineNumberFromNode(dependencyNode);
                if (!result.isSuccess()) {
                    addViolation(project.getFile(), results, lineNumber, dependency + " doesn't comes from Maven Central Repository");
                }
            }
        }
    }

}
