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

import com.android.vts.entity.ApiCoverageExcludedEntity;
import com.android.vts.job.VtsSpreadSheetSyncServlet;
import com.android.vts.util.ObjectifyTestBase;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class VtsSpreadSheetSyncServletTest extends ObjectifyTestBase {

    private Gson gson;

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock ServletConfig servletConfig;

    /** It be executed before each @Test method */
    @BeforeEach
    void setUpExtra() {
        gson = new Gson();
    }

    @Test
    public void testSyncServletJob() throws IOException, ServletException {

        factory().register(ApiCoverageExcludedEntity.class);

        when(request.getPathInfo()).thenReturn("/cron/vts_spreadsheet_sync_job");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(response.getWriter()).thenReturn(pw);

        VtsSpreadSheetSyncServlet vtsSpreadSheetSyncServlet = new VtsSpreadSheetSyncServlet();
        vtsSpreadSheetSyncServlet.init(servletConfig);
        vtsSpreadSheetSyncServlet.doGet(request, response);
        String result = sw.getBuffer().toString().trim();

        List<ApiCoverageExcludedEntity> apiCoverageExcludedEntityList =
                ofy().load().type(ApiCoverageExcludedEntity.class).list();

        assertEquals(apiCoverageExcludedEntityList.size(), 2);
        assertEquals(apiCoverageExcludedEntityList.get(0).getApiName(), "getMasterMuteTest");
        assertEquals(
                apiCoverageExcludedEntityList.get(0).getPackageName(),
                "android.hardware.audio.test");
        assertEquals(apiCoverageExcludedEntityList.get(1).getApiName(), "getMasterVolumeTest");
        assertEquals(
                apiCoverageExcludedEntityList.get(1).getPackageName(),
                "android.hardware.video.test");
    }
}
