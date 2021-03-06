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

package org.flockdata.engine.dao;

import org.flockdata.dao.TrackEventDao;
import org.flockdata.model.ChangeEvent;
import org.flockdata.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 11:07 PM
 */
@Repository
public class TrackEventDaoNeo implements TrackEventDao {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    ChangeEventRepo eventRepo;

//    @Cacheable(value = "companyEvent", unless = "#result == null")
    private org.flockdata.model.ChangeEvent findEvent(Company company, String eventCode) {
        return eventRepo.findCompanyEvent(company.getId(), eventCode.toLowerCase());
    }

    @Override
    @Cacheable(value = "companyEvent", unless = "#result == null")
    public org.flockdata.model.ChangeEvent createEvent(Company company, String eventCode) {
        org.flockdata.model.ChangeEvent ev = findEvent(company, eventCode);
        if (ev == null ) {
            String cypher = "merge (event:_Event :Event{code:{code}, name:{name}}) " +
                    "with event " +
                    "match (c:FDCompany) where id(c) = {coId} " +
                    "merge (c)-[:COMPANY_EVENT]->(event) " +
                    "return event";

            Map<String, Object> params = new HashMap<>();
            params.put("code", eventCode.toLowerCase());
            params.put("name", eventCode);
            params.put("coId", company.getId());
            Iterable<Map<String, Object>> results = template.query(cypher, params);
            //((Node)row.get("event")).getPropertyKeys();
            for (Map<String, Object> row : results) {
                ev = template.projectTo(row.get("event"), ChangeEvent.class);
            }
//            ev = findEvent(company, eventCode);
        }

        return ev;
    }

    @Override
    public Set<org.flockdata.model.ChangeEvent> findCompanyEvents(Long id) {
        return eventRepo.findCompanyEvents(id);
    }
}
