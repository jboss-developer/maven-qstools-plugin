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
package org.jboss.maven.plugins.qstools.fixers;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSCheckerException;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.jboss.maven.plugins.qstoolsc.common.ArtifactIdNameUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Fixer for {@link org.jboss.maven.plugins.qstools.checkers.ArtifactIdNameChecker}
 *
 * @author Paul Robinson
 */
@Component(role = QSFixer.class, hint = "ArtifactIdPrefixFixer")
public class ArtifactIdNameFixer implements QSFixer {

    protected XPath xPath = XPathFactory.newInstance().newXPath();

    @Requirement
    private ConfigurationProvider configurationProvider;

    @Requirement
    private ArtifactIdNameUtil artifactIdNameUtil;

    @Override
    public void fix(MavenProject project, MavenSession mavenSession, List<MavenProject> reactorProjects, Log log) throws QSCheckerException {

        try {
            Rules rules = configurationProvider.getQuickstartsRules(project.getGroupId());
            List<ArtifactIdNameUtil.PomInformation> pomsWithInvalidArtifactIds = artifactIdNameUtil.findAllIncorrectArtifactIdNames(reactorProjects, rules);

            //Update each incorrect artifactId
            for (ArtifactIdNameUtil.PomInformation pi : pomsWithInvalidArtifactIds) {
                Document doc = PositionalXMLReader.readXML(new FileInputStream(pi.getProject().getFile()));
                Node artifactIdNode = (Node) xPath.evaluate("/project/artifactId", doc, XPathConstants.NODE);
                artifactIdNode.setTextContent(pi.getExpectedArtifactId());
                XMLWriter.writeXML(doc, pi.getProject().getFile());
            }

            //Update all the parents, to use the changed artifactId
            for (MavenProject subProject : reactorProjects) {

                Document doc = PositionalXMLReader.readXML(new FileInputStream(subProject.getFile()));
                Node parentArtifactIdNode = (Node) xPath.evaluate("/project/parent/artifactId", doc, XPathConstants.NODE);
                if (parentArtifactIdNode != null && subProject.getParentFile() != null) {
                    Document parentDoc = PositionalXMLReader.readXML(new FileInputStream(subProject.getParentFile()));
                    Node artifactIdNode = (Node) xPath.evaluate("/project/artifactId", parentDoc, XPathConstants.NODE);

                    if (!parentArtifactIdNode.getTextContent().equals(artifactIdNode.getTextContent())) {
                        parentArtifactIdNode.setTextContent(artifactIdNode.getTextContent());
                        XMLWriter.writeXML(doc, subProject.getFile());
                    }
                }
            }
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }
    }

    @Override
    public int order() {

        return 0;
    }
}
