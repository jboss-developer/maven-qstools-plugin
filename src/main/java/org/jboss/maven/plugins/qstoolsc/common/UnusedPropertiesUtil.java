package org.jboss.maven.plugins.qstoolsc.common;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author paul.robinson@redhat.com 27/11/2013
 */
@Component(role = UnusedPropertiesUtil.class)
public class UnusedPropertiesUtil {

    protected XPath xPath = XPathFactory.newInstance().newXPath();

    private Set<String> usedProperties = new HashSet<String>();

    public List<PomInformation> findUnusedProperties(List<MavenProject> reactorProjects, Rules rules) throws Exception {

        List<PomInformation> unusedPropertyInfo = new ArrayList<PomInformation>();
        Map<String, List<PomInformation>> declaredProperties = new HashMap<String, List<PomInformation>>();

        for (MavenProject mavenProject : reactorProjects) {
            Document doc = PositionalXMLReader.readXML(new FileInputStream(mavenProject.getFile()));
            NodeList propertiesNodes = (NodeList) xPath.evaluate("/project/properties/*", doc, XPathConstants.NODESET);
            NodeList allNodes = (NodeList) xPath.evaluate("//*", doc, XPathConstants.NODESET);
            // find all declared properties
            for (int x = 0; x < propertiesNodes.getLength(); x++) {
                Node property = propertiesNodes.item(x);
                String propertyName = property.getNodeName();
                int lineNumber = (Integer) property.getUserData(PositionalXMLReader.BEGIN_LINE_NUMBER_KEY_NAME);
                PomInformation pi = new PomInformation(mavenProject, lineNumber, property.getNodeName());
                if (declaredProperties.get(propertyName) == null) {
                    declaredProperties.put(propertyName, new ArrayList<PomInformation>());
                }
                declaredProperties.get(propertyName).add(pi);
            }
            // find all uses for properties expression
            Pattern p = Pattern.compile("\\$\\{\\w+(.\\w+)*(-\\w+)*\\}");
            for (int x = 0; x < allNodes.getLength(); x++) {
                Node node = allNodes.item(x);
                String nodeContent = node.getTextContent();
                if (p.matcher(nodeContent).matches()) {
                    String usedProperty = node.getTextContent().replaceAll("[${}]", "");
                    usedProperties.add(usedProperty);
                }
            }
        }
        // search if all declared properties have been used
        for (String declared : declaredProperties.keySet()) {
            if (!declared.startsWith("project") && // Escape project configuration
                    !rules.getIgnoredUnusedProperties().contains(declared) && // Escape ignored properties
                    !usedProperties.contains(declared)) {

                unusedPropertyInfo.addAll(declaredProperties.get(declared));
            }
        }
        return unusedPropertyInfo;
    }

    public class PomInformation {

        private MavenProject project;

        private int line;

        private String property;

        public PomInformation(MavenProject project, int line, String property) {

            this.project = project;
            this.line = line;
            this.property = property;
        }

        public MavenProject getProject() {

            return project;
        }

        public int getLine() {

            return line;
        }

        public String getProperty() {

            return property;
        }
    }
}
