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

package com.android.vts.servlet;

import com.android.vts.entity.TestCoverageStatusEntity;
import com.android.vts.entity.TestEntity;
import com.android.vts.entity.TestRunEntity;
import com.android.vts.entity.TestRunEntity.TestRunType;
import com.android.vts.proto.VtsReportMessage;
import com.android.vts.util.DatastoreHelper;
import com.android.vts.util.FilterUtil;
import com.android.vts.util.TestRunMetadata;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.visualization.datasource.DataSourceHelper;
import com.google.visualization.datasource.DataSourceRequest;
import com.google.visualization.datasource.base.DataSourceException;
import com.google.visualization.datasource.base.ReasonType;
import com.google.visualization.datasource.base.ResponseStatus;
import com.google.visualization.datasource.base.StatusType;
import com.google.visualization.datasource.base.TypeMismatchException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.TableRow;
import com.google.visualization.datasource.datatable.value.DateTimeValue;
import com.google.visualization.datasource.datatable.value.DateValue;
import com.google.visualization.datasource.datatable.value.NumberValue;
import com.google.visualization.datasource.datatable.value.ValueType;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Represents the servlet that is invoked on loading the coverage overview page.
 */
public class ShowCoverageOverviewServlet extends BaseServlet {

  @Override
  public PageType getNavParentType() {
    return PageType.COVERAGE_OVERVIEW;
  }

  @Override
  public List<Page> getBreadcrumbLinks(HttpServletRequest request) {
    return null;
  }

  @Override
  public void doGetHandler(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    String pageType =
        request.getParameter("pageType") == null ? "html" : request.getParameter("pageType");

    RequestDispatcher dispatcher;
    if (pageType.equalsIgnoreCase("html")) {
      dispatcher = this.getCoverageDispatcher(request, response);
      try {
        request.setAttribute("pageType", pageType);
        response.setStatus(HttpServletResponse.SC_OK);
        dispatcher.forward(request, response);
      } catch (ServletException e) {
        logger.log(Level.SEVERE, "Servlet Exception caught : ", e);
      }
    } else {

      String testName = request.getParameter("testName");

      DataTable data = getCoverageDataTable(testName);
      DataSourceRequest dsRequest = null;

      try {
        // Extract the datasource request parameters.
        dsRequest = new DataSourceRequest(request);

        // NOTE: If you want to work in restricted mode, which means that only
        // requests from the same domain can access the data source, uncomment the following call.
        //
        // DataSourceHelper.verifyAccessApproved(dsRequest);

        // Apply the query to the data table.
        DataTable newData = DataSourceHelper.applyQuery(dsRequest.getQuery(), data,
            dsRequest.getUserLocale());

        // Set the response.
        DataSourceHelper.setServletResponse(newData, dsRequest, response);
      } catch (RuntimeException rte) {
        logger.log(Level.SEVERE, "A runtime exception has occured", rte);
        ResponseStatus status = new ResponseStatus(StatusType.ERROR, ReasonType.INTERNAL_ERROR,
            rte.getMessage());
        if (dsRequest == null) {
          dsRequest = DataSourceRequest.getDefaultDataSourceRequest(request);
        }
        DataSourceHelper.setServletErrorResponse(status, dsRequest, response);
      } catch (DataSourceException e) {
        if (dsRequest != null) {
          DataSourceHelper.setServletErrorResponse(e, dsRequest, response);
        } else {
          DataSourceHelper.setServletErrorResponse(e, request, response);
        }
      }
    }
  }

