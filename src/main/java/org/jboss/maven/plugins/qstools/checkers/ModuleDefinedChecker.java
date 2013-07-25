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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.w3c.dom.Document;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "moduleDefinedChecker")
public class ModuleDefinedChecker extends AbstractProjectChecker {

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Checks if all project subdirectories are defined as module";
    }

    /**
     * Check if given directory is a project folder
     * 
     * @param f
     * @return
     */
    private boolean isProjectSubdir(File f) {
        return Arrays.asList(f.list()).contains("pom.xml");

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
        File rootDir = project.getBasedir();
        List<String> submodules = new ArrayList<String>();
        for (File f : rootDir.listFiles()) {
            if (f.isDirectory() && isProjectSubdir(f)) {
                submodules.add(f.getName());
            }
        }
        submodules.removeAll(getConfigurationProvider().getQuickstartsRules(project.getGroupId()).getIgnoredModules());
        for (String dir : submodules) {
            if (!project.getModules().contains(dir)) {
                String msg = "The following dir [%s] is not listed as one of project submodules";
                addViolation(project.getFile(), results, 0, String.format(msg, dir));
            }
        }
    }

}
