/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.test.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.mapping.GetMapping;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.time.StopWatch;
import org.flockdata.engine.query.endpoint.QueryEP;
import org.flockdata.engine.query.service.QueryService;
import org.flockdata.engine.track.endpoint.TrackEP;
import org.flockdata.engine.track.service.FdServerWriter;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.JsonUtils;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.RegistrationBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.model.Tag;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.search.model.SearchResult;
import org.flockdata.track.bean.*;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.service.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Allows the fd-engine services to be tested against fd-search with actual integration.
 * fd-search is stated by Cargo as a Tomcat server.
 * fd-engine runs as a usual Spring test runner.
 * <p/>
 * This approach requires RabbitMQ and REDIS to be installed to allow integration to occur.
 * <p/>
 * No web interface is launched for fd-engine
 * <p/>
 * Make sure that you create unique User ids for your test.
 * <p/>
 * To run the integration suite:
 * mvn clean install -P integration
 * <p/>
 * If you want to debug engine then you add to your command line
 * -Dfd.debug=true -DforkCount=0
 * <p/>
 * To debug the search service refer to the commented line in pom.xml where the
 * default port is set to 8000
 * <p/>
 * User: nabil, mike
 * Date: 16/07/13
 * Time: 22:51
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"classpath:root-context.xml", "classpath:apiDispatcher-servlet.xml"})
public class TestFdIntegration {
    private static boolean runMe = true; // pass -Dfd.debug=true to disable all tests
    private static int fortressMax = 1;
    private static JestClient esClient;

    @Autowired
    TrackService trackService;
    @Autowired
    TrackEP trackEP;
    @Autowired
    RegistrationService regService;

    @Autowired
    CompanyService companyService;

    @Autowired
    LogService logService;

    @Autowired
    TagService tagService;

    @Autowired
    FortressService fortressService;

    @Qualifier("mediationFacadeNeo4j")
    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    EntityTagService entityTagService;

    @Autowired
    QueryService queryService;

    @Autowired
    QueryEP queryEP;

    @Autowired
    KvService kvService;

    static MockMvc mockMvc;

    @Autowired
    WebApplicationContext wac;

    @Autowired
    FdServerWriter serverWriter;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ApplicationContext applicationContext;

    private static Logger logger = LoggerFactory.getLogger(TestFdIntegration.class);
    private static Authentication AUTH_MIKE = new UsernamePasswordAuthenticationToken("mike", "123");

    String company = "Monowai";
    static Properties properties = new Properties();
    int esTimeout = 10; // Max attempts to find the result in ES

    @AfterClass
    public static void waitAWhile() throws Exception {
        waitAWhile(null, 3000);
    }

    public static void waitAWhile(String message) throws Exception {
        String ss = System.getProperty("sleepSeconds");
        if (ss == null || ss.equals(""))
            ss = "1";
        if (message == null)
            message = "Slept for {} seconds";
        waitAWhile(message, Long.decode(ss) * 1000);
    }

    @BeforeClass
    @Rollback(false)
    public static void cleanupElasticSearch() throws Exception {
        FileInputStream f = new FileInputStream("./src/test/resources/config.properties");
        properties.load(f);
        String abDebug = System.getProperty("fd.debug");
        if (abDebug != null)
            runMe = !Boolean.parseBoolean(abDebug);

        HttpClientConfig clientConfig = new HttpClientConfig.Builder("http://localhost:" + properties.get("es.http.port")).multiThreaded(false).build();
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(clientConfig);
        //factory.setClientConfig(clientConfig);
        esClient = factory.getObject();

        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.suppress");
        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.testfortress");
        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.ngram");
        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.rebuildtest");
        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.audittest");
        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.suppress");
        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.entitywithtagsprocess");
        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.trackgraph");
        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.audittest");
        deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.111");

        for (int i = 1; i < fortressMax + 1; i++) {
            deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.bulkloada" + i);
        }

    }

