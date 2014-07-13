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

package com.auditbucket.test.unit;

import com.auditbucket.helper.NeoSyntaxHelper;
import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.registration.model.Tag;
import org.junit.Test;

import javax.ws.rs.HEAD;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 12/06/14
 * Time: 11:35 AM
 */
public class TestQueryParameters {
    @Test
    public void documentTypes() throws Exception {
        MatrixInputBean inputBean = new MatrixInputBean();
        String result =":_MetaHeader";
        assertEquals(result, NeoSyntaxHelper.getLabels("meta", inputBean.getDocuments()));
        ArrayList<String>docs = new ArrayList<>();
        docs.add("With Space");
        docs.add("SecondDoc");
        docs.add("third-doc");
        inputBean.setDocuments(docs);
        result = "meta:`With Space` or meta:SecondDoc or meta:`third-doc`";
        assertEquals(result, NeoSyntaxHelper.getLabels("meta", inputBean.getDocuments()));

        docs.clear();
        docs.add(null);
        inputBean.setDocuments(docs);
        assertEquals("", NeoSyntaxHelper.getLabels("meta", inputBean.getDocuments()));

    }

//    @Test
//    public void concepts() throws Exception {
//        MatrixInputBean inputBean = new MatrixInputBean();
//        assertEquals(Tag.DEFAULT, NeoSyntaxHelper.getConcepts(inputBean.getConcepts()));
//        ArrayList<String>concepts = new ArrayList<>();
//        concepts.add("With Space");
//        concepts.add("SecondConcept");
//        inputBean.setConcepts(concepts);
//        assertEquals(":`With Space` :SecondConcept", NeoSyntaxHelper.getConcepts(inputBean.getConcepts()));
//
//        // check that quotes don't cause a problem
//        concepts.clear();
//        concepts.add("SecondConcept");
//        concepts.add("With Space");
//        inputBean.setConcepts(concepts);
//        assertEquals(":SecondConcept :`With Space`", NeoSyntaxHelper.getConcepts(inputBean.getConcepts()));
//
//    }

    @Test
    public void relationships() throws Exception {
        MatrixInputBean inputBean = new MatrixInputBean();
        assertEquals("", NeoSyntaxHelper.getRelationships(inputBean.getFromRlxs()));
        assertEquals("", NeoSyntaxHelper.getRelationships(inputBean.getToRlxs()));
        ArrayList<String>relationships = new ArrayList<>();
        relationships.add("With Space");
        relationships.add("SecondConcept");
        relationships.add("third-concept");
        relationships.add("dot.concept");
        inputBean.setFromRlxs(relationships);
        inputBean.setToRlxs(relationships);
        assertEquals(":`With Space` |:SecondConcept |:`third-concept` |:`dot.concept`", NeoSyntaxHelper.getRelationships(inputBean.getFromRlxs()));
        assertEquals(":`With Space` |:SecondConcept |:`third-concept` |:`dot.concept`", NeoSyntaxHelper.getRelationships(inputBean.getToRlxs()));
    }


}