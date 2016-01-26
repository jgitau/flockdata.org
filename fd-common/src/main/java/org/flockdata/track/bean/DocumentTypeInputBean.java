/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.track.bean;

import org.flockdata.model.DocumentType;
import org.flockdata.track.service.EntityService;

/**
 * User: mike
 * Date: 10/10/14
 * Time: 12:06 PM
 */
public class DocumentTypeInputBean {
    private String name;
    private String code;

    private String geoQuery;
    private DocumentType.VERSION versionStrategy = DocumentType.VERSION.FORTRESS;
    EntityService.TAG_STRUCTURE tagStructure = EntityService.TAG_STRUCTURE.DEFAULT;
    private Boolean searchEnabled; // If null default to fortress

    DocumentTypeInputBean(){}

    public DocumentTypeInputBean(String docName) {
        this();
        this.name = docName;
        this.code = docName;
    }

    public String getName() {
        return name;
    }

    public DocumentTypeInputBean setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public DocumentTypeInputBean setCode(String code) {
        this.code = code;
        return this;
    }

    // MKH - Overrides the geo query path for this DocumentType. VULNERABLE!
    // DAT-507
    public String getGeoQuery() {
        return geoQuery;
    }

    public DocumentTypeInputBean setVersionStrategy(DocumentType.VERSION versionStrategy) {
        this.versionStrategy = versionStrategy;
        return this;
    }

    public DocumentType.VERSION getVersionStrategy() {
        return versionStrategy;
    }

    public EntityService.TAG_STRUCTURE getTagStructure() {
        return tagStructure;
    }

    public DocumentTypeInputBean name(final String name) {
        this.name = name;
        return this;
    }

    public DocumentTypeInputBean code(final String code) {
        this.code = code;
        return this;
    }

    public DocumentTypeInputBean geoQuery(final String geoQuery) {
        this.geoQuery = geoQuery;
        return this;
    }

    public DocumentTypeInputBean versionStrategy(final DocumentType.VERSION versionStrategy) {
        this.versionStrategy = versionStrategy;
        return this;
    }

    public DocumentTypeInputBean tagStructure(final EntityService.TAG_STRUCTURE tagStructure) {
        this.tagStructure = tagStructure;
        return this;
    }

    public Boolean isSearchEnabled() {
        return searchEnabled;
    }


    public DocumentTypeInputBean setTagStructure(EntityService.TAG_STRUCTURE tagStructure) {
        this.tagStructure = tagStructure;
        return this;
    }
}
