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

package com.android.vts.job;

import com.android.vts.servlet.BaseServlet;
import com.android.vts.util.EmailHelper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * An abstract class to be subclassed to create Job Servlet
 */
public abstract class BaseJobServlet extends HttpServlet {

    /**
     * System Configuration Property class
     */
    protected static Properties systemConfigProp = new Properties();

    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);

        try {
            InputStream defaultInputStream =
                    BaseServlet.class.getClassLoader().getResourceAsStream("config.properties");
            systemConfigProp.load(defaultInputStream);

            EmailHelper.setPropertyValues(systemConfigProp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
