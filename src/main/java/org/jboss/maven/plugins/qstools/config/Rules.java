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
package org.jboss.maven.plugins.qstools.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.QSFixer;

/**
 * @author Rafael Benevides
 * 
 */
public class Rules {

    private List<Object> configurations;

    public Rules(List<Object> configurations) {
        this.configurations = configurations;
    }

    public String getRedHatMavenProfileSnippetLocation() {
        return (String) getConfig("redhat-maven-repository-profile-location");
    }

    @SuppressWarnings("unchecked")
    public boolean isCheckerIgnored(QSChecker checker) {
        List<String> ignoredCheckers = (List<String>) getConfig("ignored-checkers");
        return ignoredCheckers.contains(checker.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public List<String> getIgnoredUnusedProperties() {
        return (List<String>) getConfig("ignored-unused-properties");
    }

    @SuppressWarnings("unchecked")
    public List<String> getIgnoredDifferentValuesProperties() {
        return (List<String>) getConfig("ignored-differet-value-properties");
    }

    public String getExcludes() {
        Object excludes = getConfig("excludes");
        return excludes.toString().replace('[', ' ').replace(']', ' ');
    }

    @SuppressWarnings("unchecked")
    public List<String> getExcludesArray() {
        List<String> excludes = (List<String>) getConfig("excludes");
        return excludes;
    }

    public String getCheckerSpecificExcludes(QSChecker module) {
        Object moduleExclude = getConfig("excludes-" + module.getClass().getSimpleName());
        return moduleExclude == null ? "" : moduleExclude.toString().replace('[', ' ').replace(']', ' ');
    }

    @SuppressWarnings("unchecked")
    public List<String> getFixerSpecificExcludesArray(QSFixer fixer) {
        List<String> moduleExclude = (List<String>) getConfig("excludes-" + fixer.getClass().getSimpleName());
        return moduleExclude == null ? new ArrayList<String>() : moduleExclude;
    }

    public String getExpectedCompilerSource() {
        return (String) getConfig("expected-compiler-source");
    }

    public String getHeaderLocation() {
        return (String) getConfig("header-file");
    }

    public String getLicenseFileLocation() {
        return (String) getConfig("license-file");
    }

    public String getEclipseFormatterProfileLocation() {
        return (String) getConfig("eclipse-formatter-location");
    }

    public String getHeaderDefinitionLocation() {
        return (String) getConfig("header-definition");
    }

    public String getGroupId() {
        return (String) getConfig("groupid");
    }

    public String getArtifactIdPrefix() {
        return (String) getConfig("artifactid-prefix");
    }

    public String getPomNamePattern() {
        return (String) getConfig("pom-name-pattern");
    }

    public String getPomNamePatternForSubmodule() {
        return (String) getConfig("pom-name-pattern-submodule");
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getFinalNamePatterns() {
        List<Object> packagingAsList = (List<Object>) getConfig("final-name-patterns");
        Map<String, String> p = new HashMap<String, String>();
        for (Object o : packagingAsList) {
            p.putAll((Map<? extends String, ? extends String>) o);
        }
        return p;
    }

    @SuppressWarnings("unchecked")
    public Properties getExpectedBomVersion() {
        List<Object> propertiesAsList = (List<Object>) getConfig("expected-bom-versions");
        Properties p = new Properties();
        for (Object o : propertiesAsList) {
            p.putAll((Map<? extends Object, ? extends Object>) o);
        }
        return p;
    }

    @SuppressWarnings("unchecked")
    public Properties getPropertiesNames() {
        List<Object> propertiesAsList = (List<Object>) getConfig("property-names");
        Properties p = new Properties();
        for (Object o : propertiesAsList) {
            p.putAll((Map<? extends Object, ? extends Object>) o);
        }
        return p;
    }

    @SuppressWarnings("unchecked")
    public List<String> getPomOrder() {
        List<String> pomOrder = (List<String>) getConfig("pom-order");
        List<String> list = new ArrayList<String>();
        for (String o : pomOrder) {
            list.add(o);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public List<String> getIgnoredModules() {
        List<String> modules = (List<String>) getConfig("ignored-modules");
        List<String> list = new ArrayList<String>();
        for (String o : modules) {
            list.add(o);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getReadmeMetadatas() {
        List<Object> metadatas = (List<Object>) getConfig("readme-metadatas");
        Map<String, String> map = new HashMap<String, String>();
        for (Object o : metadatas) {
            map.putAll((Map<? extends String, ? extends String>) o);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public Properties getProjectBomsMigration() {
        List<Object> propertiesAsList = (List<Object>) getConfig("project-boms-migration");
        Properties p = new Properties();
        for (Object o : propertiesAsList) {
            p.putAll((Map<? extends Object, ? extends Object>) o);
        }
        return p;
    }

    @SuppressWarnings("unchecked")
    public Object getConfig(String configValue) {
        Object value = null;
        // Get the overwritten non-null value
        for (Object config : configurations) {
            Object foundValue = ((Map<String, Object>) config).get(configValue);
            if (foundValue != null) {
                value = foundValue;
            }
        }
        return value;
    }

}
