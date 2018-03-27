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

<html>
  <%@ include file="header.jsp" %>
  <link rel='stylesheet' href='/css/show_plan_release.css'>
  <link rel='stylesheet' href='/css/plan_runs.css'>
  <link rel='stylesheet' href='/css/search_header.css'>
  <script src='https://www.gstatic.com/external_hosted/moment/min/moment-with-locales.min.js'></script>
  <script src='js/time.js'></script>
  <script src='js/plan_runs.js'></script>
  <script src='js/search_header.js'></script>
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
          <table class="bordered highlight">
            <thead>
              <tr>
                <th>Branch</th>
                <th>Target</th>
                <th>Build ID</th>
                <th>Suite Plan</th>
                <th>System Fingerprint</th>
                <th>Vendor Fingerprint</th>
              </tr>
            </thead>
            <tbody>
            <c:forEach var="testSuiteResultEntity" items="${testSuiteResultEntityList}">
              <tr>
                <td><c:out value="${testSuiteResultEntity.branch}"></c:out></td>
                <td><c:out value="${testSuiteResultEntity.target}"></c:out></td>
                <td><c:out value="${testSuiteResultEntity.buildId}"></c:out></td>
                <td><c:out value="${testSuiteResultEntity.suitePlan}"></c:out></td>
                <td><c:out value="${testSuiteResultEntity.buildSystemFingerprint}"></c:out></td>
                <td><c:out value="${testSuiteResultEntity.buildVendorFingerprint}"></c:out></td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
        </div>
      </div>

    </div>
    <%@ include file="footer.jsp" %>
  </body>
</html>
