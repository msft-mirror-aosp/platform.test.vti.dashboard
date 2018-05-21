/*
 * Copyright (c) 2016 Google Inc. All Rights Reserved.
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

import com.android.vts.entity.TestPlanEntity;
import com.android.vts.entity.TestSuiteResultEntity;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserServiceFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.googlecode.objectify.ObjectifyService.ofy;

/** Represents the servlet that is invoked on loading the release page. */
public class ShowReleaseServlet extends BaseServlet {

    @Override
    public PageType getNavParentType() {
        return PageType.RELEASE;
    }

    @Override
    public List<Page> getBreadcrumbLinks(HttpServletRequest request) {
        return null;
    }

    @Override
    public void doGetHandler(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String testType =
                request.getParameter("type") == null ? "plan" : request.getParameter("type");

        RequestDispatcher dispatcher;
        if (testType.equalsIgnoreCase("plan")) {
            dispatcher = this.getTestPlanDispatcher(request, response);
        } else {
            dispatcher = this.getTestSuiteDispatcher(request, response);
        }

        try {
            request.setAttribute("testType", testType);
            response.setStatus(HttpServletResponse.SC_OK);
            dispatcher.forward(request, response);
        } catch (ServletException e) {
            logger.log(Level.SEVERE, "Servlet Excpetion caught : ", e);
        }
    }

    private RequestDispatcher getTestPlanDispatcher(
            HttpServletRequest request, HttpServletResponse response) {
        String RELEASE_JSP = "WEB-INF/jsp/show_release.jsp";
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Set<String> planSet = new HashSet<>();

        Query q = new Query(TestPlanEntity.KIND).setKeysOnly();
        for (Entity testPlanEntity : datastore.prepare(q).asIterable()) {
            planSet.add(testPlanEntity.getKey().getName());
        }

        List<String> plans = new ArrayList<>(planSet);
        plans.sort(Comparator.naturalOrder());

        request.setAttribute("isAdmin", UserServiceFactory.getUserService().isUserAdmin());
        request.setAttribute("planNames", plans);
        RequestDispatcher dispatcher = request.getRequestDispatcher(RELEASE_JSP);
        return dispatcher;
    }

    private RequestDispatcher getTestSuiteDispatcher(
            HttpServletRequest request, HttpServletResponse response) {
        String RELEASE_JSP = "WEB-INF/jsp/show_release.jsp";

        List<TestSuiteResultEntity> suiteResultEntityList =
                ofy().load()
                        .type(TestSuiteResultEntity.class)
                        .project("suitePlan")
                        .distinct(true)
                        .list();

        List<String> plans =
                suiteResultEntityList
                        .stream()
                        .map(suiteEntity -> suiteEntity.getSuitePlan())
                        .collect(Collectors.toList());
        request.setAttribute("isAdmin", UserServiceFactory.getUserService().isUserAdmin());
        request.setAttribute("planNames", plans);
        RequestDispatcher dispatcher = request.getRequestDispatcher(RELEASE_JSP);
        return dispatcher;
    }
}
