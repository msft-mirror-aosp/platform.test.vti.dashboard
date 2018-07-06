/*
 * Copyright (c) 2018 Google Inc. All Rights Reserved.
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

import com.android.vts.entity.CoverageEntity;
import com.android.vts.entity.TestCoverageStatusEntity;
import com.android.vts.entity.TestSuiteFileEntity;
import com.android.vts.entity.TestSuiteResultEntity;
import com.android.vts.proto.TestSuiteResultMessageProto.TestSuiteResultMessage;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.gson.Gson;
import com.googlecode.objectify.Key;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;

/**
 * REST endpoint for posting test suite data to the Dashboard.
 */
public class CoverageRestServlet extends BaseApiServlet {

  private static final Logger logger =
      Logger.getLogger(CoverageRestServlet.class.getName());

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    String cmd = request.getParameter("cmd");
    String coverageId = request.getParameter("coverageId");
    String testName = request.getParameter("testName");
    String testRunId = request.getParameter("testRunId");

    Boolean isIgnored = false;
    if (cmd.equals("disable")) {
      isIgnored = true;
    }
    CoverageEntity coverageEntity = CoverageEntity.findById(testName, testRunId, coverageId);
    coverageEntity.setIsIgnored(isIgnored);
    coverageEntity.save();

    TestCoverageStatusEntity testCoverageStatusEntity = TestCoverageStatusEntity.findById(testName);
    Long newCoveredLineCount =
        cmd.equals("disable") ? testCoverageStatusEntity.getUpdatedCoveredLineCount()
            - coverageEntity.getCoveredCount()
            : testCoverageStatusEntity.getUpdatedCoveredLineCount() + coverageEntity
                .getCoveredCount();
    Long newTotalLineCount =
        cmd.equals("disable") ? testCoverageStatusEntity.getUpdatedTotalLineCount() - coverageEntity
            .getTotalCount()
            : testCoverageStatusEntity.getUpdatedTotalLineCount() + coverageEntity.getTotalCount();
    testCoverageStatusEntity.setUpdatedCoveredLineCount(newCoveredLineCount);
    testCoverageStatusEntity.setUpdatedTotalLineCount(newTotalLineCount);
    testCoverageStatusEntity.save();

    String json = new Gson().toJson("Success!");
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(json);
  }
}
