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

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.xml.XMLUtil;
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;

/**
 * Fixer for {@link org.jboss.maven.plugins.qstools.checkers.MavenCompilerChecker}
 * 
 * @author Paul Robinson
 */
@Component(role = QSFixer.class, hint = "MavenCompilerFixer")
public class MavenCompilerFixer extends AbstractBaseFixerAdapter {

    @Override
    public String getFixerDescription() {
        return "Fix the maven.compiler.(source|target) from pom.xml files";
    }

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {

        String compilerSource = getConfigurationProvider().getQuickstartsRules(project.getGroupId()).getExpectedCompilerSource();

        ensurePropertiesElementExists(doc);
        ensurePropertySet(doc, "maven.compiler.target", compilerSource);
        ensurePropertySet(doc, "maven.compiler.source", compilerSource);

        Node pluginsNode = (Node) getxPath().evaluate("/project/build/plugins", doc, XPathConstants.NODE);
        Node compilerNode = (Node) getxPath().evaluate("/project/build/plugins/plugin[artifactId='maven-compiler-plugin']", doc, XPathConstants.NODE);
        Node compilerConfigNode = (Node) getxPath().evaluate("/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/./configuration", doc, XPathConstants.NODE);

        if (compilerNode != null && compilerConfigNode == null) {
            XMLUtil.removePreviousWhiteSpace(compilerNode);
            pluginsNode.removeChild(compilerNode);
        } else if (compilerConfigNode != null) {
            removeConfigIfPresent(compilerConfigNode, "target");
            removeConfigIfPresent(compilerConfigNode, "source");
        }

        // Remove compiler plugin if it doesn't have any configuration
        if (compilerConfigNode != null && compilerConfigNode.getChildNodes().getLength() == 1) {
            XMLUtil.removePreviousWhiteSpace(compilerNode);
            compilerNode.getParentNode().removeChild(compilerNode);
        }

        XMLWriter.writeXML(doc, project.getFile());
    }

    private void removeConfigIfPresent(Node compilerConfigNode, String configItem) throws Exception {

        NodeList configs = compilerConfigNode.getChildNodes();
        for (int i = 0; i < configs.getLength(); i++) {
            Node config = configs.item(i);
            if (config.getNodeName().equals(configItem)) {
                XMLUtil.removePreviousWhiteSpace(config);
                compilerConfigNode.removeChild(config);
            }
        }
    }

    private void ensurePropertySet(Document doc, String key, String value) throws Exception {

        Element propertiesElement = (Element) getxPath().evaluate("/project/properties", doc, XPathConstants.NODE);
        Element property = (Element) getxPath().evaluate("/project/properties/"+key, doc, XPathConstants.NODE);

        if (property == null) {
            Element targetElement = doc.createElement(key);
            targetElement.setTextContent(value);
            propertiesElement.appendChild(doc.createTextNode("    "));
            propertiesElement.appendChild(targetElement);
            propertiesElement.appendChild(doc.createTextNode("\n    "));
        } else if (!property.getTextContent().equals(value)) {
            property.setTextContent(value);
        }
    }

    private void ensurePropertiesElementExists(Document doc) throws Exception {

        Element propertiesElement = (Element) getxPath().evaluate("/project/properties", doc, XPathConstants.NODE);

        if (propertiesElement == null) {
            propertiesElement = doc.createElement("properties");
            Node projectElement = (Node) getxPath().evaluate("/project", doc, XPathConstants.NODE);
            projectElement.appendChild(doc.createTextNode("\n\n    "));
            projectElement.appendChild(propertiesElement);
            propertiesElement.appendChild(doc.createTextNode("\n    "));
        }
    }

}
