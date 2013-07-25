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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "pomElementOrderChecker")
public class PomElementOrderChecker extends AbstractProjectChecker {

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Checks if POM xml elements are in specific order";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.checkers.AbstractPomChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        List<String> pomElements = getConfigurationProvider().getQuickstartsRules(project.getGroupId()).getPomOrder();
        Map<String, Integer> elementsFound = new LinkedHashMap<String, Integer>();
        // Find all elements position
        for (String element : pomElements) {
            Node elementNode = (Node) getxPath().evaluate("/project/" + element, doc, XPathConstants.NODE);
            if (elementNode != null) {
                int lineNumber = getLineNumberFromNode(elementNode);
                elementsFound.put(element, lineNumber);
            }

        }
        // Compare found elements order
        String previousElement = null;
        for (String element : elementsFound.keySet()) {
            int lineNumber = elementsFound.get(element);
            if (previousElement != null) {
                int previousElementLineNumber = elementsFound.get(previousElement);
                if (lineNumber < previousElementLineNumber) {
                    String msg = "Element [%s] is not in the correct order: " + pomElements + ". It shoud come after [%s] on line %s";
                    addViolation(project.getFile(), results, lineNumber, String.format(msg, element, previousElement, previousElementLineNumber));
                }
            }
            previousElement = element;
        }
    }
}
