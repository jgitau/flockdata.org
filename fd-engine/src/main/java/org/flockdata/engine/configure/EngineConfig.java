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

package org.flockdata.engine.configure;

import org.flockdata.authentication.FdRoles;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.integration.search.SearchPingRequest;
import org.flockdata.engine.integration.search.SearchPingRequest.PingGateway;
import org.flockdata.engine.integration.store.StorePingRequest;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.VersionHelper;
import org.flockdata.model.Company;
import org.flockdata.store.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

import static org.flockdata.authentication.FdRoles.FD_ROLE_ADMIN;
import static org.flockdata.authentication.FdRoles.FD_ROLE_USER;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Service(value = "engineConfig")
@Transactional
@Configuration
public class EngineConfig implements PlatformConfig {

    @Value("${org.fd.engine.system.storage:RIAK}")
    private String storeEngine; // The default store to write to IF a fortress allows it
                                // By default, storage engine services are not disabled
                                // and current state content is retrieved from ElasticSearch

    @Value("${org.fd.engine.fortress.store:false}")
    private Boolean storeEnabled;

    @Value("${org.fd.engine.system.api:api}")
    private String apiBase ;

    @Value("${spring.cloud.config.discovery.enabled:false}")
    Boolean discoveryEnabled;

    @Autowired (required = false)
    PingGateway pingSearchGateway;

    @Autowired (required = false)
    SearchPingRequest searchPingRequest;

    @Autowired
    StorePingRequest.StorePingGateway storePingGateway;

    @Autowired (required = false)
    AmqpRabbitConfig rabbitConfig;


    @Value("${org.fd.engine.system.multiTenanted:false}")
    private Boolean multiTenanted = false;
    private boolean conceptsEnabled = true;
    @Value("${org.fd.engine.system.constraints:true}")
    private boolean systemConstraints = true;
    private boolean testMode;

    @Value("${org.fd.engine.fortress.search:true}")
    private boolean searchEnabled = true;

    @Value("${org.fd.search.api:http://localhost:8081}")
    private String fdSearch;

    @Value("${org.fd.store.api:http://localhost:8082}")
    private String fdStoreUrl;

    @Value ("${eureka.client.serviceUrl.defaultZone}")
    private String eurekaUrl;

    @Override
    public String getFdStore() {

        return fdStoreUrl+"/api";
    }

    @Override
    public PlatformConfig setSearchRequiredToConfirm(boolean b) {
        this.requireSearchToConfirm = b;
        return this;
    }

    @Value("${org.fd.engine.system.timings:false}")
    private boolean timing = false;

    // By default, we only require a reply if this is being indexed for the first time
    @Value("${org.fd.engine.search.update:true}")
    Boolean requireSearchToConfirm = false;

    public void setStoreEnabled(boolean storeEnabled) {
        this.storeEnabled = storeEnabled;
    }

    public void setSearchEnabled(String searchEnabled) {
        this.searchEnabled =Boolean.parseBoolean(searchEnabled);
    }


    public Boolean isSearchRequiredToConfirm() {
        return requireSearchToConfirm;
    }

    /**
     *
     * @return is fd-storeEngine part of the data processing pipeline?
     */
    public Boolean storeEnabled() {
        return this.storeEnabled;
    }

    public Boolean isSearchEnabled() {
        return searchEnabled;
    }

    /**
     * Should be disabled for testing purposes
     *
     * @param conceptsEnabled if true, concepts will be created in a separate thread when entities are tracked
     */
    @Override
    @Value("${org.fd.engine.system.concepts:true}")
    public Boolean setConceptsEnabled(boolean conceptsEnabled) {
        Boolean previous = conceptsEnabled;
        this.conceptsEnabled =conceptsEnabled;
        return previous  ;
    }

    public Store setStore(Store store) {
        Store previous = Store.valueOf(storeEngine);
        this.storeEngine = store.name();
        return previous;
    }

    @Override
    public Store store() {
        return Store.valueOf(storeEngine.toUpperCase());
    }

    @Override
    public String getTagSuffix(Company company) {
        if (company == null)
            return "";
        return (isMultiTenanted() ? company.getCode() : "");
    }

    @Secured({FD_ROLE_ADMIN, FD_ROLE_USER})
    public Map<String, String> getHealthAuth() {
        return getHealth();
    }

    @Bean
    public InfoEndpoint infoEndpoint() {
        return new InfoEndpoint(getHealth());
    }

    @Autowired (required = false)
    VersionHelper versionHelper;
    /**
     * Only users with a pre-validated api-key should be calling this
     *
     * @return system configuration details
     */
    @Override
    public Map<String, String> getHealth() {
        String version = "";
        if ( versionHelper !=null )
            version = versionHelper.getFdVersion();
        Map<String, String> healthResults = new TreeMap<>();

        healthResults.put("fd.version", version);

        String esPingResult;
        try {
            String esPing = "Disabled";
            if ( searchPingRequest != null )
                esPing = pingSearchGateway.ping();
            esPingResult = (esPing == null || !esPing.equals("pong") ? esPing : "Ok");
        } catch (Exception ce) {
            esPingResult = "!Unreachable ";
            if (ce.getCause() != null)
                esPingResult = esPingResult + ce.getCause().getMessage();
        }
        healthResults.put("fd-search",  esPingResult + " on " +fdSearch);

        try {
            String esPing = storePingGateway.ping();
            esPingResult = (esPing == null || !esPing.equals("pong") ? "Problem" : "Ok");
        } catch (Exception ce) {
            esPingResult = "!Unreachable ";
            if (ce.getCause() != null)
                esPingResult = esPingResult + ce.getCause().getMessage();
        }
        healthResults.put("fd-store", esPingResult + " on " +fdStoreUrl);
        healthResults.put("fd.store.engine", storeEngine);
        healthResults.put("fd.store.enabled", storeEnabled().toString());
        if ( rabbitConfig !=null ) {
            healthResults.put("rabbit.host", rabbitConfig.getHost());
            healthResults.put("rabbit.port", rabbitConfig.getPort().toString());
            healthResults.put("rabbit.user", rabbitConfig.getUser());
        }
        healthResults.put("eureka.client.serviceUrl.defaultZone", eurekaUrl);
        healthResults.put("spring.cloud.config.discovery.enabled", discoveryEnabled.toString());

        return healthResults;

    }

    @Override
    public boolean isMultiTenanted() {
        return multiTenanted;
    }

    @Override
    public void setMultiTenanted(boolean multiTenanted) {
        this.multiTenanted = multiTenanted;
    }

    @Override
    public boolean isConceptsEnabled() {
        return conceptsEnabled;
    }

    public boolean isTestMode() {
        return testMode;
    }

    @Override
    @PreAuthorize(FdRoles.EXP_EITHER)
    public String authPing() {
        return "pong";
    }

    @Override
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public String getFdSearch() {
        return fdSearch +"/api";
    }

}
