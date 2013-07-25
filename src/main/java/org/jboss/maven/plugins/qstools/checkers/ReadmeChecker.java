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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.w3c.dom.Document;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "readmeChecker")
public class ReadmeChecker extends AbstractProjectChecker {

    private String regexPattern;

    private String folderName;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Checks if README.md metadata is defined and if the title matches the folder name";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jboss.maven.plugins.qstools.checkers.AbstractProjectChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        folderName = project.getBasedir().getName() + ":";
        setupRegexPattern(project.getGroupId());
        File readme = new File(project.getBasedir(), "README.md");
        if (readme.exists()) {
            checkReadmeFile(project.getGroupId(), readme, results);
        }
    }

    /**
     * Create a regex pattern using all metadata clauses
     * 
     * Format: metadata1:|metadata2:|metadata3:
     * 
     * @param string
     * 
     * @param string
     */
    private void setupRegexPattern(String groupid) {
        Map<String, String> metadatas = getConfigurationProvider().getQuickstartsRules(groupid).getReadmeMetadatas();
        StringBuilder sb = new StringBuilder();
        for (String metadata : metadatas.keySet()) {
            sb.append(metadata + "|");
        }
        sb.append(folderName);
        regexPattern = sb.toString();
    }

    /**
     * Check if the file contains all defined metadata
     */
    private void checkReadmeFile(String groupId, File readme, Map<String, List<Violation>> results) throws IOException {
        Map<String, String> metadatas = getConfigurationProvider().getQuickstartsRules(groupId).getReadmeMetadatas();
        BufferedReader br = new BufferedReader(new FileReader(readme));
        try {
            Pattern p = Pattern.compile(regexPattern);
            List<String> usedPatterns = new ArrayList<String>();
            Map<String, String> usedValues = new HashMap<String, String>();
            while (br.ready()) {
                String line = br.readLine();
                Matcher m = p.matcher(line);
                if (m.find()) {
                    usedPatterns.add(m.group());
                    usedValues.put(m.group(), line.substring(m.group().length(), line.length()).trim());
                }
            }
            for (String metadata : metadatas.keySet()) {
                if (usedPatterns.contains(metadata)) {
                    String value = usedValues.get(metadata);
                    String expected = metadatas.get(metadata);
                    if (!value.matches(expected)) {
                        String msg = "Content for metadata [%s = %s] should follow the [%s] pattern";
                        addViolation(readme, results, 0, String.format(msg, metadata, value, expected));
                    }
                } else {
                    String msg = "File doesn't contain [%s] metadata";
                    addViolation(readme, results, 3, String.format(msg, metadata));

                }
            }
            if (!usedPatterns.contains(folderName)) {
                String msg = "Readme title doesn't match the folder name: " + folderName;
                addViolation(readme, results, 1, msg);
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }

    }

}