    public void setDefaultAuth() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        if (mockMvc == null)
            mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

    }

    private static void deleteEsIndex(String indexName) throws Exception {
        logger.info("%% Delete Index {}", indexName);
        esClient.execute(new DeleteIndex.Builder(indexName).build());
    }

    @AfterClass
    public static void shutDownElasticSearch() throws Exception {
        esClient.shutdownClient();
    }

    @Test
    public void dataTypes_WhatFieldsIndexed() throws Exception {
        assumeTrue(runMe);
        logger.info("## dataTypes_WhatFieldsIndexed");

        SystemUser su = registerSystemUser("dataTypes_WhatFieldsIndexed", "dataTypes_WhatFieldsIndexed");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("dataTypes_WhatFieldsIndexed"));
        String docType = "DT";
        String callerRef = "ABC123X";
        EntityInputBean entityInputBean =
                new EntityInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef);

        Map<String, Object> json = getRandomMap();
        json.put("int", 123);
        json.put("long", 456l);
        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime(), json);
        entityInputBean.setContent(contentInputBean);

        Entity entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();
        waitForInitialSearchResult(su.getCompany(), entity.getMetaKey());

        doEsQuery(fortress.getIndexName(), entity.getMetaKey());
        doEsFieldQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + ".int", "123", 1);
    }

    @Test
    public void companyAndFortressWithSpaces() throws Exception {
        assumeTrue(runMe);
        logger.info("## companyAndFortressWithSpaces");

        SystemUser su = registerSystemUser("testcompany", "companyAndFortressWithSpaces");
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("Track Test"));
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        EntityInputBean entityInputBean =
                new EntityInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime(), getRandomMap());
        entityInputBean.setContent(contentInputBean);

        Entity entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();

        assertEquals(EntitySearchSchema.PREFIX + "testcompany.tracktest", entity.getFortress().getIndexName());

        waitForInitialSearchResult(su.getCompany(), entity.getMetaKey());

        doEsQuery(entity.getFortress().getIndexName(), entity.getMetaKey());
    }

    @Test
    public void pdf_TrackedAndFound() throws Exception {
        assumeTrue(runMe);
        logger.info("## pdf_TrackedAndFound");

        SystemUser su = registerSystemUser("pdf_TrackedAndFound", "co-fortress");
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("pdf_TrackedAndFound"));
        String docType = "Contract";
        String callerRef = "ABC123X";
        EntityInputBean entityInputBean =
                new EntityInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime());
        contentInputBean.setAttachment(Helper.getPdfDoc(), "pdf", "test.pdf");
        entityInputBean.setContent(contentInputBean);

        Entity entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();

        waitForInitialSearchResult(su.getCompany(), entity.getMetaKey());

        doEsQuery(entity.getFortress().getIndexName(), entity.getMetaKey());
        doEsQuery(entity.getFortress().getIndexName(), "brown fox", 1);
        doEsQuery(entity.getFortress().getIndexName(), "test.pdf", 1);
        doEsFieldQuery(entity.getFortress().getIndexName(), EntitySearchSchema.FILENAME, "test.pdf", 1);
        doEsFieldQuery(entity.getFortress().getIndexName(), EntitySearchSchema.ATTACHMENT, "pdf", 1);
    }


    @Test
    public void entity_WithOnlyTagsTracksToSearch() throws Exception {
        assumeTrue(runMe);
        logger.info("## entity_WithOnlyTagsTracksToSearch");
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        SystemUser su = registerSystemUser("Mark");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entityWithTagsProcess"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", now, "ABCXYZ123");
        inputBean.setMetaOnly(true);
        inputBean.addTag(new TagInputBean("testTagNameZZ", "someAuditRLX"));
        inputBean.setEvent("TagTest");
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        logger.debug("Created Request ");
        waitForEntitiesToUpdate(su.getCompany(), result.getEntity());
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), result.getMetaKey());
        assertNotNull(summary);
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getEntity().getFortress().getIndexName(), inputBean.getEvent(), 1);
        // Can we find the Tag
        doEsQuery(summary.getEntity().getFortress().getIndexName(), "testTagNameZZ", 1);

    }

    @Test
    public void immutableEntityWithNoLogsAreIndexed() throws Exception {
        assumeTrue(runMe);
        logger.info("## immutableEntityWithNoLogsAreIndexed");
        SystemUser su = registerSystemUser("Manfred");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("immutableEntityWithNoLogsAreIndexed"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", now, "ZZZ123");
        inputBean.setEvent("immutableEntityWithNoLogsAreIndexed");
        inputBean.setMetaOnly(true); // Must be true to make over to search
        TrackResultBean trackResult;
        trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        waitForInitialSearchResult(su.getCompany(), trackResult.getEntity().getMetaKey());
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), trackResult.getMetaKey());
        waitForInitialSearchResult(su.getCompany(), trackResult.getMetaKey());
        assertNotNull(summary);
        assertSame("change logs were not expected", 0, summary.getChanges().size());
        assertNotNull("Search record not received", summary.getEntity().getSearchKey());
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getEntity().getFortress().getIndexName(), inputBean.getEvent(), 1);

        // Not flagged as meta only so will not appear in the search index until a log is created
        inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", now, "ZZZ999");
        trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        summary = mediationFacade.getEntitySummary(su.getCompany(), trackResult.getMetaKey());
        assertNotNull(summary);
        assertSame("No change logs were expected", 0, summary.getChanges().size());
        assertNull(summary.getEntity().getSearchKey());
        // Check we can't find the Event in ElasticSearch
        doEsQuery(summary.getEntity().getFortress().getIndexName(), "ZZZ999", 0);
    }

    @Test
    public void rebuildESIndexFromEngine() throws Exception {
        assumeTrue(runMe);
        logger.info("## rebuildESIndexFromEngine");
        SystemUser su = registerSystemUser("David");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("rebuildTest"));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));
        TrackResultBean auditResult = mediationFacade.trackEntity(su.getCompany(), inputBean);

        Entity entity = trackService.getEntity(su.getCompany(), auditResult.getMetaKey());
        waitForEntitiesToUpdate(su.getCompany(), entity);

        doEsQuery(entity.getFortress().getIndexName(), "*");

        // Rebuild....
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        Long lResult = mediationFacade.reindex(fo.getCompany(), fo.getCode());
        waitForEntitiesToUpdate(su.getCompany(), entity);
        assertNotNull(lResult);
        assertEquals(1l, lResult.longValue());

        doEsQuery(entity.getFortress().getIndexName(), "*");

    }

    @Test
    public void
    load_createEntityAndTimeLogsWithSearchActivated() throws Exception {
        assumeTrue(runMe);
        logger.info("## createHeaderTimeLogsWithSearchActivated");
        int max = 3;
        String metaKey;
        SystemUser su = registerSystemUser("Olivia");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("111"));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        TrackResultBean auditResult;
        auditResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        metaKey = auditResult.getMetaKey();

        assertNotNull(metaKey);

        Entity entity = trackService.getEntity(su.getCompany(), metaKey);
        assertNotNull(entity);
        assertNotNull(trackService.findByCallerRef(fo, "TestTrack", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;

        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", metaKey, new DateTime(), getSimpleMap("blah", i))).getEntity();
            i++;
        }
        waitForLogCount(su.getCompany(), entity, 3);
        waitAWhile("Give ES a chance to catch up");

        watch.stop();
        // Test that we get the expected number of log events
        if (!"rest".equals(System.getProperty("neo4j"))) // Don't check if running over rest
            assertEquals("This will fail if the DB is not cleared down, i.e. testing over REST", max, trackService.getLogCount(su.getCompany(), metaKey));

        doEsFieldQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + ".blah", "*", 1);
    }

    @Test
    public void auditsByPassGraphByCallerRef() throws Exception {
        assumeTrue(runMe);
        logger.info("## auditsByPassGraphByCallerRef started");
//        deleteEsIndex(EntitySearchSchema.PREFIX+"monowai.trackgraph");
        SystemUser su = registerSystemUser("Isabella");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TrackGraph"));

        EntityInputBean entityInput = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        entityInput.setTrackSuppressed(true);
        entityInput.setMetaOnly(true); // If true, the entity will be indexed
        // Track suppressed but search is enabled
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        waitAWhile();

        String indexName = EntitySearchSchema.parseIndex(fortress);
        assertEquals(EntitySearchSchema.PREFIX + "monowai.trackgraph", indexName);

        // Putting asserts On elasticsearch
        doEsQuery(indexName, "*", 1);
        entityInput = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        entityInput.setTrackSuppressed(true);
        entityInput.setMetaOnly(true);
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        waitAWhile();
        doEsQuery(indexName, "*", 2);

        entityInput = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        entityInput.setTrackSuppressed(true);
        entityInput.setMetaOnly(true);
        Entity entity = mediationFacade.trackEntity(su.getCompany(), entityInput).getEntity();
        assertNull(entity.getMetaKey());
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 2);

        entityInput = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        entityInput.setTrackSuppressed(true);
        entityInput.setMetaOnly(true);
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 2);

        entityInput = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC125");
        entityInput.setTrackSuppressed(true);
        entityInput.setMetaOnly(true);
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 3);

    }

    @Test
    public void searchDocIsRewrittenAfterCancellingLogs() throws Exception {
        // DAT-27
        assumeTrue(runMe);
        logger.info("## searchDocRewrite");
        SystemUser su = registerSystemUser("Felicity");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("cancelLogTag"));
        EntityInputBean entityInput = new EntityInputBean(fo.getName(), "wally", "CancelDoc", new DateTime(), "ABC123");
        ContentInputBean content = new ContentInputBean("wally", new DateTime(), getRandomMap());
        entityInput.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        entityInput.addTag(new TagInputBean("Happy Days").addEntityLink("testingb"));
        entityInput.setContent(content);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);

        waitForEntitiesToUpdate(su.getCompany(), result.getEntity());
        // ensure that non-analysed tags work
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testinga.code", "happy", 1);
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingb.code", "happy days", 1);
        // We now have 1 content with tags validated in ES

        // Add another Log - replacing the two existing Tags with two new ones
        content = new ContentInputBean("wally", new DateTime(), getRandomMap());
        entityInput.getTags().clear();
        entityInput.addTag(new TagInputBean("Sad Days").addEntityLink("testingb"));
        entityInput.addTag(new TagInputBean("Days Bay").addEntityLink("testingc"));
        entityInput.setContent(content);
        result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        waitForEntitiesToUpdate(su.getCompany(), result.getEntity());
        // We now have 2 logs, sad tags, no happy tags

        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingb.code", "sad days", 1);
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingc.code", "days bay", 1);
        // These were removed in the update
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testinga.code", "happy", 0);
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingb.code", "happy days", 0);

        // Cancel Log - this will remove the sad tags and leave us with happy tags
        mediationFacade.cancelLastLog(su.getCompany(), result.getEntity());
        waitForEntitiesToUpdate(su.getCompany(), result.getEntity());
        Collection<EntityTag> tags = entityTagService.getEntityTags(su.getCompany(), result.getEntity());
        assertEquals(2, tags.size());

        // These should have been added back in due to the cancel operation
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testinga.code", "happy", 1);
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingb.code", "happy days", 1);

        // These were removed in the cancel
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingb.code", "sad days", 0);
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingc.code", "days bay", 0);


    }

    @Test
    public void tagKeySearch() throws Exception {
        // DAT-95
        assumeTrue(runMe);
        logger.info("## tagKeySearch");
        SystemUser su = registerSystemUser("Cameron");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("tagKeySearch"));
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        ContentInputBean log = new ContentInputBean("wally", new DateTime(), getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        inputBean.addTag(new TagInputBean("Happy Days").addEntityLink("testingb"));
        inputBean.addTag(new TagInputBean("Sad Days").addEntityLink("testingb"));
        inputBean.addTag(new TagInputBean("Days Bay").addEntityLink("testingc"));
        inputBean.setContent(log);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking
        waitForEntitiesToUpdate(su.getCompany(), result.getEntity());
        // ensure that non-analysed tags work
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testinga.code", "happy", 1);
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingb.code", "happy days", 1);
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingb.code", "sad days", 1);
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingc.code", "days bay", 1);
        doEsTermQuery(result.getEntity().getFortress().getIndexName(), EntitySearchSchema.TAG + ".testingc.code", "days", 0);

    }

    @Test
    public void searchIndexWithNoMetaKeysDoesNotError() throws Exception {
        // DAT-83
        assumeTrue(runMe);
        logger.info("## searchDocWithNoMetaKeyWorks");
        SystemUser su = registerSystemUser("HarryIndex");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("searchIndexWithNoMetaKeysDoesNotError"));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        inputBean.setTrackSuppressed(true); // Write a search doc only
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));
        // First entity and log, but not stored in graph
        mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking

        inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), result.getMetaKey());

        waitForEntitiesToUpdate(su.getCompany(), entity); // 2nd document in the index
        // We have one with a metaKey and one without
        doEsQuery(EntitySearchSchema.PREFIX + "monowai." + fo.getCode(), "*", 2);

        QueryParams qp = new QueryParams(fo);
        qp.setSimpleQuery("*");
        String queryResult = runMetaQuery(qp);
        logger.info(queryResult);

        // Two search docs,but one without a metaKey

    }

    @Test
    public void engineQueryResultsReturn() throws Exception {
        // DAT-83
        assumeTrue(runMe);
        logger.info("## engineQueryResultsReturn");
        SystemUser su = registerSystemUser("Kiwi");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("QueryTest"));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));

        mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking

        inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);

        Entity entity = trackService.getEntity(su.getCompany(), result.getMetaKey());

        waitForEntitiesToUpdate(su.getCompany(), entity); // 2nd document in the index
        // We have one with a metaKey and one without
        doEsQuery(EntitySearchSchema.PREFIX + "monowai." + fo.getCode(), "*", 2);

        QueryParams qp = new QueryParams(fo);
        qp.setSimpleQuery("*");
        runMetaQuery(qp);
        //EsSearchResult queryResults =runSearchQuery(su, qp);
        //EsSearchResult queryResults =mediationFacade.search(su.getCompany(), qp);
        EsSearchResult queryResults = runSearchQuery(su, qp);
        assertNotNull(queryResults);
        assertEquals(2, queryResults.getResults().size());

        // Two search docs,but one without a metaKey

    }

    @Test
    public void utcDateFieldsThruToSearch() throws Exception {
        // DAT-196
        assumeTrue(runMe);
        logger.info("## utcDateFieldsThruToSearch");
        SystemUser su = registerSystemUser("Kiwi-UTC");
        FortressInputBean fib = new FortressInputBean("utcDateFieldsThruToSearch", false);
        fib.setTimeZone("Europe/Copenhagen"); // Arbitrary TZ
        Fortress fo = fortressService.registerFortress(su.getCompany(), fib);

        DateTimeZone ftz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(fib.getTimeZone()));
        DateTimeZone utz = DateTimeZone.UTC;
        DateTimeZone ltz = DateTimeZone.getDefault();

        DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4, 30, DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Copenhagen")));
        DateTime lastUpdated = new DateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Copenhagen")));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", fortressDateCreated, "ABC123");
        inputBean.setContent(new ContentInputBean("wally", lastUpdated, getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking

        Entity entity = trackService.getEntity(su.getCompany(), result.getMetaKey());

        assertEquals(EntitySearchSchema.PREFIX + "monowai." + fo.getCode(), entity.getFortress().getIndexName());
        assertEquals("DateCreated not in Fortress TZ", 0, fortressDateCreated.compareTo(entity.getFortressDateCreated()));

        EntityLog log = trackService.getLastEntityLog(su.getCompany(), result.getMetaKey());
        assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(ftz)));


        waitForEntitiesToUpdate(su.getCompany(), entity); // 2nd document in the index
        // We have one with a metaKey and one without
        doEsQuery(EntitySearchSchema.PREFIX + "monowai." + fo.getCode(), "*", 1);

        QueryParams qp = new QueryParams(fo);
        qp.setSimpleQuery("*");
        runMetaQuery(qp);
        EsSearchResult queryResults = runSearchQuery(su, qp);
        assertNotNull(queryResults);
        assertEquals(1, queryResults.getResults().size());
        for (SearchResult searchResult : queryResults.getResults()) {
            logger.info("whenCreated utc-{}", new DateTime(searchResult.getWhenCreated(), utz));
            assertEquals(fortressDateCreated, new DateTime(searchResult.getWhenCreated(), ftz));
            logger.info("whenCreated ftz-{}", new DateTime(searchResult.getWhenCreated(), ftz));
            assertEquals(new DateTime(fortressDateCreated, utz), new DateTime(searchResult.getWhenCreated(), utz));
            logger.info("lastUpdate  utc-{}", new DateTime(searchResult.getLastUpdate(), utz));
            assertEquals(lastUpdated, new DateTime(searchResult.getLastUpdate(), ftz));
            logger.info("lastUpdate  ftz-{}", new DateTime(searchResult.getLastUpdate(), ftz));
            assertEquals(new DateTime(lastUpdated, utz), new DateTime(searchResult.getLastUpdate(), utz));
            assertNotNull(searchResult.getAbTimestamp());
            logger.info("timestamp   ltz-{}", new DateTime(searchResult.getAbTimestamp(), ltz));

        }

    }


    private EsSearchResult runSearchQuery(SystemUser su, QueryParams input) throws Exception {
        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/query/")
                        .header("Api-Key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(input))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), EsSearchResult.class);
    }


    /**
     * Suppresses the indexing of a log record even if the fortress is set to index everything
     *
     * @throws Exception
     */
    @Test
    public void suppressIndexingOnDemand() throws Exception {
        assumeTrue(runMe);
        logger.info("## suppressIndexOnDemand");

        SystemUser su = registerSystemUser("Barbara");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("suppress"));
        EntityInputBean inputBean = new EntityInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());

        //Transaction tx = getTransaction();
        TrackResultBean indexedResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), indexedResult.getMetaKey());

        LogResultBean resultBean = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), getSimpleMap("who", "andy"))).getLogResult();
        assertNotNull(resultBean);

        waitForEntitiesToUpdate(su.getCompany(), entity);
        String indexName = entity.getFortress().getIndexName();

        doEsQuery(indexName, "andy");

        inputBean = new EntityInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setSearchSuppressed(true);
        TrackResultBean noIndex = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity noIndexEntity = trackService.getEntity(su.getCompany(), noIndex.getMetaKey());

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", noIndexEntity.getMetaKey(), new DateTime(), getSimpleMap("who", "bob")));
        // Bob's not there because we said we didn't want to index that entity
        doEsQuery(indexName, "bob", 0);
        doEsQuery(indexName, "andy");
    }

    @Test
    public void tagKeyReturnsSingleSearchResult() throws Exception {
        assumeTrue(runMe);
        logger.info("## tagKeyReturnsSingleSearchResult");

        SystemUser su = registerSystemUser("Peter");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("suppress"));
        EntityInputBean metaInput = new EntityInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        String relationshipName = "example"; // Relationship names is indexed are @tag.relationshipName.code in ES
        TagInputBean tag = new TagInputBean("Code Test Works", relationshipName);
        metaInput.addTag(tag);

        TrackResultBean indexedResult = mediationFacade.trackEntity(su.getCompany(), metaInput);
        Entity entity = trackService.getEntity(su.getCompany(), indexedResult.getMetaKey());
        String indexName = entity.getFortress().getIndexName();

        Collection<EntityTag> tags = entityTagService.getEntityTags(su.getCompany(), entity);
        assertNotNull(tags);
        assertEquals(1, tags.size());

        LogResultBean resultBean = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), getRandomMap())).getLogResult();
        assertNotNull(resultBean);

        waitForEntitiesToUpdate(su.getCompany(), entity);
        doEsTermQuery(indexName, "@tag." + relationshipName + ".code", "code test works", 1);

    }

    @Test
    public void testCancelUpdatesSearchCorrectly() throws Exception {
        assumeTrue(runMe);
        // DAT-53
        logger.info("## testCancelUpdatesSearchCorrectly");

        SystemUser su = registerSystemUser("Rocky");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("testCancelUpdatesSearchCorrectly"));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "clb1");
        inputBean.setContent(new ContentInputBean("olivia@sunnybell.com", firstDate, getSimpleMap("house", "house1")));
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        Entity entity = trackService.getEntity(su.getCompany(), metaKey);
        waitForEntitiesToUpdate(su.getCompany(), entity);

        doEsTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + ".house", "house1", 1); // First log

        LogResultBean secondLog = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("isabella@sunnybell.com", entity.getMetaKey(), firstDate.plusDays(1), getSimpleMap("house", "house2"))).getLogResult();
        assertNotSame(0l, secondLog.getWhatLog().getEntityLog().getFortressWhen());
        Set<EntityLog> logs = trackService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(2, logs.size());
        entity = trackService.getEntity(su.getCompany(), metaKey);
        waitAWhile();
        Assert.assertEquals(secondLog.getWhatLog().getEntityLog().getFortressWhen(), entity.getFortressDateUpdated());
        doEsTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + ".house", "house2", 1); // replaced first with second

        // Test block
        mediationFacade.cancelLastLog(su.getCompany(), entity);
        logs = trackService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(1, logs.size());
        entity = trackService.getEntity(su.getCompany(), metaKey); // Refresh the entity
        waitAWhile();
        doEsTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + ".house", "house1", 1); // Cancelled, so Back to house1

        // Last change cancelled
        // DAT-96
        mediationFacade.cancelLastLog(su.getCompany(), entity);
        logs = trackService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(true, logs.isEmpty());
        doEsQuery(entity.getFortress().getIndexName(), "*", 0);

        entity = trackService.getEntity(su.getCompany(), metaKey); // Refresh the entity
        assertEquals("Search Key wasn't set to null", null, entity.getSearchKey());
    }

    @Test
    public void testWhatIndexingDefaultAttributeWithNGram() throws Exception {
        assumeTrue(runMe);
        logger.info("## testWhatIndexingDefaultAttributeWithNGram");
        SystemUser su = registerSystemUser("Romeo");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ngram"));
        EntityInputBean inputBean = new EntityInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setDescription("This is a description");

        TrackResultBean indexedResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), indexedResult.getMetaKey());

        Map<String, Object> what = getSimpleMap(EntitySearchSchema.WHAT_CODE, "AZERTY");
        what.put(EntitySearchSchema.WHAT_NAME, "NameText");
        entity = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), what)).getEntity();
        waitForEntitiesToUpdate(su.getCompany(), entity);

        String indexName = entity.getFortress().getIndexName();
        getMapping(indexName);

        // This is a description
        // 123456789012345678901

        // All text is converted to lowercase, so you have to search with lower
        doEsTermQuery(indexName, EntitySearchSchema.DESCRIPTION, "des", 1);
        doEsTermQuery(indexName, EntitySearchSchema.DESCRIPTION, "de", 0);
        doEsTermQuery(indexName, EntitySearchSchema.DESCRIPTION, "descripti", 1);
        doEsTermQuery(indexName, EntitySearchSchema.DESCRIPTION, "descriptio", 1);
        doEsTermQuery(indexName, EntitySearchSchema.DESCRIPTION, "this", 1);
        // ToDo: Figure out ngram details
        //doEsTermQuery(indexName, EntitySearchSchema.DESCRIPTION, "this is a description", 0);
    }

    @Test
    public void merge_SearchDocIsReWrittenAfterTagMerge() throws Exception {
        assumeTrue(runMe);
        //DAT-279
        logger.info("## merge_SearchDocIsReWritten");
        SystemUser su = registerSystemUser("merge_SimpleSearch");
        Fortress fortress = fortressService.registerFortress(su.getCompany(),
                new FortressInputBean("mergeSimpleSearch", false));

        TagInputBean tagInputA = new TagInputBean("TagA", "MoveTag", "rlxA");
        TagInputBean tagInputB = new TagInputBean("TagB", "MoveTag", "rlxB");

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "AAA");

        inputBean.addTag(tagInputA);
        inputBean.setContent(new ContentInputBean("blah", getRandomMap()));

        Entity entityA = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "BBB");
        inputBean.addTag(tagInputB);
        // Without content, a search doc will not be created
        inputBean.setContent(new ContentInputBean("blah", getRandomMap()));

        Entity entityB = mediationFacade.trackEntity(fortress, inputBean).getEntity();
        waitForInitialSearchResult(su.getCompany(), entityA.getMetaKey());
        waitForInitialSearchResult(su.getCompany(), entityB.getMetaKey());
        Tag tagA = tagService.findTag(su.getCompany(), tagInputA.getName());
        assertNotNull(tagA);
        Tag tagB = tagService.findTag(su.getCompany(), tagInputB.getName());
        assertNotNull(tagB);
        waitAWhile();

        doEsFieldQuery(fortress.getIndexName(), "@tag.rlxa.code", "taga", 1);
        doEsFieldQuery(fortress.getIndexName(), "@tag.rlxb.code", "tagb", 1);

        mediationFacade.mergeTags(su.getCompany(), tagA, tagB);
        waitAWhile();
        // We should not find anything against tagA",
        doEsFieldQuery(fortress.getIndexName(), "@tag.rlxa.code", "taga", 0);
        doEsFieldQuery(fortress.getIndexName(), "@tag.rlxb.code", "taga", 0);
        // Both docs will be against TagB
        doEsFieldQuery(fortress.getIndexName(), "@tag.rlxa.code", "tagb", 1);
        doEsFieldQuery(fortress.getIndexName(), "@tag.rlxb.code", "tagb", 1);

    }

    @Test
    public void amqp_TrackEntity () throws Exception {
        //assumeTrue(runMe);
        logger.info("## amqp_TrackEntity");
        SystemUser su = registerSystemUser("amqp_TrackEntity");
        Fortress fortress = fortressService.registerFortress(su.getCompany(),
                new FortressInputBean("amqp_TrackEntity", false));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "DocType", DateTime.now(), "AAA");

        inputBean.setContent(new ContentInputBean("blah", getRandomMap()));
        inputBean.setApiKey(su.getApiKey());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueBind("int.fd.track.queue", "int.fd.track.exchange", "int.fd.track.queue");

        channel.basicPublish("int.fd.track.exchange", "int.fd.track.queue", null, JsonUtils.getObjectAsJsonBytes(inputBean));
        channel.close();
        connection.close();
        waitAWhile();
        Entity entityA = trackService.findByCallerRef(fortress, "DocType", "AAA");
        assertNotNull(entityA);


    }

    private SystemUser registerSystemUser(String companyName, String userName) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        Company c = companyService.create(companyName);
        SystemUser su = regService.registerSystemUser(c, new RegistrationBean(companyName, userName));
        // creating company alters the schema that sometimes throws a heuristic exception.
        Thread.yield();
        return su;

    }


    private SystemUser registerSystemUser(String loginToCreate) throws Exception {
        setDefaultAuth();
        return registerSystemUser(company, loginToCreate);
    }

    @Test
    public void stressWithHighVolume() throws Exception {
        assumeTrue(false);// Suppressing this for the time being
        logger.info("## stressWithHighVolume");
        int runMax = 10, logMax = 10, fortress = 1;

        for (int i = 1; i < fortressMax + 1; i++) {
            deleteEsIndex(EntitySearchSchema.PREFIX + "monowai.bulkloada" + i);
            doEsQuery(EntitySearchSchema.PREFIX + "monowai.bulkloada" + i, "*", -1);
        }

        waitAWhile("Wait {} secs for index to delete ");

        SystemUser su = registerSystemUser("Gina");

        ArrayList<Long> list = new ArrayList<>();

        logger.info("FortressCount: " + fortressMax + " RunCount: " + runMax + " LogCount: " + logMax);
        logger.info("We will be expecting a total of " + (runMax * logMax * fortressMax) + " messages to be handled");

        StopWatch watch = new StopWatch();
        long totalRows = 0;

        DecimalFormat f = new DecimalFormat("##.000");

        watch.start();
        while (fortress <= fortressMax) {

            String fortressName = "bulkloada" + fortress;
            StopWatch fortressWatch = new StopWatch();
            fortressWatch.start();
            int run = 1;
            long requests = 0;

            Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean(fortressName));
            requests++;
            logger.info("Starting run for " + fortressName);
            while (run <= runMax) {
                boolean searchChecked = false;
                EntityInputBean aib = new EntityInputBean(iFortress.getName(), fortress + "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC" + run);
                TrackResultBean arb = mediationFacade.trackEntity(su.getCompany(), aib);
                String metaKey = arb.getEntity().getMetaKey();
                requests++;
                int log = 1;
                while (log <= logMax) {
                    Thread.yield();
                    createLog(su.getCompany(), metaKey, log);
                    Thread.yield(); // Failure to yield Getting a frustrating thread update problem causing
//                    IllegalStateException( "Unable to delete relationship since it is already deleted."
                    // under specifically stressed situations like this. We need to be able to detect and recover
                    // from the scenario
                    requests++;
                    if (!searchChecked) {
                        searchChecked = true;
                        requests++;
                        watch.suspend();
                        fortressWatch.suspend();
                        waitForInitialSearchResult(su.getCompany(), metaKey);
                        watch.resume();
                        fortressWatch.resume();
                    } // searchCheck done
                    log++;
                } // Logs created
                run++;
            } // Entities finished with
            fortressWatch.stop();
            double fortressRunTime = (fortressWatch.getTime()) / 1000d;
            logger.info("*** {} took {}  [{}] Avg processing time= {}. Requests per second {}",
                    iFortress.getName(),
                    fortressRunTime,
                    requests,
                    f.format(fortressRunTime / requests),
                    f.format(requests / fortressRunTime));
            watch.split();
            //splitTotals = splitTotals + fortressRunTime;
            totalRows = totalRows + requests;
            list.add(iFortress.getId());
            fortress++;
        }
        watch.stop();
        double totalTime = watch.getTime() / 1000d;
        logger.info("*** Processed {} requests. Data sets created in {} secs. Fortress avg = {} avg requests per second {}",
                totalRows,
                f.format(totalTime),
                f.format(totalTime / fortressMax),
                f.format(totalRows / totalTime));

        validateLogsIndexed(list, runMax, logMax);
        doSearchTests(runMax, list);
    }

    @Test
    public void simpleQueryEPWorksForImportedRecord() throws Exception {
        //assumeTrue(runMe);
        String searchFor = "testing";

        SystemUser su = registerSystemUser("Nik");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TestFortress"));

        ContentInputBean log = new ContentInputBean("mikeTest", new DateTime(), getSimpleMap("who", searchFor));
        EntityInputBean input = new EntityInputBean("TestFortress", "mikeTest", "Query", new DateTime(), "abzz");
        input.setContent(log);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        waitForInitialSearchResult(su.getCompany(), result.getEntity().getMetaKey());

        QueryParams q = new QueryParams(fortress).setSimpleQuery(searchFor);
        doEsQuery(EntitySearchSchema.PREFIX + "*", searchFor, 1);

        String qResult = runQuery(q);
        assertNotNull(qResult);
        assertTrue("Couldn't find a hit in the result [" + result + "]", qResult.contains("total\" : 1"));

    }

    @Test
    public void utfText() throws Exception {
        assumeTrue(runMe);
        Map<String, Object> json = getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("Utf8");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("UTF8-Test"));

        ContentInputBean log = new ContentInputBean("mikeTest", new DateTime(), json);
        EntityInputBean input = new EntityInputBean(fortress.getName(), "mikeTest", "Query", new DateTime(), "abzz");
        input.setContent(log);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        waitForEntitiesToUpdate(su.getCompany(), result.getEntity());
        doEsQuery(result.getEntity().getFortress().getIndexName(), json.get("Athlete").toString(), 1);
        waitAWhile();
    }

    private String runQuery(QueryParams queryParams) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHttpHeaders(null, "mike", "123");
        HttpEntity<QueryParams> requestEntity = new HttpEntity<>(queryParams, httpHeaders);

        try {
            return restTemplate.exchange("http://localhost:9081/fd-search/v1/query/", HttpMethod.POST, requestEntity, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Client tracking error {}", e.getMessage());
        }
        return null;
    }

    private String runMetaQuery(QueryParams queryParams) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHttpHeaders(null, "mike", "123");
        HttpEntity<QueryParams> requestEntity = new HttpEntity<>(queryParams, httpHeaders);

        try {
            return restTemplate.exchange("http://localhost:9081/fd-search/v1/query/metaKeys", HttpMethod.POST, requestEntity, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Client tracking error {}", e.getMessage());
        }
        return null;
    }

    private TrackResultBean createLog(Company company, String metaKey, int log) throws Exception {
        return mediationFacade.trackLog(company, new ContentInputBean("olivia@sunnybell.com", metaKey, new DateTime(), getSimpleMap("who", log)));
    }

    private void validateLogsIndexed(ArrayList<Long> list, int countMax, int expectedLogCount) throws Exception {
        logger.info("Validating logs are indexed");
        int fortress = 0;
        int count = 1;
        //DecimalFormat f = new DecimalFormat("##.000");
        while (fortress < fortressMax) {
            while (count <= countMax) {
                Entity entity = trackService.findByCallerRefFull(list.get(fortress), "CompanyNode", "ABC" + count);
                Set<EntityLog> logs = trackService.getEntityLogs(entity.getId());
                assertNotNull(logs);
                assertEquals("Wrong number of logs returned", expectedLogCount, logs.size());
                for (EntityLog log : logs) {
                    assertEquals("logId [" + log.getId() + "] changeId[" + log.getLog().getId() + "], event[ " + log.getLog().getEvent() + "]", true, log.isIndexed());
                }

                count++;
            }
            fortress++;
        }

    }

    private Entity waitForEntitiesToUpdate(Company company, Entity entity) throws Exception {
        return waitForInitialSearchResult(company, entity.getMetaKey());
    }

    private Entity waitForInitialSearchResult(Company company, String metaKey) throws Exception {
        // Looking for the first searchKey to be logged against the entity
        int i = 0;

        Entity entity = trackService.getEntity(company, metaKey);
        if (entity == null)
            return null;
        int timeout = 50;
        while (entity.getSearchKey() == null && i <= timeout) {
            Thread.sleep(20);
            entity = trackService.getEntity(company, metaKey);
            if (i > 20)
                waitAWhile("Sleeping for the entity to update {}");
            i++;
        }
        if (i > 22)
            logger.info("Wait for search got to [{}] for metaId [{}]", i, entity.getId());
        boolean searchWorking = entity.getSearchKey() != null;
        assertTrue("Search reply not received from fd-search", searchWorking);
        return entity;
    }

    private void doSearchTests(int auditCount, ArrayList<Long> list) throws Exception {
        int fortress;
        int searchLoops = 200;
        int search = 0;
        int totalSearchRequests = 0;
        logger.info("Performing Search Tests for {} fortresses", fortressMax);
        StopWatch watch = new StopWatch();
        watch.start();

        do {
            fortress = 0;
            do {
                int x = 1;
                do {
                    int random = (int) (Math.random() * ((auditCount) + 1));
                    if (random == 0)
                        random = 1;

                    Entity entity = trackService.findByCallerRefFull(list.get(fortress), "CompanyNode", "ABC" + random);
                    assertNotNull("ABC" + random, entity);
                    assertNotNull("Looks like fd-search is not sending back results", entity.getSearchKey());
                    EntityLog entityLog = logService.getLastLog(entity);
                    assertNotNull(entityLog);

                    assertTrue("fortress " + fortress + " run " + x + " entity " + entity.getMetaKey() + " - " + entityLog.getId(), entityLog.isIndexed());
                    String result = doEsTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.META_KEY, entity.getMetaKey(), 1, true);
                    totalSearchRequests++;
                    validateResultFieds(result);

                    x++;
                } while (x < auditCount);
                fortress++;
            } while (fortress < fortressMax);
            search++;
        } while (search < searchLoops);

        watch.stop();
        double end = watch.getTime() / 1000d;
        logger.info("Total Search Requests = " + totalSearchRequests + ". Total time for searches " + end + " avg requests per second = " + totalSearchRequests / end);
    }

    private ObjectMapper objectMapper = FlockDataJsonFactory.getObjectMapper();

    private void validateResultFieds(String result) throws Exception {
        JsonNode node = objectMapper.readTree(result);

        assertNotNull(node.get(EntitySearchSchema.CREATED));
        assertNotNull(node.get(EntitySearchSchema.WHO));
        assertNotNull(node.get(EntitySearchSchema.WHEN));
        assertNotNull(node.get(EntitySearchSchema.META_KEY));
        assertNotNull(node.get(EntitySearchSchema.DOC_TYPE));
        assertNotNull(node.get(EntitySearchSchema.FORTRESS));

    }

    private String doEsQuery(String index, String queryString) throws Exception {
        return doEsQuery(index, queryString, 1);
    }

    private String doEsQuery(String index, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        //waitAWhile();
        int runCount = 0, nbrResult;
        logger.debug("doEsQuery {}", queryString);
        JestResult jResult;
        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for ES Query to work");
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "              \"query\" : \"" + queryString + "\"" +
                    "           }\n" +
                    "      }\n" +
                    "}";

            //
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .build();

            jResult = esClient.execute(search);
            assertNotNull(jResult);
            if (expectedHitCount == -1) {
                assertEquals("Expected the index [" + index + "] to be deleted but message was [" + jResult.getErrorMessage() + "]", true, jResult.getErrorMessage().contains("IndexMissingException"));
                logger.debug("Confirmed index {} was deleted and empty", index);
                return null;
            }
            if (jResult.getErrorMessage() == null) {
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else {
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again
            }
            runCount++;
        } while (nbrResult != expectedHitCount && runCount < esTimeout);
        logger.debug("ran ES query - result count {}, runCount {}", nbrResult, runCount);

        assertNotNull(jResult);
        assertEquals(index + "\r\n" + jResult.getJsonString(), expectedHitCount, nbrResult);
        return jResult.getJsonString();

        //return result.getJsonString();
    }

    private String getMapping(String indexName) throws Exception {
        GetMapping mapping = new GetMapping.Builder()
                .addIndex(indexName)
                .build();

        JestResult jResult = esClient.execute(mapping);
        return jResult.getJsonString();
    }

    private String doEsTermQuery(String indexName, String metaKey, String metaKey1, int i) throws Exception {
        return doEsTermQuery(indexName, metaKey, metaKey1, i, false);
    }

    private String doEsTermQuery(String index, String field, String queryString, int expectedHitCount, boolean suppressLog) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;
        JestResult jResult;

        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for ES Query to work");
            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          term : {\n" +
                    "              \"" + field + "\" : \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .build();

            jResult = esClient.execute(search);
            String message = index + " - " + field + " - " + queryString + (jResult == null ? "[noresult]" : "\r\n" + jResult.getJsonString());
            assertNotNull(message, jResult);
            if (jResult.getErrorMessage() == null) {
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again

        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        if (!suppressLog) {
            logger.debug("ran ES Term Query - result count {}, runCount {}", nbrResult, runCount);
            logger.trace("searching index [{}] field [{}] for [{}]", index, field, queryString);
        }
        assertEquals(jResult.getJsonString(), expectedHitCount, nbrResult);
        if (nbrResult != 0) {
            return jResult.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
        } else {
            return null;
        }
    }

    /**
     * Use this carefully. Due to ranked search results, you can get more results than you expect. If
     * you are looking for an exact match then consider doEsTermQuery
     *
     * @param index            to search
     * @param field            field containing queryString
     * @param queryString      text to search for
     * @param expectedHitCount result count
     * @return query _source
     * @throws Exception if expectedHitCount != actual hit count
     */
    private String doEsFieldQuery(String index, String field, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;

        JestResult result;
        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for ES Query to work");

            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "              \"default_field\" : \"" + field + "\",\n" +
                    "              \"query\" : \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .build();

            result = esClient.execute(search);
            String message = index + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
            assertNotNull(message, result);
            assertNotNull(message, result.getJsonObject());
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        logger.debug("ran ES Field Query - result count {}, runCount {}", nbrResult, runCount);
        assertEquals("Unexpected hit count searching '" + index + "' for {" + queryString + "} in field {" + field + "}", expectedHitCount, nbrResult);
        if (nbrResult == 0)
            return "";
        else
            return result.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
    }

    public static HttpHeaders getHttpHeaders(final String apiKey, final String username, final String password) {

        return new HttpHeaders() {
            {
                if (username != null && password != null) {
                    String auth = username + ":" + password;
                    byte[] encodedAuth = Base64.encodeBase64(
                            auth.getBytes(Charset.forName("US-ASCII")));
                    String authHeader = "Basic " + new String(encodedAuth);
                    set("Authorization", authHeader);
                } else if (apiKey != null)
                    set("Api-Key", apiKey);
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", "UTF-8");
            }
        };

    }

    /**
     * Processing delay for threads and integration to complete. If you start getting sporadic
     * Heuristic exceptions, chances are you need to call this routine to give other threads
     * time to commit their work.
     * Likewise, waiting for results from fd-search can take a while. We can't know how long this
     * is so you can experiment on your own environment by passing in -DsleepSeconds=1
     *
     * @param milliseconds to pause for
     * @throws Exception
     */
    public static void waitAWhile(String message, long milliseconds) throws Exception {
        Thread.sleep(milliseconds);
        logger.debug(message, milliseconds / 1000d);
    }

    public static Map<String, Object> getSimpleMap(String key, Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }

    public static Map<String, Object> getRandomMap() {
        return getSimpleMap("Key", "Test" + System.currentTimeMillis());
    }

    public static Map<String, Object> getBigJsonText(int i) {
        Map<String, Object> map = getSimpleMap("Key", "Random");
        int count = 0;
        do {
            map.put("Key" + count, "Now is the time for all good men to come to the aid of the party");
            count++;
        } while (count < i);
        return map;
    }

    EntityLog waitForLogCount(Company company, Entity entity, int expectedCount) throws Exception {
        // Looking for the first searchKey to be logged against the entity
        int i = 0;
        int timeout = 100;
        int count = 0;
        //int sleepCount = 90;
        //logger.debug("Sleep Count {}", sleepCount);
        //Thread.sleep(sleepCount); // Avoiding RELATIONSHIP[{id}] has no property with propertyKey="__type__" NotFoundException
        while (i <= timeout) {
            Entity updatedEntity = trackService.getEntity(company, entity.getMetaKey());
            count = trackService.getLogCount(company, updatedEntity.getMetaKey());

            EntityLog log = trackService.getLastEntityLog(company, updatedEntity.getMetaKey());
            // We have at least one log?
            if (count == expectedCount)
                return log;
            Thread.yield();
            if (i > 20)
                waitAWhile("Waiting for the log to update {}");
            i++;
        }
        if (i > 22)
            logger.info("Wait for log got to [{}] for metaId [{}]", i,
                    entity.getId());
        throw new Exception(String.format("Timeout waiting for the requested log count of %s. Got to %s", expectedCount, count));
    }


}
