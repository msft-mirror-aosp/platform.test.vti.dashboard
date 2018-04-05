<%--
  ~ Copyright (c) 2018 Google Inc. All Rights Reserved.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License"); you
  ~ may not use this file except in compliance with the License. You may
  ~ obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  ~ implied. See the License for the specific language governing
  ~ permissions and limitations under the License.
  --%>
<%@ page contentType='text/html;charset=UTF-8' language='java' %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib prefix='c' uri='http://java.sun.com/jsp/jstl/core'%>
<%@ taglib prefix='fmt' uri='http://java.sun.com/jsp/jstl/fmt'%>
<jsp:useBean id="startDateObject" class="java.util.Date"/>
<jsp:useBean id="endDateObject" class="java.util.Date"/>

<html>
  <%@ include file='header.jsp' %>
  <link type='text/css' href='/css/show_test_runs_common.css' rel='stylesheet'>
  <link type='text/css' href='/css/test_results.css' rel='stylesheet'>
  <script type='text/javascript'>
      $(document).ready(function() {

      });
  </script>
  <body>
    <div class='wide container'>

      <div class="row">
        <div class="col s12">
          <h4 id="test-suite-section-header">Test Suites</h4>
        </div>
      </div>

      <div class='row' id='test-suite-green-release-container'>
        <div class="col s12">

            <ul data-collapsible="expandable" class="collapsible popout test-runs">
                <c:forEach var="testSuiteResultEntity" items="${testSuiteResultEntityList}">
                    <li class="test-run-container">
                    <div test="SampleShellTest" time="1522488372555217" class="collapsible-header test-run">
                        <span class="test-run-metadata">
                            <b><c:out value="${testSuiteResultEntity.branch}"></c:out>/<c:out value="${testSuiteResultEntity.target}"></c:out> (<c:out value="${testSuiteResultEntity.buildId}"></c:out>)</b><br>
                            <b>Suite Build Number: </b><c:out value="${testSuiteResultEntity.suiteBuildNumber}"></c:out><br>
                            <b>VTS Build: </b><c:out value="${testSuiteResultEntity.buildId}"></c:out><br>
                            <b>Modules: </b><c:out value="${testSuiteResultEntity.modulesDone}"></c:out>/<c:out value="${testSuiteResultEntity.modulesTotal}"></c:out><br>
                            <jsp:setProperty name="startDateObject" property="time" value="${testSuiteResultEntity.startTime}"/>
                            <jsp:setProperty name="endDateObject" property="time" value="${testSuiteResultEntity.endTime}"/>
                            <fmt:formatDate value="${startDateObject}" pattern="yyyy-MM-dd HH:mm:ss" /> - <fmt:formatDate value="${endDateObject}" pattern="yyyy-MM-dd HH:mm:ss z" />
                            <c:set var="executionTime" scope="page" value="${(testSuiteResultEntity.endTime - testSuiteResultEntity.startTime) / 1000}"/>
                            (<c:out value="${executionTime}"></c:out>s)
                        </span>
                        <span class="indicator right center green">
                            <c:out value="${testSuiteResultEntity.passedTestCaseRatio}"></c:out>/<c:out value="${testSuiteResultEntity.passedTestCaseRatio + testSuiteResultEntity.failedTestCaseCount}"></c:out>
                        </span>
                        <i class="material-icons expand-arrow">expand_more</i>
                    </div>
                        <div class="collapsible-body test-results row" style="display: none;">
                            <div class="col test-col grey lighten-5 s12 left-most right-most">
                                <h5 class="test-result-label white" style="text-transform: capitalize;">
                                    Vendor Fingerprint
                                </h5>
                                <div class="test-case-container">
                                    <c:out value="${testSuiteResultEntity.buildVendorFingerprint}"></c:out>
                                </div>
                            </div>
                            <div class="col test-col grey lighten-5 s12 left-most right-most">
                                <h5 class="test-result-label white" style="text-transform: capitalize;">
                                    System Fingerprint
                                </h5>
                                <div class="test-case-container">
                                    <c:out value="${testSuiteResultEntity.buildSystemFingerprint}"></c:out>
                                </div>
                            </div>
                        </div>
                    </li>
                </c:forEach>
            </ul>

        </div>
      </div>

    </div>
    <%@ include file='footer.jsp' %>
  </body>
</html>
