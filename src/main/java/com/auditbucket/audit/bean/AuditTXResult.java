package com.auditbucket.audit.bean;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;


/**
 * User: mike
 * Date: 16/06/13
 * Time: 6:12 PM
 */
public class AuditTXResult {

    private String auditKey;
    private String fortressName;
    private String fortressKey;
    private String dataType;
    private String clientRef;
    private Long lastSystemChange;

    private IAuditLog auditLog;

    public AuditTXResult() {
    }

    public AuditTXResult(IAuditLog log) {
        this(null, log);
    }

    public AuditTXResult(IAuditHeader header, IAuditLog log) {
        if (header == null)
            header = log.getHeader();
        this.auditKey = header.getAuditKey();
        this.dataType = header.getDataType();
        this.clientRef = header.getClientRef();
        this.fortressName = header.getFortress().getName();
        this.fortressKey = header.getFortress().getFortressKey();
        this.lastSystemChange = header.getLastUpdated();
        this.auditLog = log;
    }


    public Object getAuditLog() {
        return auditLog;
    }

    public String getAuditKey() {
        return auditKey;
    }

    public String getFortressName() {
        return fortressName;
    }

    public String getFortressKey() {
        return fortressKey;
    }

    public String getDataType() {
        return dataType;
    }

    public long getLastSystemChange() {
        return lastSystemChange;
    }

    public String getClientRef() {
        return clientRef;
    }
}