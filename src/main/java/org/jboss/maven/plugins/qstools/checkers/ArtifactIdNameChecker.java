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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.QSCheckerException;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstoolsc.common.ArtifactIdNameUtil;

/**
 * @author Rafael Benevides
 * @author Paul Robinson
 */
@Component(role = QSChecker.class, hint = "artifactIdNameChecker")
public class ArtifactIdNameChecker implements QSChecker {

    private int violationsQtd;

    @Requirement
    private ConfigurationProvider configurationProvider;

    @Requirement
    private ArtifactIdNameUtil artifactIdNameUtil;

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.maven.plugins.qstools.QSChecker#getViolatonsQtd()
     */
    @Override
    public int getViolatonsQtd() {

        return violationsQtd;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.maven.plugins.qstools.QSChecker#resetViolationsQtd()
     */
    @Override
    public void resetViolationsQtd() {

        violationsQtd = 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {

        return "Check if the Maven ArtifactId uses a valid name";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.maven.plugins.qstools.QSChecker#check(org.apache.maven.project.MavenProject,
     * org.apache.maven.execution.MavenSession, java.util.List, org.apache.maven.plugin.logging.Log)
     */
    @Override
    public Map<String, List<Violation>> check(MavenProject project, MavenSession mavenSession, List<MavenProject> reactorProjects, Log log) throws QSCheckerException {

        Map<String, List<Violation>> results = new TreeMap<String, List<Violation>>();

        try {

            Rules rules = configurationProvider.getQuickstartsRules(project.getGroupId());
            List<ArtifactIdNameUtil.PomInformation> pomsWithInvalidArtifactIds =  artifactIdNameUtil.findAllIncorrectArtifactIdNames(reactorProjects, rules);

            for (ArtifactIdNameUtil.PomInformation pi : pomsWithInvalidArtifactIds) {
                String rootDirectory = (mavenSession.getExecutionRootDirectory() + File.separator).replace("\\", "\\\\");
                String fileAsString = pi.getProject().getFile().getAbsolutePath().replace(rootDirectory, "");
                if (results.get(fileAsString) == null) {
                    results.put(fileAsString, new ArrayList<Violation>());
                }
                String msg = "Project with the following artifactId [%s] doesn't match the required format. It should be: [%s]";
                results.get(fileAsString).add(new Violation(getClass(), pi.getLine(), String.format(msg, pi.getActualArtifactId(), pi.getExpectedArtifactId())));
                violationsQtd++;
            }

            return results;
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }
    }

}
