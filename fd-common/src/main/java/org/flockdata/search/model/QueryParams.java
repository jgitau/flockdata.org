/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.model;

import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.track.bean.MatrixInputBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Encapsulated search parameters
 * User: mike
 * Date: 12/04/14
 * Time: 9:44 AM
 */
public class QueryParams implements QueryInterface {
    private String searchText;
    private String segment;
    private String index;
    private ArrayList<String> fields;
    private String key;
    private String company;
    private String fortress;
    private String[] types;
    private String[] data;
    private Integer size = null;
    private Integer from = null;
    private boolean entityOnly;
    private String code;
    private Map<String, Object> query; // Raw query to pass through to ES
    private Map<String, Object> aggs; // Raw aggs to pass through to ES
    private Map<String,Object> filter; // Raw filter to pass through to ES
    private ArrayList<String> tags;
    private ArrayList<String> relationships = new ArrayList<>();

    public QueryParams(String searchText) {
        this.searchText = searchText;
    }

    public QueryParams(FortressSegment segment) {
        this(segment.getFortress());
        this.segment = segment.getCode();
    }

    public QueryParams(String index, String docType, String code) {
        this.index = index;
        setTypes(docType);
        setCode(code);
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public QueryParams() {
    }


    private QueryParams(Fortress fortress) {
        this();
        setFortress(fortress.getCode());
        setCompany(fortress.getCompany().getCode());
    }

    public QueryParams(Company company, MatrixInputBean input) {
        this.searchText = input.getQueryString();
        this.company = company.getName();
        this.size = input.getSampleSize();
        this.tags = input.getConcepts();
        if (input.getFromRlxs() != null && !input.getFromRlxs().isEmpty())
            this.relationships.addAll(input.getFromRlxs());

        if (input.getToRlxs() != null && !input.getToRlxs().isEmpty())
            this.relationships.addAll(input.getToRlxs());

        if (input.getDocuments() != null && !input.getDocuments().isEmpty()) {
            types = new String[input.getDocuments().size()];
            int i = 0;
            for (String s : input.getDocuments()) {
                this.types[i++] = s.toLowerCase();
            }
        }
    }

    public String getSearchText() {
        return searchText;
    }

    public QueryParams setSearchText(String searchText) {
        this.searchText = searchText;
        return this;
    }

    public String getCompany() {
        return company;
    }

    public QueryParams setCompany(String company) {
        this.company = company;
        return this;
    }

    public String getFortress() {
        return fortress;
    }

    public QueryParams setFortress(String fortress) {
        this.fortress = fortress;
        return this;
    }

    public String[] getTypes() {
        return types;
    }

    public String[] getData() {
        return data;
    }

    public QueryParams setTypes(String... types) {
        this.types = types;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    @Override
    public String toString() {
        return "QueryParams{" +
                "searchText='" + searchText + '\'' +
                ", company='" + company + '\'' +
                ", fortress='" + fortress + '\'' +
                ", docTypes='" + Arrays.toString(types) + '\'' +
                ", segment='" + segment + '\'' +
                '}';
    }

    public void setEntityOnly(boolean entityOnly) {
        this.entityOnly = entityOnly;
    }

    public boolean isEntityOnly() {
        return entityOnly;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }


    public QueryParams setCode(String code) {
        this.code = code;
        return this;
    }

    public String getCode() {
        return code;
    }

    public ArrayList<String> getRelationships() {
        return relationships;
    }

    /**
     * @return elasticsearch body to execute against the requested index
     */
    public Map<String, Object> getQuery() {
        return query;
    }

    public QueryParams setQuery(Map<String, Object> query) {
        this.query = query;
        return this;
    }

    public Map<String, Object> getAggs() {
        return aggs;
    }

    public QueryParams setFields(ArrayList<String> fields) {
        this.fields = fields;
        return this;
    }

    public ArrayList<String> getFields() {
        return fields;
    }


    public String getSegment() {
        return segment;
    }

    public QueryParams setSegment(String segment) {
        this.segment = segment;
        return this;
    }

    public String getIndex() {
        return index;
    }

    boolean searchTagsOnly = false;

    public QueryParams searchTags() {
        this.searchTagsOnly = true;
        return this;
    }

    public boolean isSearchTagsOnly() {
        return searchTagsOnly;
    }

    @Override
    public Map<String,Object> getFilter() {
        return filter;
    }

    public QueryParams setIndex(String index) {
        this.index = index;
        return this;
    }
}
