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

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.checkers.LicenseChecker;
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Fixer for {@link LicenseChecker}
 * 
 * @author rafaelbenevides
 * 
 */
@Component(role = QSFixer.class, hint = "LicenseFixer")
public class LicenseFixer extends AbstractBaseFixerAdapter {

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        Node licenseURL = (Node) getxPath().evaluate("/project/licenses/license/url", doc, XPathConstants.NODE);
        if (licenseURL == null || !licenseURL.getTextContent().contains("apache")) {
            Node licensesElement = (Node) getxPath().evaluate("/project/licenses", doc, XPathConstants.NODE);
            // Create <licenses/> if it doesn't exists
            if (licensesElement == null) {
                licensesElement = doc.createElement("licenses");
                Element projectElement = (Element) getxPath().evaluate("/project", doc, XPathConstants.NODE);
                projectElement.appendChild(doc.createTextNode("\n\n    "));
                projectElement.appendChild(licensesElement);
            }
            Element license = doc.createElement("license");

            Element name = doc.createElement("name");
            name.setTextContent("Apache License, Version 2.0");

            Element distribution = doc.createElement("distribution");
            distribution.setTextContent("repo");

            Element url = doc.createElement("url");
            url.setTextContent("http://www.apache.org/licenses/LICENSE-2.0.html");

            license.appendChild(name);
            license.appendChild(distribution);
            license.appendChild(url);

            licensesElement.appendChild(doc.createTextNode("\n        ")); // LF + 8 spaces
            licensesElement.appendChild(license);
        }
        XMLWriter.writeXML(doc, project.getFile());
    }

}
