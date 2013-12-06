package org.jboss.maven.plugins.qstools;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public interface QSFixer {

    /** The Plexus role identifier. */
    String ROLE = QSFixer.class.getName();

    public void fix(final MavenProject project, final MavenSession mavenSession, final List<MavenProject> reactorProjects,
        final Log log) throws QSCheckerException;

    /**
     * Defines the execution order (priority) of the fixer.
     * 
     * Higher values will be executed later then lower values)
     * 
     * @return
     */
    public int order();
    
    /**
     * Defines the description on what the fixer will fix
     * 
     * @return
     */
    public String getFixerDescription();
}
