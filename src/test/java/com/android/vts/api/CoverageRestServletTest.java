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

package com.android.vts.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static com.googlecode.objectify.ObjectifyService.factory;

import com.android.vts.entity.ApiCoverageEntity;
import com.android.vts.util.ObjectifyTestBase;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Arrays;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CoverageRestServletTest extends ObjectifyTestBase {

    private final LocalServiceTestHelper helper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    protected Closeable session;

    private Gson gson;

    @Mock HttpServletRequest request;

    @Mock HttpServletResponse response;

    @BeforeClass
    public static void setUpBeforeClass() {}

    @Before
    public void setUp() throws Exception {
        InputStream configIs =
                this.getClass().getClassLoader().getResourceAsStream("config.properties");

        Properties prop = new Properties();
        prop.load(configIs);

        gson = new Gson();

        MockitoAnnotations.initMocks(this);

        this.helper.setUp();

        Datastore datastore =
                DatastoreOptions.newBuilder()
                        .setProjectId(prop.getProperty("testProjectID"))
                        .build()
                        .getService();

        ObjectifyService.init(new ObjectifyFactory(datastore));
        ObjectifyService.register(ApiCoverageEntity.class);
        ObjectifyService.begin();

        this.session = ObjectifyService.begin();
    }

    @After
    public void tearDown() {
        this.session.close();
        this.helper.tearDown();
    }

    @Test
    public void testApiData() throws IOException, ServletException {

        factory().register(ApiCoverageEntity.class);

        when(request.getPathInfo()).thenReturn("/api/data");

        String key =
                "partition_id+%7B%0A++project_id%3A+%22android-vts-staging%22%0A%7D%0Apath+%7B%0A"
                        + "++kind%3A+%22Test%22%0A++name%3A+%22VtsHalGraphicsMapperV2_0TargetProfiling"
                        + "%22%0A%7D%0Apath+%7B%0A++kind%3A+%22TestRun%22%0A++id%3A+1534552828906226%0A%7D%0A"
                        + "path+%7B%0A++kind%3A+%22ApiCoverage%22%0A"
                        + "++name%3A+%221f529291-f1c3-4d9b-ba60-48525fe7c376%22%0A%7D%0A";
        when(request.getParameter("key")).thenReturn(key);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(response.getWriter()).thenReturn(pw);

        CoverageRestServlet coverageRestServlet = new CoverageRestServlet();
        coverageRestServlet.doGet(request, response);
        String result = sw.getBuffer().toString().trim();

        LinkedTreeMap resultMap = gson.fromJson(result, LinkedTreeMap.class);

        assertEquals(resultMap.get("id"), "1f529291-f1c3-4d9b-ba60-48525fe7c376");
        assertEquals(resultMap.get("halInterfaceName"), "IAllocator");
        assertEquals(resultMap.get("halPackageName"), "android.hardware.graphics.allocator");
        assertEquals(resultMap.get("halApi"), Arrays.asList("allocate", "dumpDebugInfo"));
        assertEquals(resultMap.get("coveredHalApi"), Arrays.asList("allocate", "dumpDebugInfo"));

    }
}
