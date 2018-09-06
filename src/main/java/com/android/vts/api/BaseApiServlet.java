/*
 * Copyright (c) 2018 Google Inc. All Rights Reserved.
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

package com.android.vts.api;

import com.google.apphosting.api.ApiProxy;
import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An abstract class to be subclassed to create API Servlet
 */
public class BaseApiServlet extends HttpServlet {

  private static final Logger logger =
      Logger.getLogger(BaseApiServlet.class.getName());

  /**
   * System Configuration Property class
   */
  protected Properties systemConfigProp = new Properties();

  /**
   * Appengine server host name
   */
  protected String hostName;

  @Override
  public void init(ServletConfig cfg) throws ServletException {
    super.init(cfg);

    ApiProxy.Environment env = ApiProxy.getCurrentEnvironment();
    hostName = env.getAttributes().get("com.google.appengine.runtime.default_version_hostname")
        .toString();
    try {
      InputStream defaultInputStream =
          BaseApiServlet.class
              .getClassLoader()
              .getResourceAsStream("config.properties");
      systemConfigProp.load(defaultInputStream);

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected void setAccessControlHeaders(HttpServletResponse resp) {
    resp.setHeader("Access-Control-Allow-Origin", hostName);
    resp.setHeader("Access-Control-Allow-Methods", "GET, PUT, POST, OPTIONS, DELETE");
    resp.addHeader("Access-Control-Allow-Headers", "Content-Type");
    resp.addHeader("Access-Control-Max-Age", "86400");
  }
}
