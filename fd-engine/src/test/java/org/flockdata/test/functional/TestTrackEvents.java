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

import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.test.utils.Helper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.ChangeEvent;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.model.Log;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.Assert.*;


/**
 * User: Mike Holdsworth
 * Since: 6/09/13
 */
@Transactional
public class TestTrackEvents extends EngineBase {

    @Test
    public void noDuplicateEventsForACompany() throws Exception {
        SystemUser su = registerSystemUser("noDuplicateEventsForACompany", mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest"));
        Company company = fortressA.getCompany();
        assertNotNull(company);
        String eventName = "DuplicateNotAllowed";
        ChangeEvent event = trackEventService.processEvent(eventName);

        assertNotNull(event);
        Long existingId = event.getId();
        assertEquals(eventName, event.getName());
        assertEquals(eventName.toLowerCase(), event.getCode());
        //assertEquals(company.getId(), event.getCompany().getId());
        Set<ChangeEvent> events = trackEventService.getCompanyEvents(company.getId());
        assertEquals(1, events.size());
        event = trackEventService.processEvent(eventName);
        assertEquals(existingId, event.getId());
        assertEquals(1, events.size());

    }

    /**
     * Ensures that the event type gets set to the correct default for create and update.
     */
    @Test
    public void defaultEventTypesAreHandled() throws Exception {

        SystemUser su = registerSystemUser("defaultEventTypesAreHandled", mike_admin);
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("defaultEventTypes", true));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String metaKey = resultBean.getMetaKey();
        assertNotNull(metaKey);

        Entity entity = trackService.getEntity(su.getCompany(), metaKey);
        assertNotNull(entity.getDocumentType());

        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", metaKey, new DateTime(), Helper.getRandomMap()));

        EntityLog when = trackService.getLastEntityLog(su.getCompany(), metaKey);
        assertNotNull(when);
        assertEquals(Log.CREATE, when.getLog().getEvent().getName()); // log event default
        assertEquals(Log.CREATE.toLowerCase(), when.getLog().getEvent().getName().toLowerCase()); // log event default

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", metaKey, new DateTime(), Helper.getRandomMap()));
        EntityLog whenB = trackService.getLastEntityLog(su.getCompany(), metaKey);
        assertNotNull(whenB);

        assertFalse(whenB.equals(when));
        assertNotNull(whenB.getLog().getEvent());
        assertEquals(Log.UPDATE, whenB.getLog().getEvent().getName());  // log event default
    }
}