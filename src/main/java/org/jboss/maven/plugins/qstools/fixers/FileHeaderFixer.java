package org.jboss.maven.plugins.qstools.fixers;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.jboss.maven.plugins.qstools.Constants;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.QSCheckerException;
import org.jboss.maven.plugins.qstools.checkers.FileHeaderChecker;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.jboss.maven.plugins.qstools.config.Rules;

/**
 * Fixer for {@link FileHeaderChecker}
 * 
 * @author rafaelbenevides
 * 
 */
@Component(role = QSFixer.class, hint = "FileHeaderFixer")
public class FileHeaderFixer implements QSFixer {

    private BuildPluginManager pluginManager;

    @Requirement
    private Context context;

    @Requirement
    private ConfigurationProvider configurationProvider;

    @Override
    public void fix(MavenProject project, MavenSession mavenSession, List<MavenProject> reactorProjects, Log log)
        throws QSCheckerException {
        Rules rules = configurationProvider.getQuickstartsRules(project.getGroupId());
        // Execute License-Maven-Plugin - http://code.mycila.com/license-maven-plugin/reports/2.3/format-mojo.html
        try {
            // Get Excluded files
            List<Element> excludes = new ArrayList<Element>();
            for (String exclude : rules.getExcludesArray()) {
                excludes.add(element("exclude", exclude));
            }
            for (String exclude : rules.getFixerSpecificExcludesArray(this)) {
                excludes.add(element("exclude", exclude));
            }
            List<Element> includes = new ArrayList<Element>();

            // Get includes extensions
            String[] includesExtensions = new String[] {
                "**/*.xml",
                "**/*.xsd",
                "**/*.java",
                "**/*.js",
                "**/*.properties",
                "**/*.html",
                "**/*.xhtml",
                "**/*.sql",
                "**/*.css" };

            for (String include : includesExtensions) {
                includes.add(element("include", include));
            }

            pluginManager = (BuildPluginManager) context.get(Constants.PLUGIN_MANAGER);
            executeMojo(plugin(groupId("com.mycila"), artifactId("license-maven-plugin"), version("2.5")),
                goal("format"),
                configuration(element(name("header"), rules.getLicenseFileLocation()),
                    element(name("aggregate"), "true"),
                    element(name("strictCheck"), "true"),
                    element(name("encoding"), "utf-8"),
                    element(name("headerDefinitions"), element(name("headerDefinition"), rules.getHeaderDefinitionLocation())),
                    element(name("includes"), includes.toArray(new Element[] {})),
                    element(name("excludes"), excludes.toArray(new Element[] {}))),
                executionEnvironment(project, mavenSession, pluginManager));
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }

    }

    @Override
    public int order() {
        return 0;
    }

}
