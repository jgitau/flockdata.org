/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.client;

import org.flockdata.client.commands.Ping;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.registration.RegistrationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

/**
 * General importer with support for CSV and XML parsing. Interacts with AbRestClient to send
 * information via a RESTful interface
 * <p>
 * Will send information to FlockData as either tags or track information.
 * <p>
 * You should extend EntityInputBean or TagInputBean and implement XMLMappable or DelimitedMappable
 * to massage your data prior to dispatch to FD.
 * <p>
 * Parameters:
 * -s=http://localhost:8080
 * <p>
 * quoted string containing "file,DelimitedClass,BatchSize"
 * "./path/to/file/cow.csv,org.flockdata.health.Countries,200"
 * <p>
 * if BatchSize is set to -1, then a simulation only is run; information is not dispatched to the server.
 * This is useful to debug the class implementing Delimited
 *
 * @see RegistrationBean
 * @see org.flockdata.registration.SystemUserResultBean
 * @see ClientConfiguration
 * <p>
 * User: Mike Holdsworth
 * Since: 13/10/13
 */
@Profile("fd-ping")
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.flockdata.authentication", "org.flockdata.shared", "org.flockdata.client"})
public class PingRunner {

    private Logger logger = LoggerFactory.getLogger(PingRunner.class);


    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired
    private FdTemplate fdTemplate;

    @PostConstruct
    void register() {
        Ping pingCmd = new Ping(fdTemplate);
        pingCmd.exec();
        logger.info("FlockData endpoint [{}] responded with [{}]", clientConfiguration.getServiceUrl(), pingCmd.error()!=null?pingCmd.error():pingCmd.result());

    }


}