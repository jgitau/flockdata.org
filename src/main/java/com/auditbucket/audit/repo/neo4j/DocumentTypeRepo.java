package com.auditbucket.audit.repo.neo4j;

import com.auditbucket.audit.repo.neo4j.model.DocumentType;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * User: mike
 * Date: 30/06/13
 * Time: 10:20 AM
 */
public interface DocumentTypeRepo extends GraphRepository<DocumentType> {
    @Query(elementClass = DocumentType.class, value = "start n=node({0}) " +
            "   MATCH n-[:documents]->documentType " +
            "   where documentType.name ={1} " +
            "  return documentType")
    DocumentType findCompanyDocType(Long companyId, String tagName);

}
