/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.audit.model.TxRef;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.helper.CompressionResult;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 5:57 PM
 */
@NodeEntity(useShortNames = true)
public class AuditChangeNode implements AuditChange {
    public static final String COLON = ":";
    @GraphId
    private Long id;

    @RelatedTo(elementClass = FortressUserNode.class, type = "changed", direction = Direction.INCOMING, enforceTargetType = true)
    @Fetch
    private FortressUserNode madeBy;

    @RelatedTo(elementClass = TxRefNode.class, type = "txIncludes", direction = Direction.INCOMING, enforceTargetType = true)
    private TxRef txRef;

    static ObjectMapper objectMapper = new ObjectMapper();

    private String comment;
    private String event;

    private byte[] what;
    private boolean compressed = false;
    private String name;


    @RelatedTo(type = "previousChange", direction = Direction.INCOMING)
    private AuditChangeNode previousChange;

    @Fetch
    @RelatedToVia(type = "logged", direction = Direction.INCOMING)
    AuditLogRelationship auditLog;

    protected AuditChangeNode() {

    }

    public AuditChangeNode(FortressUser madeBy, AuditLogInputBean inputBean, TxRef txRef) {
        this();
        this.madeBy = (FortressUserNode) madeBy;

        String event = inputBean.getEvent();
        this.event = event;
        this.name = new StringBuilder().append(event).append(COLON).append(madeBy.getName()).toString();
        setTxRef(txRef);

        CompressionResult result = CompressionHelper.compress(inputBean.getWhat());
        this.what = result.getBytes();
        this.compressed = result.isCompressed();
        this.comment = inputBean.getComment();
    }

    @JsonIgnore
    public long getId() {
        return id;
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

    /**
     * @return the name of the event that caused this change
     */
    @JsonIgnore
    public String getName() {
        return name;
    }

    @JsonIgnore
    public String getJsonWhat() {
        return CompressionHelper.decompress(what, compressed);
    }

    public void setJsonWhat(String what) {
        CompressionResult result = CompressionHelper.compress(what);
        this.what = result.getAsBytes();
        this.compressed = result.getMethod() == CompressionResult.Method.GZIP;
    }

    @Override
    public void setPreviousChange(AuditChange previousChange) {
        this.previousChange = (AuditChangeNode) previousChange;
    }

    @Override
    public AuditChange getPreviousChange() {
        return previousChange;
    }

    @Override
    public AuditLog getAuditLog() {
        return auditLog;
    }

    @Transient
    private Map<String, Object> mWhat;

    public Map<String, Object> getWhat() {

        if (mWhat != null)
            return mWhat;
        try {
            mWhat = objectMapper.readValue(CompressionHelper.decompress(what, isCompressed()), Map.class);
        } catch (IOException e) {
            mWhat = new HashMap<>();
            mWhat.put("what", "{}");
        }
        return mWhat;
    }

    public void setTxRef(TxRef txRef) {
        this.txRef = txRef;
    }

    public String getEvent() {
        return event;
    }

    private boolean isCompressed() {
        return compressed;
    }
}
