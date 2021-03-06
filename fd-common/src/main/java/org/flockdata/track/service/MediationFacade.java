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

package org.flockdata.track.service;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Non transactional coordinator providing mediation functionality between services
 * <p>
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
public interface MediationFacade {

    Collection<TrackRequestResult> trackEntities(Collection<EntityInputBean> inputBeans, String apiKey) throws FlockException, InterruptedException, ExecutionException, IOException;

    Collection<TrackRequestResult> trackEntities(Company company, Collection<EntityInputBean> inputBeans) throws FlockException, InterruptedException, ExecutionException, IOException;

    TagResultBean createTag(Company company, TagInputBean tagInput) throws FlockException, ExecutionException, InterruptedException;

    Collection<TagResultBean> createTags(String apiKey, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException;

    Collection<TagResultBean> createTags(Company company, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException;

    Collection<TrackResultBean> trackEntities(FortressSegment segment, List<EntityInputBean> inputBeans, int listSize) throws FlockException, IOException, ExecutionException, InterruptedException;

    Collection<TrackResultBean> trackEntities(Fortress fortress, List<EntityInputBean> inputBeans, int listSize) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackEntity(FortressSegment segment, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException;

    /**
     * Rebuilds all search documents for the supplied fortress
     *
     * @param fortressCode name of the fortress to rebuild
     * @throws org.flockdata.helper.FlockException Business exceptions
     */
    String reindex(Company company, String fortressCode) throws FlockException;

    String reindex(Company company, Entity entity) throws FlockException;

    String reindexByDocType(Company company, String fortressName, String docType) throws FlockException;

    EntitySummaryBean getEntitySummary(Company company, String key) throws FlockException;

    //TagCloud getTagCloud(Company company, TagCloudParams tagCloudParams) throws NotFoundException;

    void purge(Fortress fo) throws FlockException;

    void purge(Company company, String fortressCode) throws FlockException;

    void cancelLastLog(Company company, Entity entity) throws IOException, FlockException;


    void mergeTags(Company company, Long source, Long target);

    Map<String,Object> getLogContent(Entity entity, Long logId);

    /**
     * Iterates through all search documents and validates that an existing
     * Entity can be found for it by the key returned.
     *
     * Experimental
     *
     * @param company
     * @param fortressCode
     * @param docType
     * @return null - should be a message
     */

    String validateFromSearch(Company company, String fortressCode, String docType) throws FlockException;

    TrackResultBean trackEntity(Fortress fortress, EntityInputBean inputBean) throws InterruptedException, FlockException, ExecutionException, IOException;

    void purge(Company company, String fortressCode, String docType);

    void purge(Company company, String code, String code1, String segment);
}
