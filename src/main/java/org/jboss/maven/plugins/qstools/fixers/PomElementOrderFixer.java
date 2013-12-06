package org.jboss.maven.plugins.qstools.fixers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.jboss.maven.plugins.qstoolsc.common.PomOrderUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@Component(role = QSFixer.class, hint = "PomElementOrderFixer")
public class PomElementOrderFixer extends AbstractBaseFixerAdapter {

    @Requirement
    private PomOrderUtil pomOrderUtil;
    
    @Override
    public String getFixerDescription() {
        return "Fix the pom.xml element order";
    }

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        List<String> pomElementsOrder = getConfigurationProvider().getQuickstartsRules(project.getGroupId()).getPomOrder();
        Map<String, Node> elementsFound = pomOrderUtil.getElementsOrder(project, doc, pomElementsOrder);
        List<String> elementsList = new ArrayList<String>(elementsFound.keySet());

        for (String element : elementsList) {
            for (String anotherElement : elementsList) {
                if (elementsList.indexOf(element) < elementsList.indexOf(anotherElement)) {
                    Node elementNode = (Node) getxPath().evaluate("/project/" + element, doc, XPathConstants.NODE);
                    removePreviousWhiteSpace(elementNode);

                    Node anotherElementNode = (Node) getxPath().evaluate("/project/" + anotherElement, doc, XPathConstants.NODE);
                    anotherElementNode.getParentNode().insertBefore(elementNode, anotherElementNode);
                }
            }
        }
        XMLWriter.writeXML(doc, project.getFile());
    }

    @Override
    public int order() {
        return 10;
    }

}
