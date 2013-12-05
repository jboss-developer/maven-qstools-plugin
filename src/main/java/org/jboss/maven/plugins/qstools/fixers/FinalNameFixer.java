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
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathConstants;

/**
 * Fixer for {@link org.jboss.maven.plugins.qstools.checkers.FinalNameChecker}
 * 
 * @author Paul Robinson
 */
@Component(role = QSFixer.class, hint = "FinalNameFixer")
public class FinalNameFixer extends AbstractBaseFixerAdapter {

    @Override
    public String getFixerDescription() {
        return "Fix <finalName/> on all pom.xml files";
    }

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {

        String packaging = project.getPackaging();
        String expectedFinalName = getConfigurationProvider().getQuickstartsRules(project.getGroupId())
            .getFinalNamePatterns()
            .get(packaging);

        Node finalNameNode = (Node) getxPath().evaluate("//finalName", doc, XPathConstants.NODE);
        String declaredFinalName = finalNameNode == null ? project.getBuild().getFinalName() : finalNameNode.getTextContent();

        if (expectedFinalName != null && !expectedFinalName.equals(declaredFinalName)) {

            Node buildNode = (Node) getxPath().evaluate("/project/build", doc, XPathConstants.NODE);
            if (buildNode == null) {
                buildNode = doc.createElement("build");
                Node projectNode = (Node) getxPath().evaluate("/project", doc, XPathConstants.NODE);
                projectNode.appendChild(doc.createTextNode("    "));
                projectNode.appendChild(buildNode);
            }

            finalNameNode = (Node) getxPath().evaluate("/project/build/finalName", doc, XPathConstants.NODE);
            if (finalNameNode == null) {
                finalNameNode = doc.createElement("finalName");
                buildNode.insertBefore(finalNameNode, buildNode.getFirstChild());
            }

            finalNameNode.setTextContent(expectedFinalName);
        }

        XMLWriter.writeXML(doc, project.getFile());
    }
}
