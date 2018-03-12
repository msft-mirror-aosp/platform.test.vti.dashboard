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

package com.android.vts.servlet;

import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A GCS log servlet read log zip file from Google Cloud Storage bucket and show the content in it
 * from the zip file by unarchiving it
 */
@SuppressWarnings("serial")
public class ShowGcsLogServlet extends BaseServlet {

    private static final String GCS_LOG_JSP = "WEB-INF/jsp/show_gcs_log.jsp";

    /** Google Cloud Storage project ID */
    private static final String GCS_PROJECT_ID = System.getProperty("GCS_PROJECT_ID");
    /** Google Cloud Storage project's key file to access the storage */
    private static final String GCS_KEY_FILE = System.getProperty("GCS_KEY_FILE");
    /** Google Cloud Storage project's default bucket name for vtslab log files */
    private static final String GCS_BUCKET_NAME = System.getProperty("GCS_BUCKET_NAME");

    /**
     * This is the key file to access vtslab-gcs project. It will allow the dashboard to have a full
     * control of the bucket.
     */
    private InputStream keyFileInputStream;

    /** This is the instance of java google storage library */
    private Storage storage;

    /** This is the instance of App Engine memcache service java library */
    private MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();

    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);
        keyFileInputStream =
                this.getServletContext().getResourceAsStream("/WEB-INF/keys/" + GCS_KEY_FILE);

        if (keyFileInputStream == null) {
            logger.log(Level.SEVERE, "Error GCS key file is not exiting. Check key file!");
        } else {
            try {
                storage =
                        StorageOptions.newBuilder()
                                .setProjectId(GCS_PROJECT_ID)
                                .setCredentials(
                                        ServiceAccountCredentials.fromStream(keyFileInputStream))
                                .build()
                                .getService();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error on creating storage instance!");
            }
        }
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
    }

    @Override
    public PageType getNavParentType() {
        return PageType.TOT;
    }

    @Override
    public List<Page> getBreadcrumbLinks(HttpServletRequest request) {
        return null;
    }

    @Override
    public void doGetHandler(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (keyFileInputStream == null) {
            request.setAttribute("error_title", "GCS Key file Error");
            request.setAttribute("error_message", "The GCS Key file is not existed!");
            RequestDispatcher dispatcher = request.getRequestDispatcher(ERROR_MESSAGE_JSP);
            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                logger.log(Level.SEVERE, "Servlet Excpetion caught : ", e);
            }
        } else {

            String action =
                    request.getParameter("action") == null
                            ? "read"
                            : request.getParameter("action");
            String path = request.getParameter("path") == null ? "/" : request.getParameter("path");
            String entry =
                    request.getParameter("entry") == null ? "" : request.getParameter("entry");
            Path pathInfo = Paths.get(path);

            Bucket vtsReportBucket = storage.get(GCS_BUCKET_NAME);

            List<String> dirList = new ArrayList<>();
            List<String> fileList = new ArrayList<>();
            List<String> entryList = new ArrayList<>();
            Map<String, Object> resultMap = new HashMap<>();
            String entryContent = "";

            if (pathInfo.toString().endsWith(".zip")) {

                Blob blobFile = (Blob) this.syncCache.get(path.toString());
                if (blobFile == null) {
                    blobFile = vtsReportBucket.get(path);
                    this.syncCache.put(path.toString(), blobFile);
                }

                if (action.equalsIgnoreCase("read")) {
                    InputStream blobInputStream = new ByteArrayInputStream(blobFile.getContent());
                    ZipInputStream zipInputStream = new ZipInputStream(blobInputStream);

                    ZipEntry zipEntry;
                    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                        if (zipEntry.isDirectory()) {

                        } else {
                            if (entry.length() > 0) {
                                System.out.println("param entry => " + entry);
                                if (zipEntry.getName().equals(entry)) {
                                    System.out.println("matched !!!! " + zipEntry.getName());
                                    entryContent =
                                            IOUtils.toString(
                                                    zipInputStream, StandardCharsets.UTF_8.name());
                                }
                            } else {
                                entryList.add(zipEntry.getName());
                            }
                        }
                    }
                    resultMap.put("entryList", entryList);
                    resultMap.put("entryContent", entryContent);

                    String json = new Gson().toJson(resultMap);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(json);
                } else {
                    response.setContentType("application/octet-stream");
                    response.setContentLength(blobFile.getSize().intValue());
                    response.setHeader(
                            "Content-Disposition",
                            "attachment; filename=\"" + pathInfo.getFileName() + "\"");

                    response.getOutputStream().write(blobFile.getContent());
                }

            } else {

                logger.log(Level.INFO, "path info => " + pathInfo);
                logger.log(Level.INFO, "path name count => " + pathInfo.getNameCount());

                BlobListOption[] listOptions;
                if (pathInfo.getNameCount() == 0) {
                    listOptions = new BlobListOption[] {BlobListOption.currentDirectory()};
                } else {
                    if (pathInfo.getNameCount() <= 1) {
                        dirList.add("/");
                    } else {
                        dirList.add(pathInfo.getParent().toString());
                    }
                    listOptions =
                            new BlobListOption[] {
                                BlobListOption.currentDirectory(),
                                BlobListOption.prefix(pathInfo.toString() + "/")
                            };
                }

                Iterator<Blob> blobIterator = vtsReportBucket.list(listOptions).iterateAll();
                while (blobIterator.hasNext()) {
                    Blob blob = blobIterator.next();
                    logger.log(Level.INFO, "blob name => " + blob);
                    if (blob.isDirectory()) {
                        logger.log(Level.INFO, "directory name => " + blob.getName());
                        dirList.add(blob.getName());
                    } else {
                        logger.log(Level.INFO, "file name => " + blob.getName());
                        fileList.add(blob.getName());
                    }
                }

                response.setStatus(HttpServletResponse.SC_OK);
                request.setAttribute("entryList", entryList);
                request.setAttribute("dirList", dirList);
                request.setAttribute("fileList", fileList);
                request.setAttribute("path", path);
                RequestDispatcher dispatcher = request.getRequestDispatcher(GCS_LOG_JSP);
                try {
                    dispatcher.forward(request, response);
                } catch (ServletException e) {
                    logger.log(Level.SEVERE, "Servlet Excpetion caught : ", e);
                }
            }
        }
    }
}
