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

package com.android.vts.util;

import com.android.vts.entity.ProfilingPointRunEntity;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Helper object for describing time-series box plot data. */
public class BoxPlot extends Graph {
    private static final String LABEL_KEY = "label";
    private static final String SERIES_KEY = "seriesList";
    private static final String MEAN_KEY = "mean";
    private static final String STD_KEY = "std";

    private final String xLabel = "Day";
    private String yLabel;
    private String name;
    private GraphType type = GraphType.BOX_PLOT;
    private int count;
    private final Map<String, ProfilingPointSummary> seriesMap;
    private final Set<String> labelSet;
    private final List<String> labels;

    public BoxPlot(String name) {
        this.name = name;
        this.count = 0;
        seriesMap = new HashMap<>();
        labelSet = new HashSet<>();
        labels = new ArrayList<>();
    }

    /**
     * Get the x axis label.
     *
     * @return The x axis label.
     */
    @Override
    public String getXLabel() {
        return xLabel;
    }

    /**
     * Get the graph type.
     *
     * @return The graph type.
     */
    @Override
    public GraphType getType() {
        return type;
    }

    /**
     * Get the name of the graph.
     *
     * @return The name of the graph.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the y axis label.
     *
     * @return The y axis label.
     */
    @Override
    public String getYLabel() {
        return yLabel;
    }

    /**
     * Get the number of data points stored in the graph.
     *
     * @return The number of data points stored in the graph.
     */
    @Override
    public int size() {
        return this.count;
    }

    /**
     * Add data to the graph.
     *
     * @param label The name of the category.
     * @param profilingPoint The ProfilingPointRunEntity containing data to add.
     */
    @Override
    public void addData(String label, ProfilingPointRunEntity profilingPoint) {
        addSeriesData(label, "", profilingPoint);
    }

    /**
     * Add data to the graph.
     *
     * @param label The name of the category.
     * @param series The data series to add data to.
     * @param profilingPoint The ProfilingPointRunEntity containing data to add.
     */
    public void addSeriesData(String label, String series, ProfilingPointRunEntity profilingPoint) {
        if (profilingPoint.values.size() == 0)
            return;
        if (!seriesMap.containsKey(series)) {
            seriesMap.put(series, new ProfilingPointSummary());
        }
        ProfilingPointSummary summary = seriesMap.get(series);
        summary.updateLabel(profilingPoint, label);
        if (labelSet.add(label)) {
            labels.add(label);
        }
        yLabel = profilingPoint.xLabel;
        ++count;
    }

    /**
     * Serializes the graph to json format.
     *
     * @return A JsonElement object representing the graph object.
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        List<JsonObject> stats = new ArrayList<>();
        List<String> seriesList = new ArrayList<>(seriesMap.keySet());
        Collections.sort(seriesList);
        Collections.reverse(labels);
        for (String label : labels) {
            JsonObject statJson = new JsonObject();
            String boxLabel = null;
            List<JsonObject> statList = new ArrayList<>(seriesList.size());
            for (String series : seriesList) {
                ProfilingPointSummary summary = seriesMap.get(series);
                JsonObject statSummary = new JsonObject();
                Double mean = null;
                Double std = null;
                if (summary.hasLabel(label) && summary.getStatSummary(label).getCount() > 0) {
                    StatSummary stat = summary.getStatSummary(label);
                    boxLabel = stat.getLabel();
                    mean = stat.getMean();
                    std = 0.;
                    if (stat.getCount() > 1) {
                        std = stat.getStd();
                    }
                }
                statSummary.addProperty(MEAN_KEY, mean);
                statSummary.addProperty(STD_KEY, std);
                statList.add(statSummary);
            }
            statJson.addProperty(LABEL_KEY, boxLabel);
            statJson.add(VALUE_KEY, new Gson().toJsonTree(statList));
            stats.add(statJson);
        }
        json.add(VALUE_KEY, new Gson().toJsonTree(stats));
        json.add(SERIES_KEY, new Gson().toJsonTree(seriesList));
        return json;
    }
}
