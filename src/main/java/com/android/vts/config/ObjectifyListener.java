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

import com.android.vts.entity.CoverageEntity;
import com.android.vts.entity.RoleEntity;
import com.android.vts.entity.TestCoverageStatusEntity;
import com.android.vts.entity.TestEntity;
import com.android.vts.entity.TestPlanEntity;
import com.android.vts.entity.TestPlanRunEntity;
import com.android.vts.entity.TestRunEntity;
import com.android.vts.entity.TestSuiteFileEntity;
import com.android.vts.entity.TestSuiteResultEntity;
import com.android.vts.entity.UserEntity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The @WebListener annotation for registering a class as a listener of a web application.
 */
@WebListener
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
    ObjectifyService.init();
    ObjectifyService.register(CoverageEntity.class);
    ObjectifyService.register(TestCoverageStatusEntity.class);
    ObjectifyService.register(TestEntity.class);
    ObjectifyService.register(TestPlanEntity.class);
    ObjectifyService.register(TestPlanRunEntity.class);
    ObjectifyService.register(TestRunEntity.class);
    ObjectifyService.register(TestSuiteFileEntity.class);
    ObjectifyService.register(TestSuiteResultEntity.class);
    ObjectifyService.register(RoleEntity.class);
    ObjectifyService.register(UserEntity.class);
    ObjectifyService.begin();
    logger.log(Level.INFO, "Value Initialized from context.");

    Properties systemConfigProp = new Properties();

    try {
      InputStream defaultInputStream =
          ObjectifyListener.class
              .getClassLoader()
              .getResourceAsStream("config.properties");

      systemConfigProp.load(defaultInputStream);

      String roleList = systemConfigProp.getProperty("user.roleList");
      Supplier<Stream<String>> streamSupplier = () -> Arrays.stream(roleList.split(","));
      this.createRoles(streamSupplier.get());

      String adminEmail = systemConfigProp.getProperty("user.adminEmail");
      if (adminEmail.isEmpty()) {
        logger.log(Level.WARNING, "Admin email is not properly set. Check config file");
      } else {
        String adminName = systemConfigProp.getProperty("user.adminName");
        String adminCompany = systemConfigProp.getProperty("user.adminCompany");
        Optional<String> roleName = streamSupplier.get().filter(r -> r.equals("admin")).findFirst();
        this.createAdminUser(adminEmail, adminName, adminCompany, roleName.orElse("admin"));
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Receives notification that the ServletContext is about to be shut down.
   */
  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    ServletContext servletContext = servletContextEvent.getServletContext();
    logger.log(Level.INFO, "Value deleted from context.");
  }

  private void createRoles(Stream<String> roleStream) {
    roleStream.map(role -> role.trim()).forEach(roleName -> {
      RoleEntity roleEntity = new RoleEntity(roleName);
      roleEntity.save();
    });
  }

  private void createAdminUser(String email, String name, String company, String role) {
    Optional<UserEntity> adminUserEntityOptional = Optional
        .ofNullable(UserEntity.getAdminUser(email));
    if (adminUserEntityOptional.isPresent()) {
      logger.log(Level.INFO, "The user is already registered.");
    } else {
      UserEntity userEntity = new UserEntity(email, name, company, role);
      userEntity.setIsAdmin(true);
      userEntity.save();
      logger.log(Level.INFO, "The user is saved successfully.");
    }
  }
}
