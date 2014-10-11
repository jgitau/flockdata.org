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

package com.auditbucket.test.functional;

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.test.endpoint.EngineEndPoints;
import com.auditbucket.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * User: mike
 * Date: 12/06/14
 * Time: 1:16 PM
 */
@Transactional
@WebAppConfiguration

public class TestQueryResults extends EngineBase {
    public static final String VEGETABLE = "Vegetable";
    public static final String FRUIT = "Fruit";
    @Autowired
    WebApplicationContext wac;


    @Test
    public void matrixQuery() throws Exception {
        SystemUser su = registerSystemUser("matrixQuery", mike_admin);
        Fortress fortress = createFortress(su);

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "mike", "Study", new DateTime(), "StudyA");
        inputBean.addTag(new TagInputBean("Apples", "likes").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Pears", "likes").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Oranges", "dislikes").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Grapes", "allergic").setLabel(TestQueryResults.FRUIT));
//        inputBean.addTag(new TagInputBean("Peas", "dislikes").setLabel(VEGETABLE));
        inputBean.addTag(new TagInputBean("Potatoes", "likes").setLabel(VEGETABLE)); // No co-occurrence
        mediationFacade.trackEntity(su.getCompany(), inputBean) ;

        inputBean = new EntityInputBean(fortress.getName(), "mike", "Study", new DateTime(), "StudyB");
        inputBean.addTag(new TagInputBean("Apples", "dislikes").setLabel(FRUIT));
        inputBean.addTag(new TagInputBean("Pears", "likes").setLabel(FRUIT));
        inputBean.addTag(new TagInputBean("Oranges", "allergic").setLabel(FRUIT));
        inputBean.addTag(new TagInputBean("Grapes", "dislikes").setLabel(FRUIT));
        inputBean.addTag(new TagInputBean("Kiwi", "likes").setLabel(FRUIT));
        inputBean.addTag(new TagInputBean("Peas", "dislikes").setLabel(VEGETABLE));
        mediationFacade.trackEntity(su.getCompany(), inputBean) ;

        MatrixInputBean input = new MatrixInputBean();
        ArrayList<String>docs = new ArrayList<>();
        docs.add("Study");
        ArrayList<String>concepts = new ArrayList<>();

        concepts.add(FRUIT);
        input.setConcepts(concepts);
        int fruitCount = 5, things = 2;
        EngineEndPoints engineEndPoints = new EngineEndPoints(wac);
        MatrixResults results= engineEndPoints.getMatrixResult(su, input);
        //MatrixResults results = queryEP.getMatrixResult(input, su.getApiKey(), su.getApiKey());
        assertFalse(results.getResults().isEmpty());
        assertEquals(4+(4*4), results.getResults().size());
        int cCount = 5;
        // ToDo: How to assert it worked!

//        assertEquals(concepts * (concepts-1), results.getResults().size());

        input.setDocuments(docs);
        concepts.clear();   // Return everything
        input.setConcepts(concepts);
        results = engineEndPoints.getMatrixResult(su, input);
        cCount = 7;
        assertFalse(results.getResults().isEmpty());
  //      assertEquals(concepts * (concepts-1), results.getResults().size());

        concepts.clear();
        concepts.add(VEGETABLE);
        input.setConcepts(concepts);
        results = engineEndPoints.getMatrixResult(su, input);

        // Though peas is recorded against both A matrix ignores occurrence with the same "concept". If both had Peas, then a Peas-Potatoes would be returned
        assertEquals("Vegetable should has no co-occurrence", 0, results.getResults().size());

        concepts.clear();
        concepts.add(FRUIT);
        ArrayList<String>filter = new ArrayList<>();
        filter.add("allergic");
        filter.add("dislikes");

        input.setFromRlxs(filter);
        input.setToRlxs(filter);
        results = engineEndPoints.getMatrixResult(su, input);
        assertFalse(results.getResults().isEmpty());
        ArrayList<String>fortresses = new ArrayList<>();
        fortresses.add(fortress.getName());
        Collection<DocumentResultBean>documentTypes = engineEndPoints.getDocuments(su, fortresses);
        assertFalse(documentTypes.isEmpty());

    }



}
