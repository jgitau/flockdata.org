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

package org.flockdata.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flockdata.client.FdTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlockDataClientFactoryBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Value("${org.fd.client.default.fortress}")
    private String fortress;

    @Value("${org.fd.client.batchsize:1}")
    private int batch;

    @Value("${org.fd.engine.api}")
    private String serverName;

    @Value("${org.fd.client.login.user}")
    private String userName;

    @Value("${org.fd.client.login.pass}")
    private String password;

    @Autowired
    FdTemplate fdTemplate;


}
