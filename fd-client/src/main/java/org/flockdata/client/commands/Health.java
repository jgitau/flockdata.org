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

package org.flockdata.client.commands;

import org.flockdata.client.FdTemplate;
import org.flockdata.helper.JsonUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.util.Map;

/**
 * HealthCheck to a service to see if it can see other services
 *
 * @tag Command, Administration
 * @author mholdsworth
 * @since 4/04/2016
 */

public class Health extends AbstractRestCommand {

    Map<String,Object> result;

    public Health(FdTemplate restWriter) {
        super(restWriter);
    }

    public Map<String,Object> result() {
        return result;
    }

    @Override    // Command
    public Health exec() {
        String exec = getUrl() + "/api/v1/admin/health/";
        result=null; error =null;
        HttpEntity requestEntity = new HttpEntity<>(fdTemplate.getHeaders());
        try {
            ResponseEntity<String> response;
            response = fdTemplate.getRestTemplate().exchange(exec, HttpMethod.GET, requestEntity, String.class);
            result = JsonUtils.toMap(response.getBody());
        } catch (HttpClientErrorException e) {
            if (e.getMessage().startsWith("401"))
                error = "auth";
            else
                error = e.getMessage();
        } catch (HttpServerErrorException | ResourceAccessException | IOException e) {
            error = e.getMessage();
        }
        return this;
    }
}
