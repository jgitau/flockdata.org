/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.repo.neo4j.model;

import com.auditbucket.engine.repo.LogWhatData;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.model.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 5:57 PM
 */
@NodeEntity(useShortNames = true)
@TypeAlias("Log")
public class LogNode implements Log {
    private static final String COLON = ":";
    @GraphId
    private Long id;

    @RelatedTo(elementClass = FortressUserNode.class, type = "CHANGED", direction = Direction.INCOMING, enforceTargetType = true)
    @Fetch
    private FortressUserNode madeBy;

    @RelatedTo(elementClass = TxRefNode.class, type = "AFFECTED", direction = Direction.INCOMING, enforceTargetType = true)
    private TxRef txRef;

    @RelatedToVia(elementClass = LoggedRelationship.class, type ="LOGGED", direction = Direction.INCOMING)
    private LoggedRelationship trackLog;

    @RelatedTo(elementClass = ChangeEventNode.class, type = "TRACK_EVENT", direction = Direction.OUTGOING)
    @Fetch
    private ChangeEventNode event;

    private String comment;
    private String storage ;
    // Neo4J will not persist a byte[] over it's http interface. Probably fixed in V2, but not in our version
    @JsonIgnore
    private boolean compressed = false;
    private String name;

    @RelatedTo(type = "PREVIOUS_LOG", direction = Direction.OUTGOING)
    private LogNode previousLog;

    @Transient
    private LogWhatData auditWhat;

    @Override
    public String toString() {
        return "LogNode{" +
                "id=" + id +
                ", madeBy=" + madeBy +
                ", event=" + event +
                '}';
    }

    protected LogNode() {}

    public LogNode(FortressUser madeBy, LogInputBean inputBean, TxRef txRef) {
        this();
        this.madeBy = (FortressUserNode) madeBy;

        String event = inputBean.getEvent();
        this.name = event + COLON + madeBy.getCode();
        setTxRef(txRef);
        this.comment = inputBean.getComment();
    }

    @JsonIgnore
    public long getId() {
        return id;
    }

    public void setWhat(LogWhat what) {
        this.auditWhat = (LogWhatData) what;
    }

    public FortressUser getWho() {
        return madeBy;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setTrackLog(LoggedRelationship trackLog){
        this.trackLog = trackLog;
    }

    @Override
    public void setPreviousLog(Log previousLog) {
        this.previousLog = (LogNode) previousLog;
    }

    @Override
    @JsonIgnore
    public Log getPreviousLog() {
        return previousLog;
    }
    @Transient
    private Map<String, Object> what;

    public void setTxRef(TxRef txRef) {
        this.txRef = txRef;
    }

    public ChangeEvent getEvent() {
        return event;
    }

    public boolean isCompressed() {
        return compressed;
    }

    @JsonIgnore
    public String getWhatStore() {
        return storage;
    }

    public void setWhatStore(String storage) {
        this.storage = storage;
    }

    @Override
    public void setEvent(ChangeEvent event) {
        this.event = (ChangeEventNode) event;

    }
    public boolean equals(Object other) {
        if (this == other) return true;

        if (id == null) return false;

        if (! (other instanceof LogNode)) return false;

        return id.equals(((LogNode) other).id);
    }

    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }

    @Override
    public void setCompressed(Boolean compressed) {
        this.compressed=compressed;
    }

    @Override
    @JsonIgnore
    public TrackLog getTrackLog() {
        return trackLog;
    }

    @Transient
    private byte[] dataBlock = null ;
    @Override
    @JsonIgnore
    public void setDataBlock(byte[] dataBlock) {
        this.dataBlock = dataBlock;

    }

    @Override
    @JsonIgnore
    public byte[] getDataBlock() {
        return dataBlock;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setTrackLog(TrackLog trackLog) {
        this.trackLog = (LoggedRelationship) trackLog;
    }


}
