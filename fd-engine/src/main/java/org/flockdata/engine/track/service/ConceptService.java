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

package org.flockdata.engine.track.service;

import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.TrackResultBean;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by mike on 20/06/15.
 */
public interface ConceptService {

    Collection<DocumentResultBean> getDocumentsInUse(Company company);

    Set<DocumentResultBean> findConcepts(Company company, String documentName, boolean withRelationships);

    Set<DocumentResultBean> findConcepts(Company company, Collection<String> documentNames, boolean withRelationships);

    DocumentType resolveByDocCode(Fortress fortress, String documentCode);

    DocumentType resolveByDocCode(Fortress fortress, String documentCode, Boolean createIfMissing);

    void registerConcepts(Iterable<TrackResultBean> resultBeans);

    void linkEntities(DocumentType sourceType, DocumentType targetType, EntityKeyBean entityKeyBean);

    DocumentType save(DocumentType documentType);

    DocumentType findDocumentType(Fortress fortress, String documentName);

    DocumentType findDocumentType(Fortress fortress, String documentName, boolean createIfMissing);

    DocumentType findOrCreate(Fortress fortress, DocumentType documentType);

    Set<DocumentResultBean> getConceptsWithRelationships(Company company, Collection<String> documents);

    Collection<DocumentResultBean> getDocumentsInUse(Company fdCompany, Collection<String> fortresses) throws FlockException;

    Collection<DocumentResultBean> getDocumentsInUse(Company fdCompany, String fortress) throws FlockException;

    Collection<DocumentType> makeDocTypes(FortressSegment segment, List<EntityInputBean> inputBeans);

    void delete(DocumentType documentType);

    DocumentType findDocumentTypeWithSegments(DocumentType documentType);

    DocumentResultBean findDocumentTypeWithSegments(Fortress f, String doc);

    void delete(DocumentType documentType, FortressSegment segment);

    /**
     * Concept structure associated to a Fortress. All DocumentTypes and connected concepts
     * @param company   org that owns the fortress
     * @param fortress  that which we are interested in
     * @return edges and nodes
     */
    MatrixResults getContentStructure(Company company, String fortress);

    Map<String,DocumentResultBean> getParents(DocumentType documentType);
}
