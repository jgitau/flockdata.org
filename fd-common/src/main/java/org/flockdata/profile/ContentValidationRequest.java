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

package org.flockdata.profile;

import org.flockdata.profile.model.ContentModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Payload required to validate Content with a selection of data
 *
 * Created by mike on 14/04/16.
 */
public class ContentValidationRequest {

    ContentModel contentModel;
    Collection<Map<String,Object>> rows;

    public ContentValidationRequest(){}

    public ContentValidationRequest(ContentModel model) {
        this();
        this.contentModel = model;
    }

    public ContentValidationRequest(Map<String, Object> dataMap) {
        Collection<Map<String,Object>>rows = new ArrayList<>();
        rows.add(dataMap);
        this.rows = rows;
    }

    public ContentModel getContentModel() {
        return contentModel;
    }

    public Collection<Map<String, Object>> getRows() {
        return rows;
    }
}
