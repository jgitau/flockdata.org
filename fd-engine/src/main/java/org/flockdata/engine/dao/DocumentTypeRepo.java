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

package org.flockdata.engine.dao;

import org.flockdata.model.Company;
import org.flockdata.model.Concept;
import org.flockdata.model.DocumentType;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 30/06/13
 * Time: 10:20 AM
 */
public interface DocumentTypeRepo extends GraphRepository<DocumentType> {
    @Query( value =
                    "match ( d:DocType {companyKey:{0} }) return d;")
    DocumentType findFortressDocCode(String docKey);

//    @Query(value =
//                    "optional MATCH (company:FDCompany)<-[:TAG_INDEX]-(tag:TagLabel) " +
//                            "        where id(company)={0} and tag.companyKey ={1}" +
//                            "       return tag")

        @Query(value =
                    "MATCH (tag:Concept) " +
                            "        where tag.key ={1}" +
                            "       return tag")
    Concept schemaTagDefExists(Long companyId, String companyKey);


    @Query(elementClass = DocumentType.class,
            value = "MATCH (fortress:Fortress)<-[:FORTRESS_DOC]-(documentTypes:DocType) " +
                    "        where id(fortress)={0} " +
                    "       return documentTypes")
    Collection<DocumentType> getFortressDocumentsInUse(Long fortressId);

    @Query(elementClass = DocumentType.class,
            value = "match (docTypes:DocType)-[*..2]-(company:FDCompany) " +
                    "where id(company) = {0} return docTypes")
    Collection<DocumentType> getCompanyDocumentsInUse(Long companyId);

    @Query (value = "match (fortress:Fortress)-[r:FORTRESS_DOC]-(d:DocType)-[dr]-()" +
            "where id(fortress) = {0} delete r,d,dr")
    void purgeFortressDocuments(Long fortressId);

    @Query(elementClass = DocumentType.class,
            value = "MATCH (company:FDCompany) -[:OWNS]->(fortress:Fortress)<-[:FORTRESS_DOC]-(doc:DocType) " +
                    "        where id(company)={0} and doc.name in{1}" +
                    "       return doc")
    Set<DocumentType> findDocuments(Company company, Collection<String> documents);

    @Query(elementClass = DocumentType.class,
            value = "MATCH (company:FDCompany) -[:OWNS]->(fortress:Fortress)<-[:FORTRESS_DOC]-(doc:DocType) " +
                    "        where id(company)={0} " +
                    "       return doc")
    Set<DocumentType> findAllDocuments(Company company);

    @Query (value = "optional match (fortress:Fortress)-[fd:FORTRESS_DOC]-(d:DocType)-[r]-(c:Concept)" +
            "where id(d) = {0} delete r")
    void purgeDocumentAssociations(Long documentTypeId);

    @Query (value = "optional match (fortress:Fortress)-[fd:FORTRESS_DOC]-(d:DocType)-[r]-(c:Concept)" +
            "where id(fortress) = {0} delete r")
    void purgeDocumentConceptRelationshipsForFortress(Long fortressId);

    @Query (value = "optional match (d:DocType)-[r:USES_SEGMENT]-(s:FortressSegment)" +
            "where id(d) = {0} delete r")
    void purgeDocumentSegments(Long documentTypeId);

    @Query (value = "optional match (fortress:Fortress)-[:DEFINES]-(c:FortressSegment)-[r]-(d:DocType)" +
            "where id(fortress) = {0} delete r")
    void purgeFortressSegmentAssociations(Long fortressId);
}
