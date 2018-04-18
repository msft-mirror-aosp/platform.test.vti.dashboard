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
<c:set var="timeZone" value="America/Los_Angeles"/>

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

            <ul class="collapsible popout test-runs">
                <c:forEach var="testSuiteResultEntity" items="${testSuiteResultEntityPagination.list}">
                    <li class="test-run-container">
                    <div class="collapsible-header test-run">
                        <div class="row" style="margin-bottom: 0px; line-height: 30px;">
                            <div class="col s9">
                                <b><c:out value="${testSuiteResultEntity.branch}"></c:out>/<c:out value="${testSuiteResultEntity.target}"></c:out> (<c:out value="${testSuiteResultEntity.buildId}"></c:out>)</b>
                            </div>
                            <div class="col s3">
                                <span class="indicator right center green">
                                    <c:out value="${testSuiteResultEntity.passedTestCaseCount}"></c:out>/<c:out value="${testSuiteResultEntity.passedTestCaseCount + testSuiteResultEntity.failedTestCaseCount}"></c:out>
                                </span>
                            </div>
                            <div class="col s5">
                                <span class="suite-test-run-metadata">
                                    <b>Suite Build Number: </b><c:out value="${testSuiteResultEntity.suiteBuildNumber}"></c:out><br>
                                    <b>VTS Build: </b><c:out value="${testSuiteResultEntity.buildId}"></c:out><br>
                                    <b>Modules: </b><c:out value="${testSuiteResultEntity.modulesDone}"></c:out>/<c:out value="${testSuiteResultEntity.modulesTotal}"></c:out><br>
                                </span>
                            </div>
                            <div class="col s7">
                                <span class="suite-test-run-metadata">
                                    <b>Host: </b><c:out value="${testSuiteResultEntity.hostName}"></c:out><br>
                                    <b>LOG Path: </b>
                                        <c:set var="logPath" value="${fn:replace(testSuiteResultEntity.resultPath, 'gs://vts-report/', '')}"/>
                                        <a href="show_gcs_log?path=${logPath}">
                                            <c:out value="${logPath}"></c:out>
                                        </a>
                                    <br>
                                </span>
                            </div>
                            <div class="col s10">
                                <span style="font-size: 13px;">
                                <jsp:setProperty name="startDateObject" property="time" value="${testSuiteResultEntity.startTime}"/>
                                <jsp:setProperty name="endDateObject" property="time" value="${testSuiteResultEntity.endTime}"/>
                                <fmt:formatDate value="${startDateObject}" pattern="yyyy-MM-dd HH:mm:ss" timeZone="${timeZone}" /> - <fmt:formatDate value="${endDateObject}" pattern="yyyy-MM-dd HH:mm:ss z" timeZone="${timeZone}" />
                                <c:set var="executionTime" scope="page" value="${(testSuiteResultEntity.endTime - testSuiteResultEntity.startTime) / 1000}"/>
                                (<c:out value="${executionTime}"></c:out>s)
                                </span>
                            </div>
                            <div class="col s2">
                                <i class="material-icons expand-arrow">expand_more</i>
                            </div>
                        </div>
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

      <div class="row">
        <div class="col s12 center-align">
          <ul class="pagination">
            <c:choose>
                <c:when test="${testSuiteResultEntityPagination.minPageRange gt testSuiteResultEntityPagination.pageSize}">
                    <li class="waves-effect">
                        <a href="${requestScope['javax.servlet.forward.servlet_path']}?plan=${plan}&type=${testType}&page=${testSuiteResultEntityPagination.minPageRange - 1}&nextPageToken=${testSuiteResultEntityPagination.previousPageCountToken}">
                            <i class="material-icons">chevron_left</i>
                        </a>
                    </li>
                </c:when>
                <c:otherwise>

                </c:otherwise>
            </c:choose>
            <c:forEach var="pageLoop" begin="${testSuiteResultEntityPagination.minPageRange}" end="${testSuiteResultEntityPagination.maxPageRange}">
              <li class="waves-effect">
                  <a href="${requestScope['javax.servlet.forward.servlet_path']}?plan=${plan}&type=${testType}&page=${pageLoop}<c:if test="${testSuiteResultEntityPagination.currentPageCountToken ne ''}">&nextPageToken=${testSuiteResultEntityPagination.currentPageCountToken}</c:if>">
                      <c:out value="${pageLoop}" />
                  </a>
              </li>
            </c:forEach>
            <c:choose>
                <c:when test="${testSuiteResultEntityPagination.maxPages gt testSuiteResultEntityPagination.pageSize}">
                    <li class="waves-effect">
                        <a href="${requestScope['javax.servlet.forward.servlet_path']}?plan=${plan}&type=${testType}&page=${testSuiteResultEntityPagination.maxPageRange + 1}&nextPageToken=${testSuiteResultEntityPagination.nextPageCountToken}">
                            <i class="material-icons">chevron_right</i>
                        </a>
                    </li>
                </c:when>
                <c:otherwise>

                </c:otherwise>
            </c:choose>
          </ul>
        </div>
      </div>

    </div>
    <%@ include file='footer.jsp' %>
  </body>
</html>
