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

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.Constants;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.common.ProjectUtil;
import org.jboss.maven.plugins.qstools.config.Resources;
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

@Component(role = QSFixer.class, hint = "RepositoryFixer")
public class RepositoryFixer extends AbstractBaseFixerAdapter {

    @Requirement
    private Resources resources;
    
    @Requirement
    private ProjectUtil projectUtil;


    @Override
    public String getFixerDescription() {
        return "Inject Red Hat Maven Central profile into pom.xml";
    }

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        final Log log = (Log) getContext().get(Constants.LOG_CONTEXT);
        log.debug("Fixing " + project);
        String snippet = resources.getRedHatRepositorySnippet(project.getGroupId());
        Node repositoryNode = (Node) getxPath().evaluate("/project/profiles/profile/repositories", doc, XPathConstants.NODE);
        Element projectElement = (Element) getxPath().evaluate("/project", doc, XPathConstants.NODE);
        if (repositoryNode == null && !projectUtil.isSubProjec(project)) {
            Node profilesNode = (Node) getxPath().evaluate("/project/profiles", doc, XPathConstants.NODE);
            if (profilesNode == null) {
                profilesNode = doc.createElement("profiles");
                projectElement.appendChild(doc.createTextNode("\n\n    "));
                projectElement.appendChild(profilesNode);
            }
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(snippet)));

            Node comments = doc.importNode(d.getFirstChild(), true);
            Node profile = doc.importNode(d.getDocumentElement(), true);

            profilesNode.appendChild(doc.createTextNode("\n        "));
            profilesNode.appendChild(comments);
            profilesNode.appendChild(doc.createTextNode("\n        "));
            profilesNode.appendChild(profile);

            changeDefaultProfile(doc);

            // Change Arquillian profiles
            Node arquillianProfileManaged =
                (Node) getxPath().evaluate("/project/profiles/profile/dependencies/dependency[artifactId='jboss-as-arquillian-container-managed']/../../id", doc,
                    XPathConstants.NODE);
            Node arquillianProfileRemote =
                (Node) getxPath().evaluate("/project/profiles/profile/dependencies/dependency[artifactId='jboss-as-arquillian-container-remote']/../../id", doc,
                    XPathConstants.NODE);
            if (arquillianProfileManaged != null) {
                addSurefireConfigToArquillian(doc, arquillianProfileManaged.getTextContent());
            }
            if (arquillianProfileRemote != null) {
                addSurefireConfigToArquillian(doc, arquillianProfileRemote.getTextContent());
            }
            // write changes
            XMLWriter.writeXML(doc, project.getFile());
        }
    }

    private void changeDefaultProfile(Document doc) throws XPathExpressionException {
        Node activationDefault = (Node) getxPath().evaluate("/project/profiles/profile[id='default']/activation", doc, XPathConstants.NODE);
        Element propertyNode = doc.createElement("property");
        Element nameNode = doc.createElement("name");
        nameNode.setTextContent("!default");
        propertyNode.appendChild(nameNode);
        if (activationDefault != null) {
            while (activationDefault.hasChildNodes()) {
                activationDefault.removeChild(activationDefault.getFirstChild());
            }
            activationDefault.appendChild(propertyNode);
        }
    }

    private void addSurefireConfigToArquillian(Document doc, String profile) throws XPathExpressionException {
        Element buildElement = doc.createElement("build");
        Element pluginsElement = doc.createElement("plugins");
        Element pluginElement = doc.createElement("plugin");
        Element artifactIdElement = doc.createElement("artifactId");
        artifactIdElement.setTextContent("maven-surefire-plugin");
        Element versionElement = doc.createElement("version");
        versionElement.setTextContent("${version.surefire.plugin}");
        Element configurationElement = doc.createElement("configuration");
        Element skipElement = doc.createElement("skip");
        skipElement.setTextContent("false");

        buildElement.appendChild(pluginsElement);
        pluginsElement.appendChild(pluginElement);
        pluginElement.appendChild(artifactIdElement);
        pluginElement.appendChild(versionElement);
        pluginElement.appendChild(configurationElement);
        configurationElement.appendChild(skipElement);

        Node sureFireConfig =
            (Node) getxPath().evaluate("/project/profiles/profile[id='" + profile + "']/build/plugins/plugin[artifactId='maven-surefire-plugin']/configuration", doc,
                XPathConstants.NODE);
        if (sureFireConfig != null) {
            sureFireConfig.appendChild(doc.createTextNode("    "));
            sureFireConfig.appendChild(skipElement);
        } else {
            Node arquillianProfile = (Node) getxPath().evaluate("/project/profiles/profile[id='" + profile + "']", doc, XPathConstants.NODE);
            arquillianProfile.appendChild(doc.createTextNode("    "));
            arquillianProfile.appendChild(buildElement);
        }
        
    }
}