  private RequestDispatcher getCoverageDispatcher(
      HttpServletRequest request, HttpServletResponse response) {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    String COVERAGE_OVERVIEW_JSP = "WEB-INF/jsp/show_coverage_overview.jsp";

    RequestDispatcher dispatcher = null;
    boolean unfiltered = request.getParameter("unfiltered") != null;
    boolean showPresubmit = request.getParameter("showPresubmit") != null;
    boolean showPostsubmit = request.getParameter("showPostsubmit") != null;

    // If no params are specified, set to default of postsubmit-only.
    if (!(showPresubmit || showPostsubmit)) {
      showPostsubmit = true;
    }

    // If unfiltered, set showPre- and Post-submit to true for accurate UI.
    if (unfiltered) {
      showPostsubmit = true;
      showPresubmit = true;
    }

    Map<String, TestCoverageStatusEntity> testCoverageStatusMap = TestCoverageStatusEntity
        .getTestCoverageStatusMap();

    List<Key> allTests = TestEntity.getAllTest().stream().map(t -> t.getOldKey())
        .collect(Collectors.toList());

    // Add test names to list
    List<String> resultNames = new ArrayList<>();
    for (VtsReportMessage.TestCaseResult r : VtsReportMessage.TestCaseResult.values()) {
      resultNames.add(r.name());
    }

    List<JsonObject> testRunObjects = new ArrayList<>();

    Query.Filter testFilter =
        new Query.FilterPredicate(
            TestRunEntity.HAS_COVERAGE, Query.FilterOperator.EQUAL, true);
    Query.Filter timeFilter =
        FilterUtil.getTestTypeFilter(showPresubmit, showPostsubmit, unfiltered);

    if (timeFilter != null) {
      testFilter = Query.CompositeFilterOperator.and(testFilter, timeFilter);
    }
    Map<String, String[]> parameterMap = request.getParameterMap();
    List<Query.Filter> userTestFilters = FilterUtil.getUserTestFilters(parameterMap);
    userTestFilters.add(0, testFilter);
    Query.Filter userDeviceFilter = FilterUtil.getUserDeviceFilter(parameterMap);

    int coveredLines = 0;
    int uncoveredLines = 0;
    int passCount = 0;
    int failCount = 0;
    for (Key key : allTests) {
      List<Key> gets =
          FilterUtil.getMatchingKeys(
              key,
              TestRunEntity.KIND,
              userTestFilters,
              userDeviceFilter,
              Query.SortDirection.DESCENDING,
              1);
      Map<Key, Entity> entityMap = datastore.get(gets);
      for (Key entityKey : gets) {
        if (!entityMap.containsKey(entityKey)) {
          continue;
        }
        TestRunEntity testRunEntity = TestRunEntity.fromEntity(entityMap.get(entityKey));
        if (testRunEntity == null) {
          continue;
        }

        // Overwrite the coverage value with newly update value from user decision
        TestCoverageStatusEntity testCoverageStatusEntity = testCoverageStatusMap
            .get(key.getName());
        testRunEntity.setCoveredLineCount(testCoverageStatusEntity.getUpdatedCoveredLineCount());
        testRunEntity.setTotalLineCount(testCoverageStatusEntity.getUpdatedTotalLineCount());
        TestRunMetadata metadata = new TestRunMetadata(key.getName(), testRunEntity);

        testRunObjects.add(metadata.toJson());
        coveredLines += testRunEntity.getCoveredLineCount();
        uncoveredLines += testRunEntity.getTotalLineCount() - testRunEntity.getCoveredLineCount();
        passCount += testRunEntity.getPassCount();
        failCount += testRunEntity.getFailCount();
      }
    }

    FilterUtil.setAttributes(request, parameterMap);

    int[] testStats = new int[VtsReportMessage.TestCaseResult.values().length];
    testStats[VtsReportMessage.TestCaseResult.TEST_CASE_RESULT_PASS.getNumber()] = passCount;
    testStats[VtsReportMessage.TestCaseResult.TEST_CASE_RESULT_FAIL.getNumber()] = failCount;

    response.setStatus(HttpServletResponse.SC_OK);
    request.setAttribute("resultNames", resultNames);
    request.setAttribute("resultNamesJson", new Gson().toJson(resultNames));
    request.setAttribute("testRuns", new Gson().toJson(testRunObjects));
    request.setAttribute("coveredLines", new Gson().toJson(coveredLines));
    request.setAttribute("uncoveredLines", new Gson().toJson(uncoveredLines));
    request.setAttribute("testStats", new Gson().toJson(testStats));

    request.setAttribute("unfiltered", unfiltered);
    request.setAttribute("showPresubmit", showPresubmit);
    request.setAttribute("showPostsubmit", showPostsubmit);
    request.setAttribute("branches", new Gson().toJson(DatastoreHelper.getAllBranches()));
    request.setAttribute("devices", new Gson().toJson(DatastoreHelper.getAllBuildFlavors()));
    dispatcher = request.getRequestDispatcher(COVERAGE_OVERVIEW_JSP);
    return dispatcher;
  }

  private DataTable getCoverageDataTable(String testName) {

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    DataTable dataTable = new DataTable();
    ArrayList<ColumnDescription> cd = new ArrayList<>();
    ColumnDescription startDate = new ColumnDescription("startDate", ValueType.DATETIME, "Date");
    startDate.setPattern("yyyy-MM-dd");
    cd.add(startDate);
    cd.add(new ColumnDescription("coveredLineCount", ValueType.NUMBER,
        "Covered Source Code Line Count"));
    cd.add(
        new ColumnDescription("totalLineCount", ValueType.NUMBER, "Total Source Code Line Count"));
    cd.add(new ColumnDescription("percentage", ValueType.NUMBER, "Coverage Ratio (%)"));

    dataTable.addColumns(cd);

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, -30);
    Long startTime = cal.getTime().getTime() * 1000;
    Long endTime = Calendar.getInstance().getTime().getTime() * 1000;

