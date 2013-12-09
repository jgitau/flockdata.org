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

package com.auditbucket.audit.model;

import org.joda.time.DateTime;

import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 21/04/13
 * Time: 7:44 PM
 */
public interface SearchChange {
    /**
     * @return search keys unique document identifier
     */
    public String getSearchKey();

    public void setSearchKey(String parent);

    /**
     * primary key of the Audit record that this document belongs to
     *
     * @return GUID
     */
    String getAuditKey();

    public void setWhat(Map<String, Object> what);

    public Map<String, Object> getWhat();

    public Map<String, Object> getTagValues();

    public void setTags(Set<AuditTag> tagSet);

    /**
     * @return who made this change
     */
    public String getWho();

    public Long getSysWhen();

    public String getFortressName();

    public String getCompanyName();

    public String getIndexName();

    /**
     * @return date this was created in the owning fortress
     */
    public Long getWhen();

    /**
     * when this log was created in the Fortress
     *
     * @param date date
     */
    void setWhen(DateTime date);

    void setWho(String name);

    String getDocumentType();

    String getCallerRef();

    String getEvent();

    public void setSysWhen(Long sysWhen);

    void setLogId(Long id);

    Long getLogId();


    Long getAuditId();

    Long getCreatedDate();

    void setDescription(String description);

    String getDescription();

    /**
     * Hint to determine if a reply from the search service is expected
     * by the caller
     * <p/>
     * default to true
     */

    public void setReplyRequired(boolean required);

    boolean isReplyRequired();

}
