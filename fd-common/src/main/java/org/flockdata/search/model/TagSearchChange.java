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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.model.Tag;
import org.flockdata.track.bean.AliasResultBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.SearchChange;
import org.flockdata.track.service.EntityService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents data about a tag that requires indexing
 * <p>
 * Created by mike on 15/05/16.
 */
public class TagSearchChange implements SearchChange {

    private Map<String, Object> props = new HashMap<>(); // User defined properties
    private String code;
    private String name = null;
    private String type = Type.TAG.name();
    private String documentType;
    private String key = null;
    private String description;
    private Long id;
    private Long logId = null;
    private boolean delete = false;
    private String indexName;
    private boolean forceReindex = false;
    private boolean replyRequired = false;
    private String searchKey = null; // how to find this object in an es index

    private Collection<AliasResultBean> aliases = new ArrayList<>();
    private EntityKeyBean parent = null;
    private EntityService.TAG_STRUCTURE tagStructure = null;

    TagSearchChange() {

    }

    public TagSearchChange(String indexName, Tag tag) {
        this();
        this.id = tag.getId();
        this.code = tag.getCode();
        this.name = tag.getName();
        this.documentType = tag.getLabel();
        this.key = tag.getKey();
        this.indexName = indexName;
        this.searchKey = key;
        if (tag.hasProperties())
            this.props = tag.getProperties();
        aliases.addAll(tag.getAliases().stream().map(AliasResultBean::new).collect(Collectors.toList()));
    }

    @Override
    @JsonIgnore
    public boolean isType(Type type) {
        return getType().equals(type.name());
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getSearchKey() {
        return searchKey;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Long getLogId() {
        return logId;
    }

    @Override
    public void setSearchKey(String key) {
        this.searchKey = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getIndexName() {
        return indexName;
    }

    @Override
    @JsonIgnore
    public String getFortressName() {
        return null;
    }

    @Override
    public String getDocumentType() {
        return documentType;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public TagSearchChange setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setReplyRequired(boolean required) {

    }

    @Override
    public boolean isReplyRequired() {
        return replyRequired;
    }

    @Override
    public boolean isForceReindex() {
        return forceReindex;
    }

    @Override
    public Boolean isDelete() {
        return delete;
    }

    @Override
    public Map<String, Object> getProps() {
        return props;
    }

    @Override
    @JsonIgnore
    public EntityService.TAG_STRUCTURE getTagStructure() {
        return tagStructure;
    }

    @Override
    public EntityKeyBean getParent() {
        return parent;
    }

    public Collection<AliasResultBean> getAliases() {
        return aliases;
    }
}
