/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.model.*;
import org.neo4j.graphdb.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents the in-memory state of a request to log a change in to the service
 * This payload is passed around services and is enriched.
 *
 * TrackResultBean is not persisted
 *
 * User: Mike Holdsworth
 * Since: 11/05/13
 */
public class TrackResultBean implements Serializable {
    private Collection<String> serviceMessages = new ArrayList<>();

    private Entity entity;              // Resolved entity
    private EntityLog currentLog;        // Log that was created
    private EntityLog deletedLog; // Log that was removed in response to a cancel request

    private Collection<EntityTag> tags; // Tags connected to the entity

    private EntityInputBean entityInputBean;// User payload
    private ContentInputBean contentInput;  // User content payload

    private transient DocumentType documentType;
    private String index;       // Which index is this indexed in
    private Boolean newEntity = false; // Flags that the Entity was created for the first time
    private ContentInputBean.LogStatus logStatus; // What status

    private TxRef txReference = null; // Reference used to track the transaction
    private String tenant = "";


    protected TrackResultBean() {
    }

    /**
     * @param serviceMessage server side error messages to return to the caller
     */
    public TrackResultBean(String serviceMessage) {
        this();
        addServiceMessage(serviceMessage);
    }

    /**
     * Entity is only used internally by fd-engine. it can not be serialized as JSON
     * Callers should rely on entityResultBean
     *
     * @param entity  internal node
     * @param entityInputBean user supplied content to create entity
     */
    public TrackResultBean(Fortress fortress, Entity entity, EntityInputBean entityInputBean) {
        this.entity = entity;
        this.entityInputBean = entityInputBean;
        this.contentInput = entityInputBean.getContent();
        this.index = fortress.getIndexName();

    }

    public TrackResultBean(Entity entity) {
        this.entity = entity;
        this.newEntity = entity.isNewEntity();

    }

    public TrackResultBean(Fortress fortress, Node entity, EntityInputBean entityInputBean) {
        //this.entityBean = new EntityBean(fortress, entity, null);
        //this.entity = new Entity(fortress, entity);
        this.entityInputBean = entityInputBean;
        this.contentInput = entityInputBean.getContent();
        this.index = fortress.getIndexName();

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackResultBean)) return false;

        TrackResultBean that = (TrackResultBean) o;

        if (entityInputBean != null ? !entityInputBean.equals(that.entityInputBean) : that.entityInputBean != null)
            return false;
        if (contentInput != null ? !contentInput.equals(that.contentInput) : that.contentInput!= null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = entityInputBean != null ? entityInputBean.hashCode() : 0;
        result = 31 * result + (contentInput != null ? contentInput.hashCode() : 0);
        return result;
    }

    public Collection<String> getServiceMessages() {
        return serviceMessages;
    }

    public void addServiceMessage(String serviceMessage) {

        this.serviceMessages.add(serviceMessage);
    }

    /**
     */
    public Entity getEntity() {
        return entity;
    }

    public void setCurrentLog(EntityLog currentLog) {
        this.currentLog = currentLog;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public EntityLog getCurrentLog() {
        return currentLog;
    }

    public void setDeletedLog(EntityLog entityLog) {
        this.deletedLog = entityLog;
    }

    public EntityLog getDeletedLog() {
        return deletedLog;
    }

    boolean entityExisted = false;

    public void entityExisted() {
        this.entityExisted = true;
    }

    public boolean entityExists() {
        return entityExisted;
    }

    public void setTags(Collection<EntityTag> tags) {
        this.tags = tags;
    }

    @JsonIgnore
    /**
     * Only used when creating  relationships for the purpose of search
     * that bypass the graph, i.e. transient EntityTags
     */
    public Collection<EntityTag> getTags() {
        return tags;
    }

    //    @JsonIgnore
    public ContentInputBean getContentInput() {
        // ToDo: Why are we tracking input in 2 places? Tracking content having already created the entity?
        // do with the "trackLog" endpoint
        return (entityInputBean == null?contentInput:entityInputBean.getContent());
    }

    /**
     * Content being tracked
     *
     * @param contentInputBean content provided as input to the track process
     */
    public void setContentInput(ContentInputBean contentInputBean) {
        this.contentInput = contentInputBean;
    }

    /**
     * EntityInput information provided when the track call was made
     */
    //@JsonIgnore
    public EntityInputBean getEntityInputBean() {
        return entityInputBean;
    }

    /**
     *
     * @return true if this log should be processed by the search service
     */
    public boolean processLog() {
        return  ( getContentInput() != null && logStatus != ContentInputBean.LogStatus.IGNORE);
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    @JsonIgnore
    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getIndex(){
        return index;
    }

    @Override
    public String toString() {
        return "TrackResultBean{" +
                "entity=" + entity +
                '}';
    }

    public void setNewEntity() {
        newEntity = true;
    }

    public Boolean isNewEntity() {
        return newEntity;
    }

    @JsonIgnore
    public String getMetaKey() {
        return entity.getMetaKey();
    }

    public ContentInputBean.LogStatus getLogStatus() {
        return logStatus;
    }

    public void setLogStatus(ContentInputBean.LogStatus logStatus) {
        this.logStatus = logStatus;
    }

    boolean logIgnored = false;

    public void setLogIgnored() {
        this.logIgnored = true;
    }

    public boolean isLogIgnored() {
        // FixMe: Suspicious about the TRACK_ONLY status. One can ignore track and write to fd-search
        return logIgnored ||
                getLogStatus() == ContentInputBean.LogStatus.IGNORE||
                getLogStatus() == ContentInputBean.LogStatus.TRACK_ONLY;
    }

    public void setTxReference(TxRef txReference) {
        this.txReference = txReference;
    }

    public TxRef getTxReference() {
        return txReference;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
