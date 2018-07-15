<%--
  ~ Copyright (c) 2017 Google Inc. All Rights Reserved.
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
  <!-- <link rel='stylesheet' href='/css/dashboard_main.css'> -->
  <%@ include file='header.jsp' %>
  <link type='text/css' href='/css/show_test_runs_common.css' rel='stylesheet'>
  <link type='text/css' href='/css/test_results.css' rel='stylesheet'>
  <link rel='stylesheet' href='/css/search_header.css'>
  <script type='text/javascript' src='https://www.gstatic.com/charts/loader.js'></script>
  <script src='https://www.gstatic.com/external_hosted/moment/min/moment-with-locales.min.js'></script>
  <script src='js/time.js'></script>
  <script src='js/test_results.js'></script>
  <script src='js/search_header.js'></script>
  <script type='text/javascript'>
      google.charts.load('current', {'packages':['table', 'corechart']});
      google.charts.setOnLoadCallback(drawStatsChart);
      google.charts.setOnLoadCallback(drawCoverageCharts);

      var search;

      $(document).ready(function() {
          $('#test-results-container').showTests(${testRuns}, true);
          search = $('#filter-bar').createSearchHeader('Code Coverage', '', refresh);
          search.addFilter('Branch', 'branch', {
            corpus: ${branches}
          }, ${branch});
          search.addFilter('Device', 'device', {
            corpus: ${devices}
          }, ${device});
          search.addFilter('Device Build ID', 'deviceBuildId', {}, ${deviceBuildId});
          search.addFilter('Test Build ID', 'testBuildId', {}, ${testBuildId});
          search.addFilter('Host', 'hostname', {}, ${hostname});
          search.addFilter('Passing Count', 'passing', {
            type: 'number',
            width: 's2'
          }, ${passing});
          search.addFilter('Non-Passing Count', 'nonpassing', {
            type: 'number',
            width: 's2'
          }, ${nonpassing});
          search.addRunTypeCheckboxes(${showPresubmit}, ${showPostsubmit});
          search.display();

          var no_data_msg = "NO DATA";

          $('#coverageModalGraph').modal({
                width: '75%',
                dismissible: true, // Modal can be dismissed by clicking outside of the modal
                opacity: .5, // Opacity of modal background
                inDuration: 300, // Transition in duration
                outDuration: 200, // Transition out duration
                startingTop: '4%', // Starting top style attribute
                endingTop: '10%', // Ending top style attribute
                ready: function(modal, trigger) { // Callback for Modal open. Modal and trigger parameters available.
                  var testname = modal.data('testname');
                  $('#coverageModalTitle').text("Code Coverage Chart : " + testname);
                  var query = new google.visualization.Query('show_coverage_overview?pageType=datatable&testName=' + testname);
                  // Send the query with a callback function.
                  query.send(handleQueryResponse);
                },
                complete: function() {
                  $('#coverage_combo_chart_div').empty();
                  $('#coverage_line_chart_div').empty();
                  $('#coverage_table_chart_div').empty();

                  $("div.valign-wrapper > h2.center-align:contains('" + no_data_msg + "')").each(function(index){
                    $(this).parent().remove();
                  });
                  $("span.indicator.badge.blue:contains('Graph')").each(function( index ) {
                    $(this).removeClass('blue');
                    $(this).addClass('grey');
                  });

                  $('#dataTableLoading').show("slow");
                } // Callback for Modal close
              }
          );

          // Handle the query response.
          function handleQueryResponse(response) {
            $('#dataTableLoading').hide("slow");
            if (response.isError()) {
              alert('Error in query: ' + response.getMessage() + ' ' + response.getDetailedMessage());
              return;
            }
            // Draw the visualization.
            var data = response.getDataTable();
            if (data.getNumberOfRows() == 0) {
              var blankData = '<div class="valign-wrapper" style="height: 90%;">'
                            + '<h2 class="center-align" style="width: 100%;">' + no_data_msg + '</h2>'
                            + '</div>';
              $('#coverageModalTitle').after(blankData);
              return;
            }
            data.sort([{column: 0}]);

            var date_formatter = new google.visualization.DateFormat({ pattern: "yyyy-MM-dd" });
            date_formatter.format(data, 0);

            var dataView = new google.visualization.DataView(data);

            // Disable coveredLine and totalLine
            dataView.hideColumns([1,2]);

            var lineOptions = {
              title: 'Source Code Line Coverage',
              width: '100%',
              height: 450,
              curveType: 'function',
              intervals: { 'color' : 'series-color' },
              interval: {
                'fill': {
                  'style': 'area',
                  'curveType': 'function',
                  'fillOpacity': 0.2
                },
                'bar': {
                  'style': 'bars',
                  'barWidth': 0,
                  'lineWidth': 1,
                  'pointSize': 3,
                  'fillOpacity': 1
                }},
              legend: { position: 'bottom' },
              tooltip: { isHtml: true },
              fontName: 'Roboto',
              titleTextStyle: {
                color: '#757575',
                fontSize: 16,
                bold: false
              },
              pointsVisible: true,
              vAxis:{
                title: 'Code Coverage Ratio (%)',
                titleTextStyle: {
                  color: '#424242',
                  fontSize: 12,
                  italic: false
                },
                textStyle: {
                  fontSize: 12,
                  color: '#757575'
                },
              },
              hAxis: {
                title: 'Date',
                format: 'yyyy-MM-dd',
                minTextSpacing: 0,
                showTextEvery: 1,
                slantedText: true,
                slantedTextAngle: 45,
                textStyle: {
                  fontSize: 12,
                  color: '#757575'
                },
                titleTextStyle: {
                  color: '#424242',
                  fontSize: 12,
                  italic: false
                }
              },
            };
            var lineChart = new google.visualization.LineChart(document.getElementById('coverage_line_chart_div'));
            lineChart.draw(dataView, lineOptions);

            var tableOptions = {
              title: 'Covered/Total Source Code Line Count (SLOC)',
              width: '95%',
              // height: 350,
              is3D: true
            };
            var tableChart = new google.visualization.Table(document.getElementById('coverage_table_chart_div'));
            tableChart.draw(data, tableOptions);
          }
      });

      // draw test statistics chart
      function drawStatsChart() {
          var testStats = ${testStats};
          if (testStats.length < 1) {
              return;
          }
          var resultNames = ${resultNamesJson};
          var rows = resultNames.map(function(res, i) {
              nickname = res.replace('TEST_CASE_RESULT_', '').replace('_', ' ')
                         .trim().toLowerCase();
              return [nickname, parseInt(testStats[i])];
          });
          rows.unshift(['Result', 'Count']);

          // Get CSS color definitions (or default to white)
          var colors = resultNames.map(function(res) {
              return $('.' + res).css('background-color') || 'white';
          });

          var data = google.visualization.arrayToDataTable(rows);
          var options = {
              is3D: false,
              colors: colors,
              fontName: 'Roboto',
              fontSize: '14px',
              legend: {position: 'labeled'},
              tooltip: {showColorCode: true, ignoreBounds: false},
              chartArea: {height: '80%', width: '90%'},
              pieHole: 0.4
          };

          var chart = new google.visualization.PieChart(document.getElementById('pie-chart-stats'));
          chart.draw(data, options);
      }

      // draw the coverage pie charts
      function drawCoverageCharts() {
          var coveredLines = ${coveredLines};
          var uncoveredLines = ${uncoveredLines};
          var rows = [
              ["Result", "Count"],
              ["Covered Lines", coveredLines],
              ["Uncovered Lines", uncoveredLines]
          ];

          // Get CSS color definitions (or default to white)
          var colors = [
              $('.TEST_CASE_RESULT_PASS').css('background-color') || 'white',
              $('.TEST_CASE_RESULT_FAIL').css('background-color') || 'white'
          ]

          var data = google.visualization.arrayToDataTable(rows);


          var optionsRaw = {
              is3D: false,
              colors: colors,
              fontName: 'Roboto',
              fontSize: '14px',
              pieSliceText: 'value',
              legend: {position: 'bottom'},
              chartArea: {height: '80%', width: '90%'},
              tooltip: {showColorCode: true, ignoreBounds: false, text: 'value'},
              pieHole: 0.4
          };

          var optionsNormalized = {
              is3D: false,
              colors: colors,
              fontName: 'Roboto',
              fontSize: '14px',
              legend: {position: 'bottom'},
              tooltip: {showColorCode: true, ignoreBounds: false, text: 'percentage'},
              chartArea: {height: '80%', width: '90%'},
              pieHole: 0.4
          };

          var chart = new google.visualization.PieChart(document.getElementById('pie-chart-coverage-raw'));
          chart.draw(data, optionsRaw);

          chart = new google.visualization.PieChart(document.getElementById('pie-chart-coverage-normalized'));
          chart.draw(data, optionsNormalized);
      }

      // refresh the page to see the runs matching the specified filter
      function refresh() {
        var link = '${pageContext.request.contextPath}' +
            '/show_coverage_overview?' + search.args();
        if (${unfiltered}) {
          link += '&unfiltered=';
        }
        window.open(link,'_self');
      }

  </script>

  <body>
    <div class='wide container'>
      <div id='filter-bar'></div>
      <div class='row'>
        <div class='col s12'>
          <div class='col s12 card center-align'>
            <div id='legend-wrapper'>
              <c:forEach items='${resultNames}' var='res'>
                <div class='center-align legend-entry'>
                  <c:set var='trimmed' value='${fn:replace(res, "TEST_CASE_RESULT_", "")}'/>
                  <c:set var='nickname' value='${fn:replace(trimmed, "_", " ")}'/>
                  <label for='${res}'>${nickname}</label>
                  <div id='${res}' class='${res} legend-bubble'></div>
                </div>
              </c:forEach>
            </div>
          </div>
        </div>
        <div class='col s4 valign-wrapper'>
          <!-- pie chart -->
          <div class='pie-chart-wrapper col s12 valign center-align card'>
            <h6 class='pie-chart-title'>Test Statistics</h6>
            <div id='pie-chart-stats' class='pie-chart-div'></div>
          </div>
        </div>
        <div class='col s4 valign-wrapper'>
          <!-- pie chart -->
          <div class='pie-chart-wrapper col s12 valign center-align card'>
            <h6 class='pie-chart-title'>Line Coverage (Raw)</h6>
            <div id='pie-chart-coverage-raw' class='pie-chart-div'></div>
          </div>
        </div>
        <div class='col s4 valign-wrapper'>
          <!-- pie chart -->
          <div class='pie-chart-wrapper col s12 valign center-align card'>
            <h6 class='pie-chart-title'>Line Coverage (Normalized)</h6>
            <div id='pie-chart-coverage-normalized' class='pie-chart-div'></div>
          </div>
        </div>
      </div>
      <div class='col s12' id='test-results-container'></div>
    </div>

    <!-- Modal Structure -->
    <div id="coverageModalGraph" class="modal modal-fixed-footer" style="width: 75%;">
      <div class="modal-content">
        <h4 id="coverageModalTitle">Code Coverage Chart</h4>

        <div class="preloader-wrapper big active loaders">
          <div id="dataTableLoading" class="spinner-layer spinner-blue-only">
            <div class="circle-clipper left">
              <div class="circle"></div>
            </div>
            <div class="gap-patch">
              <div class="circle"></div>
            </div>
            <div class="circle-clipper right">
              <div class="circle"></div>
            </div>
          </div>
        </div>

        <!--Div that will hold the visualization graph -->
        <div id="coverage_line_chart_div"></div>
        <p></p>
        <p></p>
        <div id="coverage_table_chart_div" class="center-align"></div>
      </div>
      <div class="modal-footer">
        <a href="#!" class="modal-action modal-close waves-effect waves-green btn-flat ">Close</a>
      </div>
    </div>

    <%@ include file="footer.jsp" %>
  </body>
</html>
