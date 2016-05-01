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
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Locate a tag
 * Created by mike on 17/04/16.
 */
public class TrackEntityPost extends AbstractRestCommand  {

    private EntityInputBean entityInputBean;

    private TrackRequestResult result;

    public TrackEntityPost(ClientConfiguration clientConfiguration, FdRestWriter fdRestWriter, EntityInputBean entityInputBean) {
        super(clientConfiguration, fdRestWriter);
        this.entityInputBean = entityInputBean;
    }

    public TrackRequestResult getResult() {
        return result;
    }

    @Override
    public String exec() {
        HttpEntity<EntityInputBean> requestEntity = new HttpEntity<>(entityInputBean, httpHeaders);

        try {
            ResponseEntity<TrackRequestResult> restResult = restTemplate.exchange(url+"/api/v1/track/", HttpMethod.POST, requestEntity, TrackRequestResult.class);
            result = restResult.getBody();
            return null;
        } catch (HttpClientErrorException |HttpServerErrorException e) {

            return e.getMessage();
        }
    }
}