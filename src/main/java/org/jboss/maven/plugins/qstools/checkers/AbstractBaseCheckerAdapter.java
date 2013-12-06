package org.jboss.maven.plugins.qstools.checkers;

import org.apache.maven.project.MavenProject;
import org.jboss.maven.plugins.qstools.AbstractProjectWalker;
import org.w3c.dom.Document;

public abstract class AbstractBaseCheckerAdapter extends AbstractProjectWalker {

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        // Empty method
    }

    @Override
    public String getFixerDescription() {
        // Empty method
        return null;
    }

    @Override
    public int order() {
        return 0;
    }
    
}
