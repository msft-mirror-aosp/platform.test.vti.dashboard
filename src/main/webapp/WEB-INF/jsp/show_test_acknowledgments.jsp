<%--
  ~ Copyright (c) 2017 The Android Open Source Project
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
  <link rel='stylesheet' href='/css/test_acknowledgments.css'>
  <%@ include file='header.jsp' %>
  <script src='js/test_acknowledgments.js'></script>
  <script>
    $(document).ready(function() {
      var acks = $('#acknowledgments').testAcknowledgments(
        ${allTests}, ${branches}, ${devices}, ${testAcknowledgments}, ${readOnly});
    });
  </script>
  <body>
    <div class='container wide'>
      <div class='row' id='acknowledgments'></div>
    </div>
    <%@ include file='footer.jsp' %>
  </body>
</html>
