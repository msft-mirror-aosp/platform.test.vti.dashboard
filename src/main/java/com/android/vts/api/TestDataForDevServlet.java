/*
 * Copyright (c) 2017 Google Inc. All Rights Reserved.
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

import com.android.vts.entity.BranchEntity;
import com.android.vts.entity.BuildTargetEntity;
import com.android.vts.entity.CoverageEntity;
import com.android.vts.entity.DeviceInfoEntity;
import com.android.vts.entity.ProfilingPointRunEntity;
import com.android.vts.entity.TestCaseRunEntity;
import com.android.vts.entity.TestEntity;
import com.android.vts.entity.TestRunEntity;
import com.android.vts.entity.TestRunEntity.TestRunType;
import com.android.vts.entity.TestStatusEntity;
import com.android.vts.entity.TestStatusEntity.TestCaseReference;
import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreTimeoutException;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet for handling requests to add mock data in datastore. */
public class TestDataForDevServlet extends HttpServlet {
    protected static final Logger logger =
            Logger.getLogger(TestDataForDevServlet.class.getName());

    /** datastore instance to save the test data into datastore through datastore library. */
    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    /** Gson is a Java library that can be used to convert Java Objects into their JSON representation.
     * It can also be used to convert a JSON string to an equivalent Java object.
     */
    private Gson gson = new GsonBuilder().create();

    /** TestReportData class for mapping test-report-data.json.
     *  This internal class's each fields will be automatically mapped
     *  to test-report-data.json file through Gson
     */
    private class TestReportDataObject {
        private List<Test> testList;

        private class Test {
            private List<TestRun> testRunList;

            private class TestRun {
                private String testName;
                private int type;
                private long startTimestamp;
                private long endTimestamp;
                private String testBuildId;
                private String hostName;
                private long passCount;
                private long failCount;
                private boolean hasCoverage;
                private long coveredLineCount;
                private long totalLineCount;
                private List<Long> testCaseIds;
                private List<Long> failingTestcaseIds;
                private List<Integer> failingTestcaseOffsets;
                private List<String> links;

                private List<Coverage> coverageList;
                private List<Profiling> profilingList;
                private List<TestCaseRun> testCaseRunList;
                private List<DeviceInfo> deviceInfoList;
                private List<BuildTarget> buildTargetList;
                private List<Branch> branchList;


                private class Coverage {
                    private String group;
                    private long coveredLineCount;
                    private long totalLineCount;
                    private String filePath;
                    private String projectName;
                    private String projectVersion;
                    private List<Long> lineCoverage;
                }

                private class Profiling {
                    private String name;
                    private int type;
                    private int regressionMode;
                    private List<String> labels;
                    private List<Long> values;
                    private String xLabel;
                    private String yLabel;
                    private List<String> options;
                }

                private class TestCaseRun {
                    private List<String> testCaseNames;
                    private List<Integer> results;
                }

                private class DeviceInfo {
                    private String branch;
                    private String product;
                    private String buildFlavor;
                    private String buildId;
                    private String abiBitness;
                    private String abiName;
                }

                private class BuildTarget {
                    private String targetName;
                }

                private class Branch {
                    private String branchName;
                }

            }
        }

        @Override
        public String toString() {
            return "(" + testList + ")";
        }
    }

    private class TestPlanReportDataObject {
        private List<BranchEntity> branchEntities;
        private List<BuildTargetEntity> buildTargetEntities;
        private List<CoverageEntity> coverageEntities;

        @Override
        public String toString() {
            return "(" + branchEntities + ")";
        }
    }

