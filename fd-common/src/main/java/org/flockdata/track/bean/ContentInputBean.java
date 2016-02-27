/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.helper.FlockException;
import org.flockdata.model.ChangeEvent;
import org.flockdata.model.EntityContent;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Tracked in the KV Store this object tracks meta data and the actual content being tracked
 * <p>
 * User: Mike Holdsworth
 * Date: 8/05/13
 * Time: 7:41 PM
 */
public class ContentInputBean implements EntityContent, Serializable {
    private LogStatus fdStatus;
    private String fdMessage;
    private Boolean isTransactional = false;

    private double pVer =1d;

    // Use either key or CallerRef strategy
    // Required if not updating via a Entity
    private String key;

    // For tracking by the Callers Reference
    private String documentType;
    private String code;
    private String fortress;
    private String txRef;
    private String comment;
    private String fortressUser;
    private String event;
    private Date when;
    private Map<String, Object> data = null;
    private String attachment = null;
    private boolean forceReindex;
    private boolean status;
    private String contentType = "json";
    private String fileName;


    public ContentInputBean() {
    }

    public ContentInputBean(String fortressUser, DateTime fortressWhen) {
        this();
        this.fortressUser = fortressUser;
        if (fortressWhen != null)
            this.when = fortressWhen.toDate();
    }

    /**
     * @param fortressUser -user name recognisable in the fortress
     * @param key      -guid
     * @param fortressWhen -fortress view of DateTime
     * @param data         -escaped JSON
     */
    public ContentInputBean(String fortressUser, String key, DateTime fortressWhen, Map<String, Object> data, Boolean isTransactional) throws FlockException {
        this(fortressUser, fortressWhen);
        this.key = key;
        setTransactional(isTransactional);
        setData(data);
    }

    /**
     * @param fortressUser -user name recognisable in the fortress
     * @param key      -guid
     * @param fortressWhen -fortress view of DateTime
     * @param data         - Map
     * @param event        -how the caller would like to catalog this change (create, update etc)
     */
    public ContentInputBean(String fortressUser, String key, DateTime fortressWhen, Map<String, Object> data, String event) throws FlockException {
        this(fortressUser, key, fortressWhen, data);
        this.event = event;
    }

    public ContentInputBean(String fortressUser, String key, DateTime fortressWhen, Map<String, Object> data, String event, String txName) throws FlockException {
        this(fortressUser, key, fortressWhen, data, event);
        this.setTxRef(txName);
    }

    public ContentInputBean(Map<String, Object> result) {
        this.data = result;
    }

    public ContentInputBean(String fortressUser, DateTime when, Map<String, Object> data) throws FlockException {
        this(fortressUser, null, when, data);
    }

    public ContentInputBean(String fortressUser, String key, DateTime when, Map<String, Object> data) throws FlockException {
        this(fortressUser, key, when, data, false);
    }

    public ContentInputBean(String user, Map<String, Object> data) {
        this.fortressUser = user;
        this.data = data;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getFortressUser() {
        return fortressUser;
    }

    public void setFortressUser(String fortressUser) {
        this.fortressUser = fortressUser;
    }

    public Date getWhen() {
        return when;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    public Map<String, Object> getData() {
        return data;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public void setData(Map<String, Object> data) throws FlockException {
        this.data = data;

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getComment() {
        return comment;
    }

    public String getCode() {
        return code;
    }

    /**
     * The caller ref must be unique for the Document Type in the Fortress
     * If you do not have a unique ref, then you must pass the AuditKey instead.
     *
     * @param code fortress primary key
     */
    public void setCode(String code) {
        this.code = code;
    }

    public void setCallerRef(String fortress, String documentType, String callerRef) {
        this.code = callerRef;
        this.documentType = documentType;
        this.fortress = fortress;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getTxRef() {
        return txRef;
    }

    /**
     * This LogInput will be tracked against the supplied TxRef
     *
     * @param txRef TX Key Reference to use
     */
    public void setTxRef(String txRef) {
        if (txRef != null && txRef.equals(""))
            this.txRef = null;
        else {
            this.txRef = txRef;
            setTransactional(true);
        }
    }

    public void setFdMessage(String fdMessage) {
        this.fdMessage = fdMessage;

    }

    /**
     * event sourcing type functionality. If true, AB will create a transaction identifier
     * that the caller can supply in subsequent updates
     *
     * @param isTransactional track
     */
    public void setTransactional(Boolean isTransactional) {
        this.isTransactional = isTransactional;
    }

    public Boolean isTransactional() {
        return isTransactional;
    }

    public void setStatus(LogStatus logStatus) {
        this.fdStatus = logStatus;

    }

    public String getDocumentType() {
        return documentType;
    }

    public String getFortress() {
        return fortress;
    }

    public boolean isForceReindex() {
        return forceReindex;
    }

    public void setForceReindex(boolean forceReindex) {
        this.forceReindex = forceReindex;
    }

    private transient ChangeEvent changeEvent;

    @JsonIgnore
    public ChangeEvent getChangeEvent() {
        return changeEvent;
    }

    public void setChangeEvent(ChangeEvent changeEvent) {
        this.changeEvent = changeEvent;
    }

    public LogStatus getStatus() {
        return fdStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getAttachment() {
        return attachment;
    }

    /**
     * @param attachment  base64 encoded bytes
     * @param contentType valid  HTTP MediaType
     * @param fileName    How you would like this file to be known if it's downloaded
     */
    public void setAttachment(String attachment, String contentType, String fileName) {
        this.attachment = attachment;
        this.contentType = contentType;
        this.fileName = fileName;
    }

    @JsonIgnore
    public boolean hasData() {
        boolean json = getData() != null && !getData().isEmpty();
        boolean attachment = getAttachment() != null;
        return json || attachment;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType.toLowerCase();
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * TRACK_ONLY == Don't store in the graph but do write to the search service
     */
    public enum LogStatus {
        IGNORE, OK, FORBIDDEN, NOT_FOUND, REINDEX, ILLEGAL_ARGUMENT, TRACK_ONLY
    }

    /**
     *
     * @return version of the contentProfile used to create this payload
     */
    public Double getpVer() {
        return pVer;
    }

    @Override
    public String toString() {
        return "LogInputBean{" +
                "event='" + event + '\'' +
                ", documentType='" + documentType + '\'' +
                ", code='" + code + '\'' +
                ", key='" + key + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentInputBean)) return false;

        ContentInputBean that = (ContentInputBean) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null) return false;
        if (documentType != null ? !documentType.equals(that.documentType) : that.documentType != null) return false;
        if (fortress != null ? !fortress.equals(that.fortress) : that.fortress != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (txRef != null ? !txRef.equals(that.txRef) : that.txRef != null) return false;
        return !(when != null ? !when.equals(that.when) : that.when != null);

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (fortress != null ? fortress.hashCode() : 0);
        result = 31 * result + (txRef != null ? txRef.hashCode() : 0);
        result = 31 * result + (when != null ? when.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        return result;
    }
}
