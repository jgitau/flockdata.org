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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.model.EntityTag;
import org.flockdata.registration.TagResultBean;

import java.util.Map;

/**
 * Created by mike on 19/02/16.
 */
public class EntityTagResult {


    private  GeoDataBeans geoData;
    private Map<String,Object> props;
    private TagResultBean tag;
    private String relationship    ;

    EntityTagResult(){}

    public EntityTagResult(EntityTag logTag) {
        this();
        logTag.getTag();
        props = logTag.getProperties();
        this.tag = new TagResultBean(logTag.getTag());
        this.relationship = logTag.getRelationship();
        this.geoData = logTag.getGeoData();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public GeoDataBeans getGeoData() {
        return geoData;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Object> getProps() {
        return props;
    }

    public TagResultBean getTag() {
        return tag;
    }

    public String getRelationship() {
        return relationship;
    }
}
