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

package org.flockdata.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.integration.VersionHelper;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.base.IndexMappingService;
import org.flockdata.search.configure.SearchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Service
@Configuration
public class SearchAdmin {
    @Autowired
    EntityChangeWriter engineDao;

    @Autowired
    IndexMappingService indexMappingService;

    @Autowired
    SearchConfig searchConfig;

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Autowired (required = false)
    VersionHelper versionHelper;
    public Map<String, Object> getHealth() {

        String version = "";
        if ( versionHelper!=null)
            version =versionHelper.getFdVersion();

        Map<String, Object> healthResults = new HashMap<>();
        healthResults.put("elasticsearch", engineDao.ping());
        healthResults.put("fd.search.version", version);

        String nodes = searchConfig.getTransportAddresses();
        if ( nodes !=null )
            healthResults.put("es.nodes", nodes);
        else
            healthResults.put("org.fd.search.es.transportOnly", false);

        healthResults.put("org.fd.search.es.settings", searchConfig.getEsDefaultSettings());
        healthResults.put("org.fd.search.es.mapping", searchConfig.getEsMappingPath());

        return healthResults;

    }

    @Bean
    public InfoEndpoint infoEndpoint() {
        return new InfoEndpoint(getHealth());
    }

    public void deleteIndexes (Collection<String>indexesToDelete){
        indexMappingService.deleteIndexes(indexesToDelete);
    }

    @PostConstruct
    void logStatus(){
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();
        try {
            ObjectWriter or = om.writerWithDefaultPrettyPrinter();
            logger.info("fd-search.status\r\n" + or.writeValueAsString(getHealth()));
        } catch (JsonProcessingException e) {

            logger.error("doHealth", e.getMessage());
        }
    }
}
