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

package org.flockdata.test.engine.services;

import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * Tests that deal with log changes - correct number, detecting changes etc
 *
 * User: mike
 * Date: 30/10/14
 * Time: 9:51 AM
 */
public class TestLogCounts extends EngineBase {
    private Logger logger = LoggerFactory.getLogger(TestEntityTrack.class);

    @Test
    public void historic_BackFillingLogsWontCreateDuplicates() throws Exception {
        // DAT-267
        logger.debug("### historic_BackFillingLogsDontCreateDuplicates");
        SystemUser su = registerSystemUser("historic_BackFillingLogsDontCreateDuplicates");
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("historic_BackFillingLogsWontCreateDuplicates", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP, new DocumentTypeInputBean("poppy"));

        DateTime today = DateTime.now();
        inputBean.setContent(new ContentInputBean("poppy", today, EntityContentHelper.getSimpleMap("name", "a")));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertEquals("Exactly 1 log expected", 1, entityService.getLogCount(su.getCompany(), result.getEntity().getKey()));
        DateTime yesterday = today.minusDays(1);
        inputBean.setKey(result.getEntity().getKey());
        inputBean.setContent(new ContentInputBean("poppy", yesterday, EntityContentHelper.getSimpleMap("name", "b")));
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertEquals("Exactly 2 logs expected", 2, entityService.getLogCount(su.getCompany(), result.getEntity().getKey()));

        mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertEquals("Back filling an identical log should not create a new one", 2, entityService.getLogCount(su.getCompany(), result.getEntity().getKey()));

        // Insert a log for which only the FortressDate time has changed. Should not create a new log
        DateTime yesterdayMoreRecent = yesterday.plusHours(1);
        inputBean.setContent(new ContentInputBean("poppy", yesterdayMoreRecent, EntityContentHelper.getSimpleMap("name", "b")));
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertEquals("Back filling an identical log should not create a new one", 2, entityService.getLogCount(su.getCompany(), result.getEntity().getKey()));
    }

    @Test
    public void historic_AttachmentsWithSameChecksumWontCreateDuplicates() throws Exception {
        // DAT-267
        logger.debug("### historic_AttachmentsWithSameChecksumWontCreateDuplicates");
        SystemUser su = registerSystemUser("historic_AttachmentsWithSameChecksumWontCreateDuplicates");
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("historic_AttachmentsWithSameChecksumWontCreateDuplicates", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP, new DocumentTypeInputBean("poppy" ));

        DateTime today = DateTime.now();
        ContentInputBean cib = new ContentInputBean("poppy", today);
        cib.setAttachment(EntityContentHelper.getPdfDoc(), "pdf", "test.pdf");
        inputBean.setContent(cib);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertEquals("Exactly 1 log expected", 1, entityService.getLogCount(su.getCompany(), result.getEntity().getKey()));

        result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertEquals("Tracking the same content should not create a new log", 1, entityService.getLogCount(su.getCompany(), result.getEntity().getKey()));

        DateTime yesterday = today.minusDays(1);
        inputBean.setKey(result.getEntity().getKey());
        cib = new ContentInputBean("poppy", yesterday);
        cib.setAttachment(EntityContentHelper.getPdfDoc(), "pdf", "test.pdf");
        inputBean.setContent(cib);
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertEquals("Same content but different fortress date should not create  new log", 1, entityService.getLogCount(su.getCompany(), result.getEntity().getKey()));

    }

//    @Test
//    public void reprocess_ChangingWhatOnSameDatetimeCreatesLogs() throws Exception {
//        SystemUser su = registerSystemUser("fixing");
//        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fixing", true));
//        assertFalse(fortress.isSearchEnabled());
//        String code = UUID.randomUUID().toString();
//
//        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), code);
//        ContentInputBean contentInputBean = new ContentInputBean("mike", new DateTime(), Helper.getSimpleMap("col", 123));
//        inputBean.setContent(contentInputBean);
//        TrackResultBean result = mediationFacade.trackEntity(inputBean, su.getApiKey());
//        Entity entity = result.getEntity();
//        waitForFirstLog(su.getCompany(), entity);
//        contentInputBean.setData(Helper.getSimpleMap("col", 124));
//        mediationFacade.trackEntity(inputBean, su.getApiKey());
//        // See LogNode.logKey (setCurrentLog)
//        assertEquals("All content dates the same except What - got wrong count", 2, trackService.getEntityLogs(entity.getId()).size());
//
//    }
}
