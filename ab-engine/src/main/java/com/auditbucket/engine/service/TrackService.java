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

package com.auditbucket.engine.service;

import com.auditbucket.dao.TrackDao;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.KeyGenService;
import com.auditbucket.registration.service.SystemUserService;
import com.auditbucket.search.model.MetaSearchChange;
import com.auditbucket.search.model.SearchResult;
import com.auditbucket.track.bean.*;
import com.auditbucket.track.model.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Transactional services to support record and working with headers and logs
 * <p/>
 * User: Mike Holdsworth
 * Date: 8/04/13
 */
@Service
@Transactional
public class TrackService {
    private static final String EMPTY = "";
    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    TrackEventService trackEventService;

    @Autowired
    SystemUserService sysUserService;

    @Autowired
    TagTrackService tagTrackService;

    @Autowired
    SchemaService schemaService;

    @Autowired
    WhatService whatService;

    @Autowired
    TrackDao trackDao;

    @Autowired
    TagService tagService;

    @Autowired
    private SecurityHelper securityHelper;

    private Logger logger = LoggerFactory.getLogger(TrackService.class);

    @Autowired
    private KeyGenService keyGenService;

    public LogWhat getWhat(MetaHeader metaHeader, Log change) {
        return whatService.getWhat(metaHeader, change);
    }


    TxRef beginTransaction(Company company) {
        return beginTransaction(keyGenService.getUniqueKey(), company);
    }

    TxRef beginTransaction(String id, Company company) {
        return trackDao.beginTransaction(id, company);

    }

    public Map<String, Object> findByTXRef(String txRef) {
        TxRef tx = findTx(txRef);
        return (tx == null ? null : trackDao.findByTransaction(tx));
    }

    /**
     * Creates a fortressName specific metaHeader for the caller. FortressUserNode is automatically
     * created if it does not exist.
     *
     * @return unique primary key to be used for subsequent log calls
     */
    public TrackResultBean createHeader(Company company, Fortress fortress, MetaInputBean inputBean) {
        DocumentType documentType = schemaService.resolveDocType(fortress, inputBean.getDocumentType());

        MetaHeader ah = null;
        if ( inputBean.getMetaKey() != null ){
            ah = getHeader(company, inputBean.getMetaKey());
        }
        if ( ah == null && (inputBean.getCallerRef() != null && !inputBean.getCallerRef().equals(EMPTY)))
            ah = findByCallerRef(fortress, documentType, inputBean.getCallerRef());
        if (ah != null) {
            logger.debug("Existing metaHeader record found by Caller Ref [{}] found [{}]", inputBean.getCallerRef(), ah.getMetaKey());
            inputBean.setMetaKey(ah.getMetaKey());

            TrackResultBean arb = new TrackResultBean(ah);
            arb.setMetaInputBean(inputBean);
            arb.setWasDuplicate();
            arb.setLogInput(inputBean.getLog());
            // Could be rewriting tags
            // DAT-153 - move this to the end of the process?
            TrackLog trackLog = getLastLog(ah.getId());
            arb.setTags(tagTrackService.associateTags(company, ah, trackLog , inputBean.getTags()));
            return arb;
        }

        try {
            ah = makeHeader(inputBean, fortress, documentType);
        } catch (DatagioException e) {
            logger.error(e.getMessage());
            return new TrackResultBean("Error processing inputBean [{}]" + inputBean + ". Error " + e.getMessage());
        }
        TrackResultBean resultBean = new TrackResultBean(ah);
        resultBean.setMetaInputBean(inputBean);
        resultBean.setTags(tagTrackService.associateTags(company, resultBean.getMetaHeader(), null, inputBean.getTags()));

        resultBean.setLogInput(inputBean.getLog());
        return resultBean;

    }

