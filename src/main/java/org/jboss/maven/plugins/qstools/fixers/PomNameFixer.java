package org.jboss.maven.plugins.qstools.fixers;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.checkers.PomNameChecker;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.jboss.maven.plugins.qstoolsc.common.PomNameUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Fixer for {@link PomNameChecker}
 * 
 * @author rafaelbenevides
 * 
 */
@Component(role = QSFixer.class, hint = "PomNameFixer")
public class PomNameFixer extends AbstractBaseFixerAdapter {
    
    @Requirement
    private PomNameUtil pomNameUtil;

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        Rules rules = getConfigurationProvider().getQuickstartsRules(project.getGroupId());
        String pattern = pomNameUtil.getExpectedPattern(project, rules);
        if (!pattern.equals(project.getName())) {
            Node nameNode = (Node) getxPath().evaluate("/project/name", doc, XPathConstants.NODE);
            if (nameNode == null) {
                nameNode = doc.createElement("name");
                Node projectNode = (Node) getxPath().evaluate("/project", doc, XPathConstants.NODE);
                projectNode.appendChild(doc.createTextNode("    "));
                projectNode.appendChild(nameNode);
            }
            nameNode.setTextContent(pattern);
            XMLWriter.writeXML(doc, project.getFile());
        }

    }

    
}