    /** Add mock data to local dev datastore. */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return;
        }

        UserService userService = UserServiceFactory.getUserService();
        User currentUser = userService.getCurrentUser();

        String pathInfo = request.getPathInfo();
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length > 1) {
            // Read the json output
            Reader postJsonReader = new InputStreamReader(request.getInputStream());
            Gson gson = new GsonBuilder().create();

            String testType = pathParts[1];
            if (testType.equalsIgnoreCase("report")) {
                TestReportDataObject trdObj =
                    gson.fromJson(postJsonReader, TestReportDataObject.class);
                logger.log(Level.INFO, "trdObj => " + trdObj);
                trdObj.testList.forEach(test -> {
                    test.testRunList.forEach(testRun -> {
                        TestEntity testEntity = new TestEntity(testRun.testName);

                        Key testRunKey = KeyFactory.createKey(testEntity.key, TestRunEntity.KIND,
                            testRun.startTimestamp);

                        List<TestCaseReference> failingTestCases = new ArrayList<>();
                        for (int idx = 0; idx < testRun.failingTestcaseIds.size(); idx++){
                            failingTestCases.add(
                                new TestCaseReference(testRun.failingTestcaseIds.get(idx),
                                    testRun.failingTestcaseOffsets.get(idx))
                            );
                        }

                        TestStatusEntity testStatusEntity = new TestStatusEntity(testRun.testName,
                            testRun.startTimestamp, (int) testRun.passCount, failingTestCases.size(),
                            failingTestCases);
                        datastore.put(testStatusEntity.toEntity());

                        testRun.coverageList.forEach(testRunCoverage -> {
                            CoverageEntity coverageEntity = new CoverageEntity(testRunKey,
                                testRunCoverage.group, testRunCoverage.coveredLineCount,
                                testRunCoverage.totalLineCount, testRunCoverage.filePath,
                                testRunCoverage.projectName, testRunCoverage.projectVersion,
                                testRunCoverage.lineCoverage);
                            datastore.put(coverageEntity.toEntity());
                        });

                        testRun.profilingList.forEach(testRunProfile -> {
                            ProfilingPointRunEntity profilingEntity = new ProfilingPointRunEntity(
                                testRunKey, testRunProfile.name, testRunProfile.type,
                                testRunProfile.regressionMode, testRunProfile.labels,
                                testRunProfile.values, testRunProfile.xLabel, testRunProfile.yLabel,
                                testRunProfile.options);
                            datastore.put(profilingEntity.toEntity());
                        });

                        TestCaseRunEntity testCaseEntity = new TestCaseRunEntity();
                        testRun.testCaseRunList.forEach(testCaseRun -> {
                            for (int idx = 0; idx < testCaseRun.testCaseNames.size(); idx++){
                                testCaseEntity.addTestCase(testCaseRun.testCaseNames.get(idx),
                                    testCaseRun.results.get(idx));
                            }
                        });
                        datastore.put(testCaseEntity.toEntity());

                        testRun.deviceInfoList.forEach(deviceInfo -> {
                            DeviceInfoEntity deviceInfoEntity = new DeviceInfoEntity(
                                testRunKey, deviceInfo.branch, deviceInfo.product,
                                deviceInfo.buildFlavor, deviceInfo.buildId, deviceInfo.abiBitness,
                                deviceInfo.abiName);;
                            datastore.put(deviceInfoEntity.toEntity());
                        });

                        testRun.buildTargetList.forEach(buildTarget -> {
                            BuildTargetEntity buildTargetEntity =
                                new BuildTargetEntity(buildTarget.targetName);
                            datastore.put(buildTargetEntity.toEntity());
                        });

                        testRun.branchList.forEach(branch -> {
                            BranchEntity branchEntity = new BranchEntity(branch.branchName);
                            datastore.put(branchEntity.toEntity());
                        });

                        TestRunEntity testRunEntity = new TestRunEntity(testEntity.key,
                            TestRunType.fromNumber(testRun.type), testRun.startTimestamp,
                            testRun.endTimestamp, testRun.testBuildId, testRun.hostName,
                            testRun.passCount, testRun.failCount, testRun.testCaseIds,
                            testRun.links, testRun.coveredLineCount, testRun.totalLineCount);
                        datastore.put(testRunEntity.toEntity());

                        Entity newTestEntity = testEntity.toEntity();

                        Transaction txn = datastore.beginTransaction();
                        try {
                            // Check if test already exists in the datastore
                            try {
                                Entity oldTest = datastore.get(testEntity.key);
                                TestEntity oldTestEntity = TestEntity.fromEntity(oldTest);
                                if (oldTestEntity == null || !oldTestEntity.equals(testEntity)) {
                                    datastore.put(newTestEntity);
                                }
                            } catch (EntityNotFoundException e) {
                                datastore.put(newTestEntity);
                            }
                            txn.commit();

                        } catch (ConcurrentModificationException | DatastoreFailureException
                            | DatastoreTimeoutException e) {
                            logger.log(Level.WARNING,
                                "Retrying test run insert: " + newTestEntity.getKey());
                        } finally {
                            if (txn.isActive()) {
                                logger.log(
                                    Level.WARNING,
                                    "Transaction rollback forced for run: " + testRunEntity.key);
                                txn.rollback();
                            }
                        }
                    });
                });
            } else {
                TestPlanReportDataObject tprdObj =
                    gson.fromJson(postJsonReader, TestPlanReportDataObject.class);
            }
        } else {
            logger.log(Level.WARNING, "URL path parameter is omitted!");
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
}
