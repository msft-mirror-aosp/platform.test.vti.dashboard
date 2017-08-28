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

import com.android.vts.entity.TestEntity;
import com.android.vts.entity.TestStatusEntity;
import com.android.vts.entity.UserFavoriteEntity;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Represents the servlet that is invoked on loading the first page of dashboard. */
public class DashboardMainServlet extends BaseServlet {
    private static final String DASHBOARD_MAIN_JSP = "WEB-INF/jsp/dashboard_main.jsp";
    private static final String DASHBOARD_ALL_LINK = "/?showAll=true";
    private static final String DASHBOARD_FAVORITES_LINK = "/";
    private static final String ALL_HEADER = "All Tests";
    private static final String FAVORITES_HEADER = "Favorites";
    private static final String NO_TESTS_ERROR = "No test results available.";
    private static final String FAVORITES_BUTTON = "Show Favorites";
    private static final String ALL_BUTTON = "Show All";
    private static final String UP_ARROW = "keyboard_arrow_up";
    private static final String DOWN_ARROW = "keyboard_arrow_down";

    @Override
    public PageType getNavParentType() {
        return PageType.TOT;
    }

    @Override
    public List<Page> getBreadcrumbLinks(HttpServletRequest request) {
        return null;
    }

    /** Helper class for displaying test entries on the main dashboard. */
    public class TestDisplay implements Comparable<TestDisplay> {
        private final Key testKey;
        private final int passCount;
        private final int failCount;
        private boolean muteNotifications;

        /**
         * Test display constructor.
         *
         * @param testKey The key of the test.
         * @param passCount The number of tests passing.
         * @param failCount The number of tests failing.
         */
        public TestDisplay(Key testKey, int passCount, int failCount) {
            this.testKey = testKey;
            this.passCount = passCount;
            this.failCount = failCount;
            this.muteNotifications = false;
        }

        /**
         * Get the key of the test.
         *
         * @return The key of the test.
         */
        public String getName() {
            return this.testKey.getName();
        }

        /**
         * Get the number of passing test cases.
         *
         * @return The number of passing test cases.
         */
        public int getPassCount() {
            return this.passCount;
        }

        /**
         * Get the number of failing test cases.
         *
         * @return The number of failing test cases.
         */
        public int getFailCount() {
            return this.failCount;
        }

        /**
         * Get the notification mute status.
         *
         * @return True if the subscriber has muted notifications, false otherwise.
         */
        public boolean getMuteNotifications() {
            return this.muteNotifications;
        }

        /** Set the notification mute status. */
        public void setMuteNotifications(boolean muteNotifications) {
            this.muteNotifications = muteNotifications;
        }

        @Override
        public int compareTo(TestDisplay test) {
            return this.testKey.getName().compareTo(test.getName());
        }
    }

    @Override
    public void doGetHandler(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        User currentUser = userService.getCurrentUser();
        RequestDispatcher dispatcher = null;
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        HttpSession session = request.getSession(true);
        PageType referTo = PageType.TREE;
        if (session.getAttribute("treeDefault") != null) {
            boolean treeDefault = (boolean) session.getAttribute("treeDefault");
            if (!treeDefault) {
                referTo = PageType.TABLE;
            }
        }

        List<TestDisplay> displayedTests = new ArrayList<>();
        List<String> allTestNames = new ArrayList<>();
        List<Key> unprocessedTestKeys = new ArrayList<>();

        Map<Key, TestDisplay> testMap = new HashMap<>(); // map from table key to TestDisplay
        Map<String, String> subscriptionMap = new HashMap<>();

        boolean showAll = request.getParameter("showAll") != null;
        String header;
        String buttonLabel;
        String buttonIcon;
        String buttonLink;
        String error = null;

        Query query = new Query(TestEntity.KIND).setKeysOnly();
        for (Entity test : datastore.prepare(query).asIterable()) {
            allTestNames.add(test.getKey().getName());
            unprocessedTestKeys.add(test.getKey());
        }

        query =
                new Query(TestStatusEntity.KIND)
                        .addProjection(
                                new PropertyProjection(TestStatusEntity.PASS_COUNT, Long.class))
                        .addProjection(
                                new PropertyProjection(TestStatusEntity.FAIL_COUNT, Long.class));
        for (Entity status : datastore.prepare(query).asIterable()) {
            TestStatusEntity statusEntity = TestStatusEntity.fromEntity(status);
            if (statusEntity == null) continue;
            Key testKey = KeyFactory.createKey(TestEntity.KIND, statusEntity.testName);
            if (!unprocessedTestKeys.contains(testKey)) continue;
            TestDisplay display =
                    new TestDisplay(testKey, statusEntity.passCount, statusEntity.failCount);
            testMap.put(testKey, display);
            unprocessedTestKeys.remove(testKey);
        }

        // Process tests without statuses
        for (Key testKey : unprocessedTestKeys) {
            TestDisplay display = new TestDisplay(testKey, -1, -1);
            testMap.put(testKey, display);
        }

        if (testMap.size() == 0) {
            error = NO_TESTS_ERROR;
        }

        if (showAll) {
            for (Key testKey : testMap.keySet()) {
                displayedTests.add(testMap.get(testKey));
            }
            header = ALL_HEADER;
            buttonLabel = FAVORITES_BUTTON;
            buttonIcon = UP_ARROW;
            buttonLink = DASHBOARD_FAVORITES_LINK;
        } else {
            if (testMap.size() > 0) {
                Filter userFilter =
                        new FilterPredicate(
                                UserFavoriteEntity.USER, FilterOperator.EQUAL, currentUser);
                query = new Query(UserFavoriteEntity.KIND).setFilter(userFilter);

                for (Entity favoriteEntity : datastore.prepare(query).asIterable()) {
                    UserFavoriteEntity favorite = UserFavoriteEntity.fromEntity(favoriteEntity);
                    Key testKey = favorite.testKey;
                    if (!testMap.containsKey(testKey)) {
                        continue;
                    }
                    TestDisplay display = testMap.get(testKey);
                    display.setMuteNotifications(favorite.muteNotifications);
                    displayedTests.add(display);
                    subscriptionMap.put(
                            testKey.getName(), KeyFactory.keyToString(favoriteEntity.getKey()));
                }
            }
            header = FAVORITES_HEADER;
            buttonLabel = ALL_BUTTON;
            buttonIcon = DOWN_ARROW;
            buttonLink = DASHBOARD_ALL_LINK;
        }
        displayedTests.sort(Comparator.naturalOrder());

        response.setStatus(HttpServletResponse.SC_OK);
        request.setAttribute("allTestsJson", new Gson().toJson(allTestNames));
        request.setAttribute("subscriptionMapJson", new Gson().toJson(subscriptionMap));
        request.setAttribute("testNames", displayedTests);
        request.setAttribute("headerLabel", header);
        request.setAttribute("showAll", showAll);
        request.setAttribute("buttonLabel", buttonLabel);
        request.setAttribute("buttonIcon", buttonIcon);
        request.setAttribute("buttonLink", buttonLink);
        request.setAttribute("error", error);
        request.setAttribute("resultsUrl", referTo.defaultUrl);
        dispatcher = request.getRequestDispatcher(DASHBOARD_MAIN_JSP);
        try {
            dispatcher.forward(request, response);
        } catch (ServletException e) {
            logger.log(Level.SEVERE, "Servlet Excpetion caught : ", e);
        }
    }
}
