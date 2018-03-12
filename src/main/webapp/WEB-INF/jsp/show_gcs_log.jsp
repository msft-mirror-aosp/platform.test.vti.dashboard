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
        var paraMap = {'path': '', 'entry': ''};
        $('.modal').modal({
          dismissible: true,  // Modal can be dismissed by clicking outside of the modal
          opacity: .99,  // Opacity of modal background
          inDuration: 300,  // Transition in duration
          outDuration: 200,  // Transition out duration
          startingTop: '4%',  // Starting top style attribute
          endingTop: '10%',  // Ending top style attribute
          ready: function(modal, trigger) {  // Callback for Modal open. Modal and trigger parameters available.
            if ($(trigger).attr("href") == "#logEntryListModal") {
              $(modal).find('#modal-entry-list').find("li").remove();
              paraMap['path'] = trigger.text().trim();
              var fileName = paraMap['path'].substring(paraMap['path'].lastIndexOf('/')+1);  // Getting filename
              $(modal).find('#entry-list-modal-title').text(fileName);  // Set file name for modal window
              var url = "${requestScope['javax.servlet.forward.servlet_path']}?path=" + paraMap['path'];
              $(modal).find('#downloadLink').prop('href', url + "&action=download");
              $.get( url, function(data) {
                var entryList = $(modal).find('#modal-entry-list');
                $(data.entryList).each(function( index, element ) {
                  entryList.append("<li><a href='#logEntryViewModal'>" + element + "</a></li>");
                });
              });
            } else {
              paraMap['entry'] = trigger.text().trim();
              $(modal).find('#entry-view-modal-title').text(paraMap['entry']);
              var entryUrl = "${requestScope['javax.servlet.forward.servlet_path']}?path=" + paraMap['path'] + "&entry=" + paraMap['entry'];
              $.get( entryUrl, function(data) {
                $(modal).find('#entry-view-modal-content').text(data.entryContent);
              });
            }
          },
          complete: function() {
              console.log("modal closed!");
          }  // Callback for Modal close
        });
      });
  </script>

  <body>
    <div class='wide container'>
      <div class='row' id='release-container'>
        <h3>Directory List</h3>
        <c:forEach varStatus="dirLoop" var="dirName" items="${dirList}">
          <p>
            <a href="${requestScope['javax.servlet.forward.servlet_path']}?path=${dirName}">
              <c:out value="${dirName}"></c:out>
                <c:if test="${dirLoop.first && path ne '/'}">
                    (Move to Parent)
                </c:if>
            </a>
          </p>
          <c:if test="${!dirLoop.last}">
          </c:if>
        </c:forEach>
        <hr/>
        <h5>Current Directory Path : ${path}</h5>
        <hr/>
        <h3>File List</h3>
        <c:forEach varStatus="fileLoop" var="fileName" items="${fileList}">
          <p>
            <a href="#logEntryListModal">
              <c:out value="${fileName}"></c:out>
            </a>
          </p>
          <c:if test="${!fileLoop.last}">
          </c:if>
        </c:forEach>
      </div>
    </div>

    <%@ include file="footer.jsp" %>

    <!-- Modal For Zip file entries -->
    <div id="logEntryListModal" class="modal">
      <div class="modal-content">
        <h4 id="entry-list-modal-title" class="truncate"></h4>
        <div id="entry-list-modal-content">
          <ul id="modal-entry-list"></ul>
        </div>
      </div>
      <div class="modal-footer">
        <div class="row">
          <div class="col s3 offset-s6">
            <a href="#!" id="downloadLink" class="modal-action modal-close waves-effect waves-green btn">Download</a>
          </div>
          <div class="col s3">
            <a href="#!" class="modal-action modal-close waves-effect waves-green btn">Close</a>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal For Zip file entry's content -->
    <div id="logEntryViewModal" class="modal modal-fixed-footer">
      <div class="modal-content">
        <h4 id="entry-view-modal-title" class="truncate"></h4>
        <div id="entry-view-modal-content">

        </div>
      </div>
      <div class="modal-footer">
        <a href="#!" class="modal-action modal-close waves-effect waves-green btn">Close</a>
      </div>
    </div>
  </body>
</html>
