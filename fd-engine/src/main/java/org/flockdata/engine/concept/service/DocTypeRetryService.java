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

package org.flockdata.engine.concept.service;

import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.model.DocumentType;
import org.flockdata.model.FortressSegment;
import org.flockdata.track.bean.EntityInputBean;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.transaction.HeuristicRollbackException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 2/12/14
 * Time: 7:48 AM
 */
@Service
@Async("fd-engine")
public class DocTypeRetryService {

    private final ConceptService conceptService;

    @Autowired
    public DocTypeRetryService(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    /**
     * Creates document types for the input beans if they do not exist
     * Handles linked entities which may be part of the EntityInputBean.
     * Ensures the segment also exists for the linked Entities
     *
     * @param segment   all InputBeans are deemed to belong to this segment
     * @param inputBeans collection of Entities from which to find DocumentTypes and linked Entities
     * @return Collection of DocumentType objects that were created
     */
    @Retryable(include = {TransactionFailureException.class, HeuristicRollbackException.class, DataRetrievalFailureException.class, InvalidDataAccessResourceUsageException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class}, maxAttempts = 20, backoff = @Backoff(delay = 150, maxDelay = 500))
    public Future<Collection<DocumentType>> createDocTypes(FortressSegment segment, List<EntityInputBean> inputBeans) {

        return new AsyncResult<>(conceptService.makeDocTypes(segment, inputBeans));
    }
}
