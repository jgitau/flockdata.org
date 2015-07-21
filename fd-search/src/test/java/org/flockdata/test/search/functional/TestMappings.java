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

package org.flockdata.test.search.functional;

import junit.framework.TestCase;
import org.flockdata.model.*;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.search.model.*;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.GeoDataBean;
import org.flockdata.track.bean.SearchChangeBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 15/08/14
 * Time: 12:53 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestMappings extends ESBase {

    private Logger logger = LoggerFactory.getLogger(TestMappings.class);

    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        String fortress = "fort";
        String company = "test";
        String doc = "doc";
        String user = "mike";

        Entity entity = Helper.getEntity(company, fortress, user, doc);

        EntitySearchChange change = new EntitySearchChange(entity);
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname").
                setCode("my TAG");

        Tag tag = new Tag(tagInput);
        tags.add(new EntityTagOut(entity, tag, "mytag", null));

        change.setTags(tags);

        deleteEsIndex(entity.getFortress().getIndexName());
        //searchRepo.ensureIndex(change.getIndexName(), change.getType());
        SearchResults searchResults = trackService.createSearchableChange(new EntitySearchChanges(change));
        SearchResult searchResult = searchResults.getSearchResults().iterator().next();
        Thread.sleep(1000);
        assertNotNull(searchResult);
        assertNotNull(searchResult.getSearchKey());
        entity.setSearchKey(searchResult.getSearchKey());
        json = searchRepo.findOne(entity);

        doFacetQuery(entity.getFortress().getIndexName(), "tag.mytag.thelabel.code.facet", "my TAG", 1, "Exact match of tag code is not working");
        doFieldQuery(entity.getFortress().getIndexName(), "tag.mytag.thelabel.code", "my tag", 1, "Gram match of un-faceted tag code is not working");
//        doTermQuery(entity.getFortress().getIndexName(), "tag.mytag.code", "my tag", 1, "Case insensitive text match of tag codes is not working");
        //doTermQuery(entity.getFortress().getIndexName(), "tag.mytag.code", "my", 1, "Keyword search of tag codes is not working");
//        doTermQuery(entity.getFortress().getIndexName(), "tag.mytag.code.analyzed", "my tag", 1, "Case insensitive search of tag codes is not working");
        assertNotNull(json);

    }

    @Test
    public void count_CorrectSearchResults() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        String fortress = "fort";
        String company = "test";
        String doc = "doc";
        String user = "mike";

        Entity entity = Helper.getEntity(company, fortress, user, doc);
        Entity entityB = Helper.getEntity(company, fortress, user, doc);

        EntitySearchChange change = new EntitySearchChange(entity);
        EntitySearchChange changeB = new EntitySearchChange(entityB);
        change.setDescription("Test Description");
        change.setWhat(json);
        changeB.setWhat(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname");
        tagInput.setCode("my TAG");
        Tag tag = new Tag(tagInput);

        tags.add(new EntityTagOut(entity, tag, "mytag", null));
        change.setTags(tags);

        deleteEsIndex(entity.getFortress().getIndexName());

        Collection<SearchChangeBean> changes = new ArrayList<>();
        changes.add(change);
        changes.add(changeB);
        EntitySearchChanges searchChanges = new EntitySearchChanges(changes);
        SearchResults searchResults = trackService.createSearchableChange(searchChanges);
        assertEquals("2 in 2 out", 2, searchResults.getSearchResults().size());
    }



    @Test
    public void testCustomMappingWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = Helper.getEntity("cust", "fort", "anyuser", "fort");
        Entity entityB = Helper.getEntity("cust", "fortb", "anyuser", "fortb");

        SearchChangeBean changeA = new EntitySearchChange(entityA, new ContentInputBean(json));
        SearchChangeBean changeB = new EntitySearchChange(entityB, new ContentInputBean(json));

        // FortB will have
        changeA.setDescription("Test Description");
        changeB.setDescription("Test Description");

        deleteEsIndex(entityA.getFortress().getIndexName());
        deleteEsIndex(entityB.getFortress().getIndexName());

        searchRepo.ensureIndex(changeA.getIndexName(), changeA.getDocumentType());
        searchRepo.ensureIndex(changeB.getIndexName(), changeB.getDocumentType());
        changeA = searchRepo.handle(changeA);
        changeB = searchRepo.handle(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        // by default we analyze the @description field
        doDefaultFieldQuery(entityA.getFortress().getIndexName(), "description", changeA.getDescription(), 1);

        // In fortb.json we don't analyze the description (overriding the default) so it shouldn't be found
        doDefaultFieldQuery(entityB.getFortress().getIndexName(), "description", changeB.getDescription(), 0);

    }

    @Test
    public void sameIndexDifferentDocumentsHaveMappingApplied() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = Helper.getEntity("cust", "fort", "anyuser", "fortdoc");
        Entity entityB = Helper.getEntity("cust", "fort", "anyuser", "doctype");


        SearchChangeBean changeA = new EntitySearchChange(entityA, new ContentInputBean(json));
        SearchChangeBean changeB = new EntitySearchChange(entityB, new ContentInputBean(json));

        Tag tag = new Tag(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        assertEquals("my TAG", tag.getCode());
        ArrayList<EntityTag> tagsA = new ArrayList<>();
        tagsA.add(new EntityTagOut(entityA, tag, "mytag", null));

        ArrayList<EntityTag> tagsB = new ArrayList<>();
        tagsB.add(new EntityTagOut(entityB, tag, "mytag", null));

        changeA.setTags(tagsA);
        changeB.setTags(tagsB);

        deleteEsIndex(entityA.getFortress().getIndexName());
        deleteEsIndex(entityB.getFortress().getIndexName());

        searchRepo.ensureIndex(changeA.getIndexName(), changeA.getDocumentType());
        searchRepo.ensureIndex(changeB.getIndexName(), changeB.getDocumentType());
        changeA = searchRepo.handle(changeA);
        changeB = searchRepo.handle(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        doFacetQuery(entityA.getFortress().getIndexName(), entityA.getType().toLowerCase(), "tag.mytag.thelabel.code.facet", tag.getCode(), 1);
        doFacetQuery(entityB.getFortress().getIndexName(), entityB.getType().toLowerCase(), "tag.mytag.thelabel.code.facet", tag.getCode(), 1);
        doFacetQuery(entityB.getFortress().getIndexName(), "tag.mytag.thelabel.code.facet", tag.getCode(), 2);

    }

    @Test
    public void tagWithRelationshipNamesMatchingNodeNames() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = Helper.getEntity("cust", "fort-tag-rlx", "anyuser", "fortdoc");

        SearchChangeBean changeA = new EntitySearchChange(entityA, new ContentInputBean(json));

        Tag tag = new Tag(new TagInputBean("aValue", "myTag", "myTag"));
        tag.setName("myTag");// This will be used as the relationship name between the entity and the tag!

        ArrayList<EntityTag> tags = new ArrayList<>();
        tags.add(new EntityTagOut(entityA, tag, "mytag", null));
        changeA.setTags(tags);

        deleteEsIndex(entityA.getFortress().getIndexName());

        searchRepo.ensureIndex(changeA.getIndexName(), changeA.getDocumentType());

        changeA = searchRepo.handle(changeA);

        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeA.getSearchKey());

        // DAT-328
        doFacetQuery(entityA.getFortress().getIndexName(), entityA.getType().toLowerCase(), "tag.mytag.code.facet", tag.getCode(), 1);

    }

    @Test
    public void geo_Points() throws Exception {
        String comp = "geo_Points";
        String fort = "geo_Points";
        String user = "mikey";

        Entity entity = Helper.getEntity(comp, fort, user, fort);
        deleteEsIndex(entity.getFortress().getIndexName());

        Map<String, Object> what = Helper.getSimpleMap(
                EntitySearchSchema.WHAT_CODE, "GEO");
        what.put(EntitySearchSchema.WHAT_NAME, "NameText");
        what.put(EntitySearchSchema.WHAT_DESCRIPTION, "This is a description");

        TagInputBean tagInput = new TagInputBean("tagcode", "TagLabel", "tag-relationship");
        Tag tag = new Tag(tagInput);


        ArrayList<EntityTag> tags = new ArrayList<>();

        HashMap<String, Object> tagProps = new HashMap<>();
        tagProps.put("num", 100d);
        tagProps.put("str", "hello");
        EntityTag entityTag = new EntityTagOut(entity, tag, "entity-relationship", tagProps);
        // DAT-442 Geo refactoring
        GeoDataBean geoData = new GeoDataBean();
        geoData.add("country", "NZ", "New Zealand", 174.0, -41.0);
        assertEquals("NZ", geoData.getProperties().get("country.code"));
        assertEquals("New Zealand", geoData.getProperties().get("country.name"));
        assertEquals("174.0,-41.0", geoData.getProperties().get("points.country"));
        entityTag.setGeoData(geoData);
        tags.add(entityTag);

        EntitySearchChange change = new EntitySearchChange(entity);

        change.setWhat(what);
        change.setTags(tags);

        searchRepo.ensureIndex(change.getIndexName().toLowerCase(), change.getDocumentType());
        SearchChangeBean searchResult = searchRepo.handle(change);
        TestCase.assertNotNull(searchResult);
        Thread.sleep(2000);

        String result = doQuery(change.getIndexName().toLowerCase(), "*", 1);
        logger.info(result);
        assertTrue(result.contains("points.country"));
        assertTrue(result.contains("174"));
        assertTrue(result.contains("-41"));

        doCompletionQuery(change.getIndexName().toLowerCase(), "nz", 1, "Couldn't autocomplete on geo tag for NZ");
        doCompletionQuery(change.getIndexName().toLowerCase(), "new", 1, "Couldn't autocomplete on geo tag for New Zealand");
    }

}
