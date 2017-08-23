/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.vts.servlet;

import com.android.vts.entity.ProfilingPointRunEntity;
import com.android.vts.entity.TestEntity;
import com.android.vts.entity.TestRunEntity;
import com.android.vts.proto.VtsReportMessage;
import com.android.vts.util.BoxPlot;
import com.android.vts.util.DatastoreHelper;
import com.android.vts.util.FilterUtil;
import com.android.vts.util.GraphSerializer;
import com.android.vts.util.PerformanceUtil;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet for handling requests to load graphs. */
public class ShowProfilingOverviewServlet extends BaseServlet {
    private static final String PROFILING_OVERVIEW_JSP = "WEB-INF/jsp/show_profiling_overview.jsp";

    private static final String HIDL_HAL_OPTION = "hidl_hal_mode";
    private static final String[] splitKeysArray = new String[] {HIDL_HAL_OPTION};
    private static final Set<String> splitKeySet = new HashSet<>(Arrays.asList(splitKeysArray));

    @Override
    public PageType getNavParentType() {
        return PageType.PROFILING_LIST;
    }

    @Override
    public List<Page> getBreadcrumbLinks(HttpServletRequest request) {
        List<Page> links = new ArrayList<>();
        String testName = request.getParameter("testName");
        links.add(new Page(PageType.PROFILING_OVERVIEW, testName, "?testName=" + testName));
        return links;
    }

    @Override
    public void doGetHandler(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        RequestDispatcher dispatcher = null;
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        String testName = request.getParameter("testName");
        long endTime = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());
        long startTime = endTime - TimeUnit.DAYS.toMicros(14);

        // Create a query for test runs matching the time window filter
        Key parentKey = KeyFactory.createKey(TestEntity.KIND, testName);
        Filter profilingFilter =
                FilterUtil.getProfilingTimeFilter(
                        parentKey, TestRunEntity.KIND, startTime, endTime);
        Query profilingQuery =
                new Query(ProfilingPointRunEntity.KIND)
                        .setAncestor(parentKey)
                        .setFilter(profilingFilter);
        Map<String, BoxPlot> plotMap = new HashMap<>();
        for (Entity e :
                datastore
                        .prepare(profilingQuery)
                        .asIterable(DatastoreHelper.getLargeBatchOptions())) {
            ProfilingPointRunEntity pt = ProfilingPointRunEntity.fromEntity(e);
            if (pt == null
                    || pt.regressionMode
                            == VtsReportMessage.VtsProfilingRegressionMode
                                    .VTS_REGRESSION_MODE_DISABLED) continue;
            String option = PerformanceUtil.getOptionAlias(pt, splitKeySet);

            if (!plotMap.containsKey(pt.name)) {
                plotMap.put(pt.name, new BoxPlot(pt.name));
            }

            BoxPlot plot = plotMap.get(pt.name);
            long days = (endTime - e.getParent().getId()) / TimeUnit.DAYS.toMicros(1);
            long time = endTime - days * TimeUnit.DAYS.toMicros(1);

            plot.addSeriesData(Long.toString(time), option, pt);
        }

        List<BoxPlot> plots = new ArrayList<>();
        for (String key : plotMap.keySet()) {
            BoxPlot plot = plotMap.get(key);
            if (plot.size() == 0) continue;
            plots.add(plot);
        }
        Collections.sort(
                plots,
                new Comparator<BoxPlot>() {
                    @Override
                    public int compare(BoxPlot b1, BoxPlot b2) {
                        return b1.getName().compareTo(b2.getName());
                    }
                });

        Gson gson =
                new GsonBuilder()
                        .registerTypeHierarchyAdapter(BoxPlot.class, new GraphSerializer())
                        .create();
        request.setAttribute("plots", gson.toJson(plots));
        request.setAttribute("testName", request.getParameter("testName"));
        dispatcher = request.getRequestDispatcher(PROFILING_OVERVIEW_JSP);
        try {
            dispatcher.forward(request, response);
        } catch (ServletException e) {
            logger.log(Level.SEVERE, "Servlet Exception caught : ", e);
        }
    }
}
