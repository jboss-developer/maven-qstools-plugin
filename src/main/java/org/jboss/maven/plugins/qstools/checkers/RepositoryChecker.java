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

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.common.ProjectUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Rafael Benevides
 *
 */
@Component(role = QSChecker.class, hint = "RepositoryChecker")
public class RepositoryChecker extends AbstractBaseCheckerAdapter {

    @Requirement
    private ProjectUtil projectUtil;

    @Override
    public String getCheckerDescription() {
        return "Check if the Quickstart Contains the JBoss Maven Repository profile";
    }

    @Override
    public void checkProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        Node repositoryNode = (Node) getxPath().evaluate("/project/profiles/profile/repositories", doc, XPathConstants.NODE);
        // only valid for top-level projects
        if (!projectUtil.isSubProjec(project) && repositoryNode == null) {
            addViolation(project.getFile(), results, 0, "pom.xml doesn't contains the JBoss Maven Repository profile");
        }
    }

}
