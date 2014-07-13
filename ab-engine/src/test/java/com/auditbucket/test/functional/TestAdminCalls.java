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

import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 19/05/14
 * Time: 3:46 PM
 */
@Transactional
public class TestAdminCalls extends TestEngineBase {

    @Autowired
    private MediationFacade mediationFacade;

    private Logger logger = LoggerFactory.getLogger(TestTrack.class);
    private String monowai = "Monowai";
    private String mike = "mike";

    @Test
    public void deleteFortressWithHeadersAndTagsOnly() throws Exception {

        SystemUserResultBean su = regEP.registerSystemUser(new RegistrationBean(monowai, mike)).getBody();
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        TagInputBean tagInputBean = new TagInputBean("DeleteTest", "NamedTag", "deltest");
        inputBean.addTag(tagInputBean);


        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        inputBean.addTag(tagInputBean);

        mediationFacade.createHeader(inputBean, null);

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            adminEP.purgeFortress(fo.getName(), null, null);
            fail ("An authorisation exception should have been thrown");
        } catch ( Exception e ){
            // This is good
        }
        setSecurity();
        adminEP.purgeFortress(fo.getName(),null, null);
        assertNull( trackService.getHeader(ahKey));
        assertNull(fortressService.findByName(fo.getName()));
    }

    @Test
    public void deleteFortressPurgesHeaderAndLogs() throws Exception {

        SystemUserResultBean su = regEP.registerSystemUser(new RegistrationBean(monowai, mike)).getBody();
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 1}"));

        assertEquals(2, trackService.getLogCount(resultBean.getMetaKey()));

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            adminEP.purgeFortress(fo.getName(), null, null);
            fail ("An authorisation exception should have been thrown");
        } catch ( Exception e ){
            // This is good
        }
        setSecurity();
        adminEP.purgeFortress(fo.getName(), su.getApiKey(), su.getApiKey());
        assertNull( trackService.getHeader(ahKey));
        assertNull(fortressService.findByName(fo.getName()));
    }

    @Test
    public void deleteFortressPurgesDataWithTags() throws Exception {

        SystemUserResultBean su = regEP.registerSystemUser(new RegistrationBean(monowai, mike)).getBody();
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        TagInputBean tagInputBean = new TagInputBean("DeleteTest", "NamedTag", "deltest");
        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));

        inputBean.setCallerRef("123abc");
        inputBean.setLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        mediationFacade.createHeader(inputBean, null);

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            adminEP.purgeFortress(fo.getName(), null, null);
            fail ("An authorisation exception should have been thrown");
        } catch ( Exception e ){
            // This is good
        }
        setSecurity();
        adminEP.purgeFortress(fo.getName(), su.getApiKey(), su.getApiKey());
        assertNull( trackService.getHeader(ahKey));
        assertNull(fortressService.findByName(fo.getName()));


    }
    @Test
    public void purgeFortressClearsDown() throws Exception{
        setSecurity();
        SystemUserResultBean su = regEP.registerSystemUser(new RegistrationBean(monowai, mike)).getBody();
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("purgeFortressClearsDown", true));

        MetaInputBean trackBean = new MetaInputBean(fortress.getName(), "olivia@ast.com", "CompanyNode", null, "abc2");
        trackBean.addTag(new TagInputBean("anyName", "rlx"));
        trackBean.addTag(new TagInputBean("otherName", "rlxValue").setReverse(true));
        LogInputBean logBean = new LogInputBean("me", DateTime.now(), json );
        trackBean.setLog(logBean);
        String resultA = mediationFacade.createHeader(trackBean, null).getMetaKey();

        assertNotNull(resultA);

        trackBean = new MetaInputBean(fortress.getName(), "olivia@ast.com", "CompanyNode", null, "abc3");
        trackBean.addTag(new TagInputBean("anyName", "rlx"));
        trackBean.addTag(new TagInputBean("otherName", "rlxValue").setReverse(true));
        logBean = new LogInputBean("me", DateTime.now(), json );
        trackBean.setLog(logBean);

        String resultB = mediationFacade.createHeader(trackBean, su.getApiKey()).getMetaKey();

        Collection<String> others = new ArrayList<>();
        others.add(resultB);
        trackEP.putCrossReference(resultA, others, "rlxName", su.getApiKey(), su.getApiKey());

        others = new ArrayList<>();
        others.add(resultA);
        trackEP.putCrossReference(resultB, others, "rlxNameB", su.getApiKey(), su.getApiKey());

        mediationFacade.purge(fortress.getName(), su.getApiKey());
        assertNull ( trackService.getHeader(resultA) );
        assertNull ( trackService.getHeader(resultB) );

    }

}