    com.google.cloud.datastore.Key startKey = datastore.newKeyFactory()
        .setKind(TestRunEntity.KIND)
        .addAncestors(PathElement.of(TestEntity.KIND, testName),
            PathElement.of(TestRunEntity.KIND, startTime))
        .newKey(startTime);

    com.google.cloud.datastore.Key endKey = datastore.newKeyFactory()
        .setKind(TestRunEntity.KIND)
        .addAncestors(PathElement.of(TestEntity.KIND, testName),
            PathElement.of(TestRunEntity.KIND, endTime))
        .newKey(endTime);

    Filter testRunFilter = CompositeFilter.and(
        PropertyFilter.lt("__key__", endKey),
        PropertyFilter.gt("__key__", startKey),
        PropertyFilter.eq("hasCoverage", true)
    );

    com.google.cloud.datastore.Query<com.google.cloud.datastore.Entity> testRunQuery =
        com.google.cloud.datastore.Query
        .newEntityQueryBuilder()
        .setKind(TestRunEntity.KIND)
        .setFilter(testRunFilter)
        .build();

    List<TestRunEntity> testRunEntityList = new ArrayList<>();
    QueryResults<com.google.cloud.datastore.Entity> testRunIterator = datastore.run(testRunQuery);
    while (testRunIterator.hasNext()) {
      com.google.cloud.datastore.Entity entity = testRunIterator.next();

      Key parentKey = KeyFactory.createKey(TestEntity.KIND, entity.getString("testName"));

      List<LongValue> testCaseIdList = entity.getList("testCaseIds");
      List<StringValue> linkList = new ArrayList<>();
      if (entity.contains("logLinks")) {
        linkList = entity.getList("logLinks");
      }
      TestRunEntity testRunEntity = new TestRunEntity(
          KeyFactory.createKey(parentKey, TestRunEntity.KIND, entity.getLong("startTimestamp")),
          TestRunType.fromNumber(Long.valueOf(entity.getLong("type")).intValue()),
          entity.getLong("startTimestamp"),
          entity.getLong("endTimestamp"),
          entity.getString("testBuildId"),
          entity.getString("hostName"),
          entity.getLong("passCount"),
          entity.getLong("failCount"),
          testCaseIdList.stream().map(value -> value.get()).collect(Collectors.toList()),
          linkList.stream().map(v -> v.get()).collect(Collectors.toList()),
          entity.getLong("coveredLineCount"),
          entity.getLong("totalLineCount")
      );
      testRunEntityList.add(testRunEntity);
    }

    DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    Map<String, List<TestRunEntity>> testRunEntityListMap = testRunEntityList.stream().collect(
        Collectors.groupingBy(v -> dateTimeFormatter.print(v.getStartTimestamp() / 1000))
    );

    testRunEntityListMap.forEach((key, entityList) -> {
      if (dataTable.getRows().size() < 10) {
        GregorianCalendar gCal = new GregorianCalendar();
        gCal.setTimeZone(TimeZone.getTimeZone("GMT"));
        gCal.setTimeInMillis(entityList.get(0).getStartTimestamp() / 1000);

        Long sumCoveredLine = entityList.stream().mapToLong(val -> val.getCoveredLineCount()).sum();
        Long sumTotalLine = entityList.stream().mapToLong(val -> val.getTotalLineCount()).sum();
        BigDecimal coveredLineNum = new BigDecimal(sumCoveredLine);
        BigDecimal totalLineNum = new BigDecimal(sumTotalLine);
        BigDecimal totalPercent = new BigDecimal(100);
        float percentage = coveredLineNum.multiply(totalPercent).divide(totalLineNum, 2,
            RoundingMode.HALF_DOWN).floatValue();

        TableRow tableRow = new TableRow();
        tableRow.addCell(new DateTimeValue(gCal));
        tableRow.addCell(new NumberValue(sumCoveredLine));
        tableRow.addCell(new NumberValue(sumTotalLine));
        tableRow.addCell(new NumberValue(percentage));
        try {
          dataTable.addRow(tableRow);
        } catch (TypeMismatchException e) {
          logger.log(Level.WARNING, "Invalid type! ");
        }
      }
    });

    return dataTable;
  }
}
