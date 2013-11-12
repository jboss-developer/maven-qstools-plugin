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
package org.jboss.maven.plugins.qstools;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.shrinkwrap.resolver.api.NoResolvedResultException;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * This Mojo is used to check if all Dependencies declared in a </dependencyManagement> section of a BOM is resolvable.
 * 
 * @author Rafael Benevides
 * 
 */
@Mojo(name = "bom-check", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true, threadSafe = true, aggregator = false)
public class BomCheckerMojo extends AbstractMojo {

    @Component
    private MavenProject project;
    
    @Parameter(property="qstools.bom-check.failbuild", defaultValue="true")
    private boolean failbuild;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        //Shut up Shrinkwrap resolver
        Logger.getLogger("org.jboss.shrinkwrap.resolver.impl").setLevel(Level.SEVERE);
        List<NoResolvedResultException> exceptions = new ArrayList<NoResolvedResultException>();
        getLog().info("Verifying if the dependencies on project's Dependency Management section are resolvable");
        DependencyManagement depmgmt = project.getDependencyManagement();
        if (depmgmt != null) {
            List<Dependency> dependencies = depmgmt.getDependencies();
            for (Dependency dep : dependencies) {
                if (dep.getScope() != null
                    // ignore runtime/system dependencies
                    && (dep.getScope().equals("runtime") || dep.getScope().equals("system"))) {
                    getLog().debug("Ignoring runtime/system dependency " + dep);
                } else {
                    try {
                        String pkg = dep.getType() == null ? "jar" : dep.getType();
                        String gav = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + pkg + ":" + dep.getVersion();
                        getLog().debug("Trying to resolve " + gav);
                        Maven.resolver().loadPomFromFile(project.getFile()).resolve(gav).withMavenCentralRepo(true).withClassPathResolution(false).withTransitivity().asFile();
                    } catch (NoResolvedResultException e) {
                        //Collect all resolution failures
                        exceptions.add(e);
                    }
                }

            }
        }
        if (exceptions.isEmpty()) {
            getLog().info("All Dependencies were resolved");
        } else {
            getLog().info("The following dependencies where NOT resolved:");
            for (NoResolvedResultException e : exceptions) {
                getLog().info(e.getMessage());
            }
            if (failbuild){
                throw new MojoFailureException("Unresolved dependencies on project");
            }
        }

    }

}
