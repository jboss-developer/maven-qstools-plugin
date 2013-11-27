package org.jboss.maven.plugins.qstools.fixers;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.checkers.IndentationChecker;
import org.jboss.maven.plugins.qstools.checkers.TabSpaceChecker;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.io.Files;

/**
 * Fixer for {@link TabSpaceChecker} and {@link IndentationChecker}
 * 
 * @author rafaelbenevides
 * 
 */
@Component(role = QSFixer.class, hint = "FileFormatFixer")
public class FileFormatFixer extends AbstractBaseFixerAdapter {

    @SuppressWarnings("unchecked")
    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        Rules rules = getConfigurationProvider().getQuickstartsRules(project.getGroupId());
        // Read DefaultEclipseSettings
        Map<String, String> options = DefaultCodeFormatterConstants.getEclipseDefaultSettings();

        // initialize the compiler settings to be able to format 1.6 code
        String compilerSource = rules.getExpectedCompilerSource();
        options.put(JavaCore.COMPILER_COMPLIANCE, compilerSource);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, compilerSource);
        options.put(JavaCore.COMPILER_SOURCE, compilerSource);

        // Configure CodeFormatter with Eclipse XML Formatter Profile
        InputStream xmlInputStream = getConfigurationProvider().getFileInputStream(new URL(rules.getEclipseFormatterProfileLocation()));
        Document formatterSettingsDoc = PositionalXMLReader.readXML(xmlInputStream);
        NodeList settingsNodes = formatterSettingsDoc.getElementsByTagName("setting");
        for (int i = 0; i < settingsNodes.getLength(); i++) {
            Node node = settingsNodes.item(i);
            String id = node.getAttributes().getNamedItem("id").getTextContent();
            String value = node.getAttributes().getNamedItem("value").getTextContent();
            options.put(id, value);
        }

        // Instantiate the default code formatter with the given options
        CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);

        // Apply the formatter to every Java source under the project's folder
        List<File> javaSources = FileUtils.getFiles(project.getBasedir(), "**/*.java", "");
        for (File javaSource : javaSources) {
            String source = Files.toString(javaSource, Charset.forName("UTF-8"));
            TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, // format a compilation unit
                source, // source to format
                0, // starting position
                source.length(), // length
                0, // initial indentation
                System.getProperty("line.separator") // line separator
            );

            IDocument document = new org.eclipse.jface.text.Document(source);
            edit.apply(document);
            Files.write(document.get(), javaSource, Charset.forName("UTF-8"));
        }

    }
}
