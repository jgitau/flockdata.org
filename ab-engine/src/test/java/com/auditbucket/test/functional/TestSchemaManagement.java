package com.auditbucket.test.functional;

import com.auditbucket.audit.bean.MetaInputBean;
import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.fortress.endpoint.FortressEP;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 3/04/14
 * Time: 9:54 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional

public class TestSchemaManagement {
    @Autowired
    TrackEP trackEP;

    @Autowired
    FortressEP fortressEP;

    @Autowired
    RegistrationEP registrationEP;

    @Autowired
    private Neo4jTemplate template;

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authA);
        Neo4jHelper.cleanDb(template);
    }

    private String uid = "mike@monowai.com";
    private Authentication authA = new UsernamePasswordAuthenticationToken(uid, "user1");
    private String monowai = "Monowai";
    private String mike = "test@ab.com";

    @Test
    public void documentTypesTrackedPerFortress() throws Exception {
        Company c = registrationEP.register(new RegistrationBean(monowai, mike, "bah")).getBody().getCompany();
        Fortress fortressA = fortressEP.registerFortress(new FortressInputBean("auditTestA", true), c.getApiKey()).getBody();
        Fortress fortressB = fortressEP.registerFortress(new FortressInputBean("auditTestB", true), c.getApiKey()).getBody();

        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyA = trackEP.trackHeader(inputBean, c.getApiKey(), c.getApiKey()).getBody().getMetaKey();

        inputBean = new MetaInputBean(fortressB.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyB = trackEP.trackHeader(inputBean, c.getApiKey(), c.getApiKey()).getBody().getMetaKey();

        assertFalse(metaKeyA.equals(metaKeyB));
        // There should be a doc type per fortress and it should have the same Id.
        // ToDo: fortress actions based on fortress api-key
        Collection<DocumentType> docTypesA = fortressEP.getDocumentTypes (fortressA.getName(), c.getApiKey(), c.getApiKey());
        assertEquals(1, docTypesA.size());

        Collection<DocumentType> docTypesB = fortressEP.getDocumentTypes (fortressB.getName(), c.getApiKey(), c.getApiKey());
        assertEquals(1, docTypesB.size());

        // Should be the same key
        assertEquals(docTypesA.iterator().next().getId(), docTypesB.iterator().next().getId());
    }
}
