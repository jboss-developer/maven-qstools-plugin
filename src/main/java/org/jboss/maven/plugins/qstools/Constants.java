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
package org.jboss.maven.plugins.qstools;

/**
 * @author Rafael Benevides
 * 
 */
public class Constants {

    public static final String STACKS_CONTEXT = "qstools.stacks";

    public static final String CONFIG_FILE_CONTEXT = "qstools.configFileURL";

    public static final String LOG_CONTEXT = "qstools.log";

    public static final String MAVEN_SESSION_CONTEXT = "qstools.mavenSession";
    
    public static final String IGNORED_QUICKSTARTS_CONTEXT = "qstools.ignored.quickstarts";

    public static final int CACHE_EXPIRES_SECONDS = 60; // 1 minute;
}
