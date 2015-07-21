/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.unit;

import org.flockdata.engine.dao.MatrixDaoNeo4j;
import org.flockdata.helper.CypherHelper;
import org.flockdata.query.KeyValue;
import org.flockdata.query.MatrixInputBean;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

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
        String result = ":Entity";
        Assert.assertEquals(result, CypherHelper.getLabels("meta", inputBean.getDocuments()));
        ArrayList<String> docs = new ArrayList<>();
        docs.add("With Space");
        docs.add("SecondDoc");
        docs.add("third-doc");
        inputBean.setDocuments(docs);
        result = "meta:`With Space` or meta:SecondDoc or meta:`third-doc`";
        assertEquals(result, CypherHelper.getLabels("meta", inputBean.getDocuments()));

        docs.clear();
        docs.add(null);
        inputBean.setDocuments(docs);
        assertEquals("", CypherHelper.getLabels("meta", inputBean.getDocuments()));

    }

    @Test
    public void concepts() throws Exception {
        Collection<Object> ids = new ArrayList<>();
        Collection<Object> names = new ArrayList<>();
        ids.add("123");
        ids.add("123");
        ids.add("456");
        names.add("Name A");
        names.add("Name A");
        names.add("Name B");
        Collection<KeyValue> values = new ArrayList<>();
        MatrixDaoNeo4j.setTargetTags(values, ids, names);
        assertEquals(2, values.size());
        for (KeyValue value : values) {
            switch (value.getKey()) {
                case "123":
                    assertEquals("Name A", value.getValue());
                    break;
                case "456":
                    assertEquals("Name B", value.getValue());
                    break;
                default:
                    throw new Exception("Unexpected value");
            }

        }
    }

    @Test
    public void relationships() throws Exception {
        MatrixInputBean inputBean = new MatrixInputBean();
        assertEquals("", CypherHelper.getRelationships(inputBean.getFromRlxs()));
        assertEquals("", CypherHelper.getRelationships(inputBean.getToRlxs()));
        ArrayList<String> relationships = new ArrayList<>();
        relationships.add("With Space");
        relationships.add("SecondConcept");
        relationships.add("third-concept");
        relationships.add("dot.concept");
        relationships.add("2010");        // Numbers need to be escaped
        inputBean.setFromRlxs(relationships);
        inputBean.setToRlxs(relationships);
        assertEquals(":`With Space` |:SecondConcept |:`third-concept` |:`dot.concept` |:`2010`", CypherHelper.getRelationships(inputBean.getFromRlxs()));
        assertEquals(":`With Space` |:SecondConcept |:`third-concept` |:`dot.concept` |:`2010`", CypherHelper.getRelationships(inputBean.getToRlxs()));
    }


}
