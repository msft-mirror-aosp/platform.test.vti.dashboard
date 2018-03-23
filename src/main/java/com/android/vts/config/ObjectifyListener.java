/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.vts.config;

import com.android.vts.entity.TestSuiteResultEntity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/** The @WebListener annotation for registering a class as a listener of a web application. */
// @WebListener
/**
 * Initializing Objectify Service at the container start up before any web components like servlet
 * get initialized.
 */
public class ObjectifyListener implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(ObjectifyListener.class.getName());

    /**
     * Receives notification that the web application initialization process is starting. This
     * function will register Entity classes for objectify.
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ObjectifyFactory objectifyFactory = ObjectifyService.factory();
        objectifyFactory.register(TestSuiteResultEntity.class);
        objectifyFactory.begin();
        logger.log(Level.INFO, "Value Initialized from context.");
    }

    /** Receives notification that the ServletContext is about to be shut down. */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        logger.log(Level.INFO, "Value deleted from context.");
    }
}
