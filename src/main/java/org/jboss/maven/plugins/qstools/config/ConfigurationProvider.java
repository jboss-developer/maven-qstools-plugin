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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.jboss.maven.plugins.qstools.Constants;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = ConfigurationProvider.class)
public class ConfigurationProvider {

    @Requirement
    private Context context;
    
    @Requirement
    private Resources resources;

    private Log log;

    private Map<String, Rules> configRules = new HashMap<String, Rules>();

    private URL configFileURL;

    private void configure() throws ContextException {
        configFileURL = (URL) context.get(Constants.CONFIG_FILE_CONTEXT);
        log = (Log) context.get(Constants.LOG_CONTEXT);
    }

    /**
     * Return the {@link Rules} which should be used for a Quickstarts/Project
     * 
     * @param groupId - the groupId of the Quickstart/Project
     * 
     * @return
     */
    public Rules getQuickstartsRules(String groupId) {
        if (configFileURL == null) {
            try {
                configure();
            } catch (ContextException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
        Rules rules = configRules.get(groupId);
        if (rules == null) {
            rules = initializeConfig(groupId);
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private Rules initializeConfig(String groupId) {
        InputStream inputStream = null;
        try {
            // Retrieve inputStream (local cache or remote)
            inputStream = resources.getFileInputStream(configFileURL);
            Yaml yaml = new Yaml();
            Map<String, Object> configFile = (Map<String, Object>) yaml.load(inputStream);
            Map<String, Object> quickstartsGroupIds = (Map<String, Object>) configFile.get("quickstarts");
            List<Object> configs = (List<Object>) quickstartsGroupIds.get(groupId);
            if (configs == null) {
                Map<String, Object> defaultRulesSection = (Map<String, Object>) ((List<Object>) configFile.get("rules")).get(0);
                List<Object> defaultRules = new LinkedList<Object>();
                defaultRules.add(defaultRulesSection);
                configs = defaultRules;
            }
            Rules rules = new Rules(configs);
            configRules.put(groupId, rules);
            return configRules.get(groupId);
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException", e);
            return null;
        } catch (ContextException e) {
            log.error("ContextException", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Something bad happened when closing the inputstream", e);
                }
            }
        }
    }

}
