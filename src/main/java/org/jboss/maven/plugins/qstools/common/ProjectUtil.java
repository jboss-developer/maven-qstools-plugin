package org.jboss.maven.plugins.qstools.common;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = ProjectUtil.class)
public class ProjectUtil {

    /**
     * HAcky way to determine if the project is a maven submodule or not. It uses the presence of root README.md to determine if
     * it is a submodule
     * 
     * @param project
     * @return
     */
    public boolean isSubProjec(MavenProject project) {
        return ((!new File(project.getBasedir(), "README.md").exists()) && (new File(project.getBasedir().getParent(), "README.md").exists()));
    }

}
