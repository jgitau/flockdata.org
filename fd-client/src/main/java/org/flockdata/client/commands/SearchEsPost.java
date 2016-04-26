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

import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.ClientConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

/**
 * Passthrough query to ES
 * Created by mike on 17/04/16.
 */
public class SearchEsPost extends AbstractRestCommand {

    private QueryParams queryParams;

    private Map<String,Object> result;

    public SearchEsPost(ClientConfiguration clientConfiguration, FdRestWriter fdRestWriter, QueryParams queryParams) {
        super(clientConfiguration, fdRestWriter);
        this.queryParams = queryParams;
    }


    public Map<String,Object> getResult() {
        return result;
    }

    @Override
    public String exec() {
        HttpEntity requestEntity = new HttpEntity<>(queryParams,httpHeaders);

        try {
            ParameterizedTypeReference<Map<String,Object>> responseType = new ParameterizedTypeReference<Map<String, Object>>() {};
            ResponseEntity<Map<String,Object>> response;
            response = restTemplate.exchange(url + "/api/v1/query/es", HttpMethod.POST, requestEntity, responseType);

            result = response.getBody();
        } catch (HttpClientErrorException e) {
            return e.getMessage();
        } catch (HttpServerErrorException e) {
            return e.getMessage();
        } catch (ResourceAccessException e) {
            return e.getMessage();
        }
        return null;// Everything worked
    }
}