    public MetaHeader makeHeader(MetaInputBean inputBean, Fortress fortress, DocumentType documentType) throws DatagioException {
        MetaHeader ah = trackDao.create(inputBean, fortress, documentType);
        if (ah.getId() == null)
            inputBean.setMetaKey("NT " + fortress.getFortressKey()); // We ain't tracking this

        inputBean.setMetaKey(ah.getMetaKey());
        logger.debug("MetaHeader created: id=[{}] key=[{}] for fortress [{}] callerKeyRef = [{}]", ah.getId(), ah.getMetaKey(), fortress.getCode(), ah.getCallerKeyRef());
        return ah;
    }

    /**
     * When you have no API key, find if authorised
     *
     * @param metaKey known GUID
     * @return header the caller is authorised to view
     */
    public MetaHeader getHeader(@NotEmpty String metaKey) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByLogin(userName);
        if (su == null)
            throw new SecurityException(userName + "Not authorised to retrieve headers");

        return getHeader(su.getCompany(), metaKey, false);
    }

    public MetaHeader getHeader(Company company, String metaKey) {
        if (company == null && metaKey != null)
            return getHeader(metaKey); // we can still find by authenticated user

        if (company == null)
            return null;

        return getHeader(company, metaKey, false);
    }

    public MetaHeader getHeader(Company company, @NotEmpty String headerKey, boolean inflate) {

        if (company == null)
            return getHeader(headerKey);
        MetaHeader ah = trackDao.findHeader(headerKey, inflate);
        if (ah == null || ah.getFortress() == null)
            return null;

        if (!(ah.getFortress().getCompany().getId().equals(company.getId())))
            throw new SecurityException("CompanyNode mismatch. [" + headerKey + "] working for [" + company.getName() + "] cannot write meta records for [" + ah.getFortress().getCompany().getName() + "]");
        return ah;
    }

    /**
     * Forces this service to refresh the header which may be stale
     *
     * @param company         who for
     * @param metaKey         header
     * @param trackResultBean input data to process
     * @return result of the operation
     * @throws DatagioException
     * @throws IOException
     */
    public LogResultBean writeLog(Company company, String metaKey, TrackResultBean trackResultBean) throws DatagioException, IOException {
        LogInputBean input = trackResultBean.getLog();
        MetaHeader metaHeader = null;

        // Incoming MetaHeader may be stale so refresh from this transactions view of it
        if (metaKey != null) {
            metaHeader = getHeader(company, metaKey); // Refresh the header with the latest version
        }
        if ( metaHeader == null && (trackResultBean.getMetaInputBean()!=null && trackResultBean.getMetaInputBean().isTrackSuppressed()))
            metaHeader = trackResultBean.getMetaHeader();

        logger.debug("writeLog - Received request to log for header=[{}]", metaHeader);
        if (metaHeader == null) {
            if (input.getMetaId() == null) {
                if (metaKey == null || metaKey.equals(EMPTY)) {
                    metaHeader = findByCallerRef(trackResultBean.getFortressName(), trackResultBean.getDocumentType(), trackResultBean.getCallerRef());
                    if (metaHeader != null)
                        input.setMetaKey(metaHeader.getMetaKey());
                } else
                    metaHeader = getHeader(metaKey); // true??
            } else
                metaHeader = getHeader(input.getMetaId());  // Only set internally by AuditBucket. Never rely on the caller
        }
        LogResultBean resultBean = new LogResultBean(input, metaHeader);
        if (metaHeader == null) {
            resultBean.setStatus(LogInputBean.LogStatus.NOT_FOUND);
            resultBean.setMessage("Unable to locate requested header");
            logger.debug(resultBean.getMessage());
            return resultBean;
        }
        logger.trace("looking for fortress user {}", metaHeader.getFortress());
        String fortressUser = (input.getFortressUser()!=null?input.getFortressUser():trackResultBean.getMetaInputBean().getFortressUser());
        FortressUser thisFortressUser = fortressService.getFortressUser(metaHeader.getFortress(), fortressUser, true);
        return createLog(metaHeader, input, thisFortressUser);
    }

    /**
     * Event log record for the supplied metaHeader from the supplied input
     *
     *
     * @param authorisedHeader metaHeader the caller is authorised to work with
     * @param input            trackLog details containing the data to log
     * @param thisFortressUser User name in calling system that is making the change
     * @return populated log information with any error messages
     */
    private LogResultBean createLog(MetaHeader authorisedHeader, LogInputBean input, FortressUser thisFortressUser) throws DatagioException, IOException {
        // Warning - making this private means it doesn't get a transaction!

        Fortress fortress = authorisedHeader.getFortress();

        // Transactions checks
        TxRef txRef = handleTxRef(input, fortress.getCompany());
        LogResultBean resultBean = new LogResultBean(input, authorisedHeader);
        //ToDo: May want to track a "View" event which would not change the What data.
        if (input.getWhat() == null || input.getWhat().isEmpty()) {
            resultBean.setStatus(LogInputBean.LogStatus.IGNORE);
            resultBean.setMessage("No 'what' information provided. Ignoring this request");
            return resultBean;
        }

        resultBean.setTxReference(txRef);

        TrackLog existingLog;
        existingLog = getLastLog(authorisedHeader);

        Boolean searchActive = fortress.isSearchActive();
        DateTime fortressWhen = (input.getWhen() == null ? new DateTime(DateTimeZone.forID(fortress.getTimeZone())) : new DateTime(input.getWhen()));

        if (existingLog != null) {
            logger.debug("createLog, existing log found {}", existingLog);
            boolean unchanged = whatService.isSame(authorisedHeader, existingLog.getLog(), input.getWhat());
            if (unchanged) {
                logger.trace("Ignoring a change we already have {}", input);
                resultBean.setStatus(LogInputBean.LogStatus.IGNORE);
                if (input.isForceReindex()) { // Caller is recreating the search index
                    resultBean.setStatus((LogInputBean.LogStatus.REINDEX));
                    resultBean.setLogToIndex(existingLog);
                    resultBean.setMessage("Ignoring a change we already have. Honouring request to re-index");
                } else
                    resultBean.setMessage("Ignoring a change we already have");
                return resultBean;
            }
            if (input.getEvent() == null) {
                input.setEvent(Log.UPDATE);
            }
            if (searchActive)
                authorisedHeader = waitOnInitialSearchResult(authorisedHeader);


        } else { // first ever log for the metaHeader
            logger.debug("createLog - first log created for the header");
            if (input.getEvent() == null) {
                input.setEvent(Log.CREATE);
            }
            //if (!metaHeader.getLastUser().getId().equals(thisFortressUser.getId())){
            authorisedHeader.setLastUser(thisFortressUser);
            authorisedHeader.setCreatedBy(thisFortressUser);
        }

        Log thisLog = trackDao.prepareLog(thisFortressUser, input, txRef, (existingLog != null ? existingLog.getLog() : null));
        // Prepares the change
        input.setChangeEvent(thisLog.getEvent());
        resultBean.setWhatLog(thisLog);
        resultBean.setMetaHeader(authorisedHeader);

        if (authorisedHeader.getId() == null)
            input.setStatus(LogInputBean.LogStatus.TRACK_ONLY);
        else
            input.setStatus(LogInputBean.LogStatus.OK);

        // This call also saves the header
        TrackLog newLog = trackDao.addLog(authorisedHeader, thisLog, fortressWhen, existingLog);

        resultBean.setSysWhen(newLog.getSysWhen());

        boolean moreRecent = (existingLog == null || existingLog.getFortressWhen().compareTo(newLog.getFortressWhen())<=0 );

        if (moreRecent && searchActive)
            resultBean.setLogToIndex(newLog);  // Notional log to index.

        return resultBean;

    }

    public Collection<MetaHeader> getHeaders(Fortress fortress, Long skipTo) {
        return trackDao.findHeaders(fortress.getId(), skipTo);
    }

    public Collection<MetaHeader> getHeaders(Fortress fortress, String docTypeName, Long skipTo) {
        DocumentType docType = schemaService.resolveDocType(fortress, docTypeName);
        return trackDao.findHeaders(fortress.getId(), docType.getName(), skipTo);
    }

    private MetaHeader waitOnInitialSearchResult(MetaHeader metaHeader) {

        if (metaHeader.isSearchSuppressed() || metaHeader.getSearchKey() != null)
            return metaHeader; // Nothing to wait for as we're suppressing searches for this metaHeader

        int timeOut = 100;
        int i = 0;

        while (metaHeader.getSearchKey() == null && i < timeOut) {
            i++;
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
            metaHeader = getHeader(metaHeader.getId());
        }
        if (metaHeader.getSearchKey() == null)
            logger.error("Timeout waiting for the initial search document to be created [{}]", metaHeader.getMetaKey());
        return metaHeader;

    }

    MetaHeader getHeader(Long id) {
        return trackDao.getHeader(id);
    }

    private TxRef handleTxRef(LogInputBean input, Company company) {
        TxRef txRef = null;
        if (input.isTransactional()) {
            if (input.getTxRef() == null) {
                txRef = beginTransaction(company);
                input.setTxRef(txRef.getName());
            } else {
                txRef = beginTransaction(input.getTxRef(), company);
            }
        }

        return txRef;
    }

    public TxRef findTx(String txRef) {
        return findTx(txRef, false);
    }

    TxRef findTx(String txRef, boolean fetchHeaders) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByLogin(userName);

        if (su == null)
            throw new SecurityException("Not authorised");
        TxRef tx = trackDao.findTxTag(txRef, su.getCompany(), fetchHeaders);
        if (tx == null)
            return null;
        return tx;
    }

    public Set<MetaHeader> findTxHeaders(String txName) {
        TxRef txRef = findTx(txName);
        if (txRef == null)
            return null;
        return trackDao.findHeadersByTxRef(txRef.getId());
    }

    public void updateHeader(MetaHeader metaHeader) {
        trackDao.save(metaHeader);
    }

    public TrackLog getLastLog(String metaKey) throws DatagioException {
        MetaHeader header = getValidHeader(metaKey);
        return getLastLog(header.getId());

    }

    public TrackLog getLastLog(Company company, String metaKey) throws DatagioException {
        MetaHeader header = getHeader(company, metaKey);
        return getLastLog(header);
    }

    public TrackLog getLastLog(MetaHeader metaHeader) throws DatagioException {
        if (metaHeader == null || metaHeader.getId() == null)
            return null;
        //logger.debug("Getting lastLog MetaID [{}]", metaHeader.getId());
        return trackDao.getLastLog(metaHeader.getId());
    }

    public TrackLog getLastLog(Long headerId) {
        return trackDao.getLastLog(headerId);
    }

    public Set<TrackLog> getLogs(Long headerId) {
        return trackDao.getLogs(headerId);
    }

    public Set<TrackLog> getLogs(Company company, String headerKey) throws DatagioException {
        MetaHeader metaHeader = getHeader(company, headerKey);
        return trackDao.getLogs(metaHeader.getId());
    }

    public Set<TrackLog> getLogs(String headerKey, Date from, Date to) throws DatagioException {
        MetaHeader metaHeader = getValidHeader(headerKey);
        return getLogs(metaHeader, from, to);
    }

    Set<TrackLog> getLogs(MetaHeader metaHeader, Date from, Date to) {
        return trackDao.getLogs(metaHeader.getId(), from, to);
    }

    /**
     * blocking call. This will not update the search store. For that call the
     * function in the MediationFacade
     *
     *
     * @param company
     * @param headerKey UID of the Header
     * @return MetaSearchChange the search change to index, or null if there are no logs
     * @throws IOException
     */
    public MetaSearchChange cancelLastLogSync(Company company, String headerKey) throws IOException, DatagioException {
        AsyncResult<MetaSearchChange> futureHeader = cancelLastLog(company, headerKey );
        return futureHeader.get();
    }

    /**
     * This could be used toa assist in compensating transactions to roll back the last change
     * if the caller decides a rollback is required after the log has been written.
     * If there are no Log records left, then the metaHeader will also be removed and the
     * AB headerKey will be forever invalid.
     *
     *
     *
     * @param company
     * @param headerKey UID of the metaHeader
     * @return Future<MetaSearchChange> search change to index, or null if there are no logs
     */
    @Async
    public AsyncResult<MetaSearchChange> cancelLastLog(Company company, String headerKey) throws IOException, DatagioException {
        MetaHeader metaHeader = getValidHeader(headerKey, true);
        TrackLog existingLog = getLastLog(metaHeader.getId());
        if (existingLog == null)
            return null;

        Log currentLog = existingLog.getLog();
        Log fromLog = currentLog.getPreviousLog();
        String searchKey = metaHeader.getSearchKey();

        if (fromLog != null) {
            trackDao.fetch(fromLog);
            TrackLog newTrack = trackDao.getLog(fromLog.getTrackLog().getId());
            // Anything that was removed against the previous log has to be added back to the metaheader
            //Set<TrackTag> deletedTags = getLogTags(company, fromLog);
            metaHeader.setLastChange(fromLog);
            metaHeader.setLastUser(fortressService.getFortressUser(metaHeader.getFortress(), fromLog.getWho().getCode()));
            metaHeader.setFortressLastWhen(newTrack.getFortressWhen());
            metaHeader = trackDao.save(metaHeader);
            tagTrackService.moveTags(company, fromLog, metaHeader );
            trackDao.delete(currentLog);

        } else {
            // No changes left, there is now just a header
            // ToDo: What to to with the header? Delete it? Store the "canceled By" User? Assign the log to a Cancelled RLX?
            // Delete from ElasticSearch??
            metaHeader.setLastUser(fortressService.getFortressUser(metaHeader.getFortress(), metaHeader.getCreatedBy().getCode()));
            metaHeader.setFortressLastWhen(0l);
            metaHeader.setSearchKey(null);
            metaHeader = trackDao.save(metaHeader);
            trackDao.delete(currentLog);
        }
        whatService.delete(metaHeader, currentLog); // ToDo: Move to mediation facade
        MetaSearchChange searchDocument = null;
        if (fromLog == null) {
            // Nothing to index, no changes left so we're done
            searchDocument = new MetaSearchChange(metaHeader);
            searchDocument.setDelete(true);
            searchDocument.setSearchKey(searchKey);
            return new AsyncResult<>(searchDocument);
        }

        // Sync the update to ab-search.
        if (metaHeader.getFortress().isSearchActive() && !metaHeader.isSearchSuppressed()) {
            // Update against the MetaHeader only by re-indexing the search document
            HashMap<String, Object> priorWhat = (HashMap<String, Object>) whatService.getWhat(metaHeader, fromLog).getWhat();

            searchDocument = new MetaSearchChange(metaHeader, priorWhat, fromLog.getEvent().getCode(), new DateTime(fromLog.getTrackLog().getFortressWhen()));
            searchDocument.setTags(tagTrackService.findTrackTags(metaHeader));
            searchDocument.setReplyRequired(false);
            searchDocument.setForceReindex(true);
        }
        return new AsyncResult<>(searchDocument);
    }

    /**
     * counts the number of logs that exist for the given metaHeader
     *
     * @param headerKey GUID
     * @return count
     */
    public int getLogCount(String headerKey) throws DatagioException {
        MetaHeader metaHeader = getValidHeader(headerKey);
        return trackDao.getLogs(metaHeader.getId()).size();

    }

    private MetaHeader getValidHeader(String headerKey) throws DatagioException {
        return getValidHeader(headerKey, false);
    }

    private MetaHeader getValidHeader(String headerKey, boolean inflate) throws DatagioException {
        MetaHeader header = trackDao.findHeader(headerKey, inflate);
        if (header == null) {
            throw new DatagioException("No metaHeader for [" + headerKey + "]");
        }
        String userName = securityHelper.getLoggedInUser();
        SystemUser sysUser = sysUserService.findByLogin(userName);

        if (!header.getFortress().getCompany().getId().equals(sysUser.getCompany().getId())) {
            throw new SecurityException("Not authorised to work with this meta data");
        }
        return header;

    }

    public MetaHeader findByCallerRef(String fortress, String documentType, String callerRef) {
        Fortress iFortress = fortressService.findByName(fortress);
        if (iFortress == null)
            return null;

        return findByCallerRef(iFortress, documentType, callerRef);
    }

    public MetaHeader findByCallerRefFull(Long fortressId, String documentType, String callerRef) {
        Fortress fortress = fortressService.getFortress(fortressId);
        return findByCallerRefFull(fortress, documentType, callerRef);

    }

    /**
     * \
     * inflates the search result with dependencies populated
     *
     * @param fortress     System
     * @param documentType Class of doc
     * @param callerRef    fortressName PK
     * @return inflated header
     */
    public MetaHeader findByCallerRefFull(Fortress fortress, String documentType, String callerRef) {
        return findByCallerRef(fortress, documentType, callerRef);
    }

    /**
     * Locates all the MetaHeaders irrespective of the document type. Use this when you know that that metaKey is
     * unique for the entire fortressName
     *
     * @param company      Company you are authorised to work with
     * @param fortressName Fortress to restrict the search to
     * @param callerRef    key to locate
     * @return metaHeaders
     */
    public Iterable<MetaHeader> findByCallerRef(Company company, String fortressName, String callerRef) {
        Fortress fortress = fortressService.findByName(company, fortressName);
        return findByCallerRef(fortress, callerRef);
    }

    public Collection<MetaHeader> findByCallerRef(Fortress fortress, String callerRef) {
        return trackDao.findByCallerRef(fortress.getId(), callerRef.trim());
    }


    public MetaHeader findByCallerRef(Fortress fortress, String documentType, String callerRef) {

        DocumentType doc = schemaService.resolveDocType(fortress, documentType, false);
        if (doc == null){
            logger.debug ("Unable to find document for callerRef {}, {}, {}", fortress, documentType, callerRef);
            return null;
        }
        return findByCallerRef(fortress, doc, callerRef);

    }

    /**
     * @param fortress     owning system
     * @param documentType class of document
     * @param callerRef    fortressName primary key
     * @return LogResultBean or NULL.
     */
    public MetaHeader findByCallerRef(Fortress fortress, DocumentType documentType, String callerRef) {
        return trackDao.findByCallerRef(fortress.getId(), documentType.getId(), callerRef.trim());
    }


    public TrackedSummaryBean getMetaSummary(Company company, String metaKey) throws DatagioException {
        MetaHeader header = getHeader(company, metaKey, true);
        if (header == null)
            throw new DatagioException("Invalid Meta Key [" + metaKey + "]");
        Set<TrackLog> changes = getLogs(header.getId());
        Set<TrackTag> tags = tagTrackService.findTrackTags(company, header);
        return new TrackedSummaryBean(header, changes, tags);
    }


    public LogDetailBean getFullDetail(String metaKey, Long logId) {
        Company company = securityHelper.getCompany();
        return getFullDetail(company, metaKey, logId);
    }

    public LogDetailBean getFullDetail(Company company, String metaKey, Long logId) {
        MetaHeader metaHeader = getHeader(company, metaKey, true);
        if (metaHeader == null)
            return null;

        TrackLog log = trackDao.getLog(logId);
        trackDao.fetch(log.getLog());
        LogWhat what = whatService.getWhat(metaHeader, log.getLog());
        log.getLog().setWhat(what);
        return new LogDetailBean(log, what);
    }

    public TrackLog getLogForHeader(MetaHeader header, Long logId) {
        if (header != null) {

            TrackLog log = trackDao.getLog(logId);
            if (!log.getMetaHeader().getId().equals(header.getId()))
                return null;

            trackDao.fetch(log.getLog());
            return log;
        }
        return null;
    }

    public Iterable<TrackResultBean> createHeaders(Company company, Fortress fortress, Iterable<MetaInputBean> inputBeans) {
        Collection<TrackResultBean> arb = new CopyOnWriteArrayList<>();
        for (MetaInputBean inputBean : inputBeans) {
            logger.trace("Batch Processing metaKey=[{}], documentType=[{}]", inputBean.getCallerRef(), inputBean.getDocumentType());
            arb.add(createHeader(company, fortress, inputBean));
        }
        return arb;

    }

    /**
     * Cross references to meta headers to create a link
     *
     * @param company          validated company the caller is authorised to work with
     * @param metaKey          source from which a xref will be created
     * @param xRef             target for the xref
     * @param relationshipName name of the relationship
     */
    public Collection<String> crossReference(Company company, String metaKey, Collection<String> xRef, String relationshipName) throws DatagioException {
        MetaHeader header = getHeader(company, metaKey);
        if (header == null) {
            throw new DatagioException("Unable to find the Meta Header [" + metaKey + "]. Perhaps it has not been processed yet?");
        }
        Collection<MetaHeader> targets = new ArrayList<>();
        Collection<String> ignored = new ArrayList<>();
        for (String next : xRef) {
            MetaHeader m = getHeader(company, next);
            if (m != null) {
                targets.add(m);
            } else {
                ignored.add(next);
                //logger.info ("Unable to find MetaKey ["+metaHeader+"]. Skipping");
            }
        }
        trackDao.crossReference(header, targets, relationshipName);
        return ignored;
    }

    public Map<String, Collection<MetaHeader>> getCrossReference(Company company, String metaKey, String xRefName) throws DatagioException {
        MetaHeader header = getHeader(company, metaKey);
        if (header == null) {
            throw new DatagioException("Unable to find the Meta Header [" + metaKey + "]. Perhaps it has not been processed yet?");
        }

        return trackDao.getCrossReference(company, header, xRefName);
    }

    public Map<String, Collection<MetaHeader>> getCrossReference(Company company, String fortressName, String callerRef, String xRefName) throws DatagioException {
        Fortress fortress = fortressService.findByName(company, fortressName);

        MetaHeader source = trackDao.findByCallerRefUnique(fortress.getId(), callerRef);
        if (source == null) {
            throw new DatagioException("Unable to find the Meta Header [" + callerRef + "]");
        }

        return trackDao.getCrossReference(company, source, xRefName);
    }

    public List<MetaKey> crossReferenceByCallerRef(Company company, MetaKey sourceKey, Collection<MetaKey> targetKeys, String xRefName) throws DatagioException {
        Fortress f = fortressService.findByName(company, sourceKey.getFortressName());
        MetaHeader header ;
        if ( sourceKey.getDocumentType()==null || sourceKey.getDocumentType().equals("*"))
            header = trackDao.findByCallerRefUnique(f.getId(), sourceKey.getCallerRef());
        else {
            DocumentType document = schemaService.resolveDocType(f, sourceKey.getDocumentType(), false);
            header = trackDao.findByCallerRef(f.getId(), document.getId(), sourceKey.getCallerRef());
        }
        if (header == null)
            throw new DatagioException("Unable to locate the MetaHeader for MetaKey [" + sourceKey + "]");

        //16051954
        Collection<MetaHeader> targets = new ArrayList<>();
        List<MetaKey> ignored = new ArrayList<>();

        for (MetaKey metaKey : targetKeys) {
            int count = 1;

            Collection<MetaHeader> metaHeaders ;
            if ( metaKey.getDocumentType().equals("*"))
                metaHeaders= findByCallerRef(f, metaKey.getCallerRef() );
            else {
                MetaHeader mh =findByCallerRef(fortressService.findByName(company, metaKey.getFortressName()), metaKey.getDocumentType(), metaKey.getCallerRef());
                if ( mh == null ) {
                    ignored.add(metaKey);
                    metaHeaders = null;

                } else {
                    Collection<MetaHeader> array= new ArrayList<>();
                    array.add(mh);
                    metaHeaders = array;
                }
            }
            if ( metaHeaders!=null ){
                for (MetaHeader metaHeader : metaHeaders) {
                    if (count > 1 || count == 0)
                        ignored.add(metaKey);
                    else
                        targets.add(metaHeader);
                    count++;
                }
            }

        }
        if ( !targets.isEmpty() )
            trackDao.crossReference(header, targets, xRefName);
        return ignored;
    }

    public Map<String,MetaHeader> getHeaders(Company company, Collection<String> metaKeys) {
        return trackDao.findHeaders(company, metaKeys);
    }

    public void purge(Fortress fortress) {

        trackDao.purgeTagRelationships(fortress);

        trackDao.purgeFortressLogs(fortress);
        trackDao.purgePeopleRelationships(fortress);
        schemaService.purge(fortress);
        //trackDao.purgeFortressDocuments(fortress);
        trackDao.purgeHeaders(fortress);

    }

    public void saveMetaData(SearchResult searchResult, Long metaId) {
        // Only exists and is public because we need the transaction
        MetaHeader header;
        try {
            header = getHeader(metaId); // Happens during development when Graph is cleared down and incoming search results are on the q
        } catch (DataRetrievalFailureException e) {
            logger.error("Unable to locate header for metaId {} in order to handle the search metaKey. Ignoring.", metaId);
            return;
        }

        if (header == null) {
            logger.error("metaKey could not be found for [{}]", searchResult);
            return;
        }

        if (header.getSearchKey() == null) {
            header.setSearchKey(searchResult.getSearchKey());
            trackDao.save(header);
            logger.trace("Updating Header{} search searchResult =[{}]", header.getMetaKey(), searchResult);
        }

        if (searchResult.getLogId() == null) {
            // Indexing header meta data only
            return;
        }
        TrackLog trackLog;
        // The change has been indexed
        try {
            trackLog = trackDao.getLog(searchResult.getLogId());
            if (trackLog == null) {
                logger.error("Illegal node requested from handleSearchResult [{}]", searchResult.getLogId());
                return;
            }
        } catch (DataRetrievalFailureException e) {
            logger.error("Unable to locate track log {} for metaId {} in order to handle the search metaKey. Ignoring.", searchResult.getLogId(), header.getId());
            return;
        }

        // Another thread may have processed this so save an update
        if (!trackLog.isIndexed()) {
            // We need to know that the change we requested to index has been indexed.
            logger.trace("Updating index status for {}", trackLog);
            trackLog.setIsIndexed();
            trackDao.save(trackLog);

        } else {
            logger.trace("Skipping {} as it is already indexed", trackLog);
        }
    }

    public Set<TrackTag> getLastLogTags(Company company, String metaKey) throws  DatagioException{
        TrackLog lastLog = getLastLog(company, metaKey);
        if ( lastLog == null )
            return new HashSet<>();

        return getLogTags(company, lastLog.getLog());
    }

    private Set<TrackTag> getLogTags(Company company, Log log) {
        return tagTrackService.findLogTags(company, log);

    }

    public TrackLog getLog(Company company, String metaKey, long logId) throws DatagioException {
        MetaHeader header = getHeader(company, metaKey);
        TrackLog log = trackDao.getLog(logId);

        if ( log == null )
            throw new DatagioException(String.format("Invalid logId %d for %s ", logId, metaKey));

        if (!log.getMetaHeader().getId().equals(header.getId()))
            throw new DatagioException(String.format("Invalid logId %d for %s ", logId, metaKey));
        return log;
    }

    public Set<TrackTag> getLogTags(Company company, TrackLog tl) {
        return getLogTags(company, tl.getLog());  //To change body of created methods use File | Settings | File Templates.
    }
}
