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

package com.android.vts.util;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Helper class for pagination. */
public class Pagination<T> implements Iterable<T> {

    /** the default page size */
    public static final int DEFAULT_PAGE_SIZE = 10;
    /** the default page window size */
    private static final int DEFAULT_PAGE_WINDOW = 10;

    /** the current page */
    private int page;

    /** the page window size */
    private int pageSize = DEFAULT_PAGE_SIZE;

    /** the total number of found entities */
    private int totalCount;

    /** the cursor string token where to start */
    private String nextPageCountToken = "";

    /** the maximum number of pages */
    private int maxPages;

    /** the list of object on the page */
    private List<T> list = new ArrayList<>();

    public Pagination(List<T> list, int page, int pageSize, int totalCount) {
        this.list = list;
        this.page = page;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    public Pagination(Query<T> query, int page, int pageSize, String nextPageToken) {
        this.page = page;
        this.pageSize = pageSize;

        int limitValue = pageSize * DEFAULT_PAGE_WINDOW + pageSize;
        query = query.limit(limitValue);
        if (nextPageToken.equals("")) {
            this.totalCount = query.count();
        } else {
            query = query.startAt(Cursor.fromWebSafeString(nextPageToken));
            this.totalCount = query.count();
        }

        this.maxPages = this.totalCount / this.pageSize + (this.totalCount % this.pageSize == 0 ? 0 : 1);

        QueryResultIterator<T> resultIterator = query.iterator();
        int iteratorIndex = 1;
        int startIndex = page / pageSize * pageSize;
        while (resultIterator.hasNext()) {
            if (startIndex <= iteratorIndex && iteratorIndex < startIndex + this.pageSize)
                this.list.add(resultIterator.next());
            else
                resultIterator.next();
            iteratorIndex++;
        }

        this.nextPageCountToken = resultIterator.getCursor().toWebSafeString();
    }

    public Iterator<T> iterator() {
        return list.iterator();
    }

    /**
     * Gets the total number of objects.
     *
     * @return  the total number of objects as an int
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Gets the number of page window.
     *
     * @return  the number of page window as an int
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Gets the maximum number of pages.
     *
     * @return  the maximum number of pages as an int
     */
    public int getMaxPages() {
        return this.maxPages;
    }

    /**
     * Gets the page.
     *
     * @return  the page as an int
     */
    public int getPage() {
        return page;
    }

    /**
     * Gets the minimum page in the window.
     *
     * @return  the page number
     */
    public int getMinPageRange() {
        if (this.getPage() < DEFAULT_PAGE_WINDOW) {
            return 1;
        } else {
            return this.getPage() / DEFAULT_PAGE_WINDOW * DEFAULT_PAGE_WINDOW;
        }
    }

    /**
     * Gets the maximum page in the window.
     *
     * @return  the page number
     */
    public int getMaxPageRange() {
        if (this.getPage() < DEFAULT_PAGE_WINDOW) {
            return DEFAULT_PAGE_WINDOW;
        } else {
            if (this.getMaxPages() > DEFAULT_PAGE_WINDOW) {
                return this.getPage() / DEFAULT_PAGE_WINDOW * DEFAULT_PAGE_WINDOW + this.pageSize;
            } else {
                return this.getMinPageRange() + this.getMaxPages();
            }
        }
    }

    /**
     * Gets the subset of the list for the current page.
     *
     * @return  a List
     */
    public List<T> getList() {
        return this.list;
    }

    /**
     * Gets the cursor token for the next page starting point.
     *
     * @return  a string of cursor of next starting point
     */
    public String getNextPageCountToken() {
        return this.nextPageCountToken;
    }

}
