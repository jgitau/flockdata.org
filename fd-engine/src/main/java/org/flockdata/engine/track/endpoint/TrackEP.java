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

package org.flockdata.engine.track.endpoint;

import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.EntityLog;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.*;
import org.flockdata.track.service.EntityService;
import org.flockdata.track.service.MediationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Write operations for Entities and Tags
 *
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/track")
public class TrackEP {
    @Autowired
    private EntityService entityService;

    @Autowired
    private MediationFacade mediationFacade;


    @RequestMapping(value = "/", consumes = "application/json", produces = "application/json", method = RequestMethod.PUT)
    public Collection<TrackRequestResult> trackEntities(@RequestBody List<EntityInputBean> inputBeans,
                                                        HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);

        return mediationFacade.trackEntities(company, inputBeans);
    }

    /**
     * Tracks an entity
     *
     * @param input Entity input
     * @return TrackResultBean
     * @throws org.flockdata.helper.FlockException
     */
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    public
    ResponseEntity<TrackRequestResult> trackEntity(@RequestBody EntityInputBean input,
                                                HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        TrackResultBean trackResultBean;
        trackResultBean = mediationFacade.trackEntity(company, input);

        if ( trackResultBean.entityExists())
            if ( trackResultBean.getCurrentLog()!= null && trackResultBean.isLogIgnored())
                return new ResponseEntity<>(new TrackRequestResult(trackResultBean), HttpStatus.NOT_MODIFIED);

        trackResultBean.addServiceMessage("OK");
        return new ResponseEntity<>(new TrackRequestResult(trackResultBean), HttpStatus.CREATED);

    }


    @RequestMapping(value = "/log/", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public ResponseEntity<EntityLog> trackLog(@RequestBody ContentInputBean input ,
                                              HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);

        TrackResultBean resultBean = mediationFacade.trackLog(company, input);
        ContentInputBean.LogStatus ls = resultBean.getLogStatus();
        if (ls.equals(ContentInputBean.LogStatus.FORBIDDEN))
            return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.FORBIDDEN);
        else if (ls.equals(ContentInputBean.LogStatus.NOT_FOUND)) {
            throw new NotFoundException("Unable to locate the requested key");
        } else if (ls.equals(ContentInputBean.LogStatus.IGNORE)) {
            input.setFdMessage("Ignoring request to change as the 'data' has not changed");
            return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.NOT_MODIFIED);
        } else if (ls.equals(ContentInputBean.LogStatus.ILLEGAL_ARGUMENT)) {
            return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.CREATED);
    }


    @RequestMapping(value = "/{fortress}/{recordType}/{code}", produces = "application/json", method = RequestMethod.PUT)
    public ResponseEntity<TrackRequestResult> trackByClientRef(@RequestBody EntityInputBean input,
                                                            @PathVariable("fortress") String fortress,
                                                            @PathVariable("recordType") String recordType,
                                                            @PathVariable("code") String code,
                                                            HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        TrackResultBean trackResultBean;
        input.setFortress(new FortressInputBean(fortress));
        input.setDocumentType(new DocumentTypeInputBean(recordType));
        input.setCode(code);
        input.setKey(null);
        trackResultBean = mediationFacade.trackEntity(company, input);
        trackResultBean.addServiceMessage("OK");
        return new ResponseEntity<>(new TrackRequestResult(trackResultBean), HttpStatus.OK);

    }

    @RequestMapping(value = "/{key}/{xRefName}/link", produces = "application/json", method = RequestMethod.POST)
    public @ResponseBody Collection<String> crossReference(@PathVariable("key") String key, Collection<String> keys, @PathVariable("xRefName") String relationshipName,
                                                           HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return entityService.crossReference(company, key, keys, relationshipName);
    }

    /**
     * Looks across all document types for the caller ref within the fortress. If the code is not unique or does not
     * exist then an exception is thown.
     *
     * @param fortressName application
     * @param code    source
     * @param entities   targets
     * @param xRefName     name of the cross reference
     * @return unresolvable caller references
     * @throws org.flockdata.helper.FlockException if not exactly one Entity for the code in the fortress
     */
    @RequestMapping(value = "/{fortress}/all/{code}/{xRefName}/link", produces = "application/json", method = RequestMethod.POST)
    public @ResponseBody  Collection<EntityKeyBean> crossReferenceEntity(@PathVariable("fortress") String fortressName,
                                                                   @PathVariable("code") String code,
                                                                   @RequestBody Collection<EntityKeyBean> entities,
                                                                   @PathVariable("xRefName") String xRefName,
                                                                   HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return entityService.linkEntities(company, new EntityKeyBean("*", fortressName, code), entities, xRefName);
    }


    @RequestMapping(value = "/link", produces = "application/json", method = RequestMethod.POST)
    public @ResponseBody Collection<EntityToEntityLinkInput> linkEntities(@RequestBody List<EntityToEntityLinkInput> entityToEntityLinkInputs,
                                                                          HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        return entityService.linkEntities(company, entityToEntityLinkInputs);
    }



}