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

package org.flockdata.test.unit.client;

import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.tags.TagMapper;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 20/06/14
 * Time: 10:19 AM
 */
public class TestCSVConcepts {
    @org.junit.Test
    public void csvTags() throws Exception{
        ContentModel params = ContentModelDeserializer.getContentModel("/model/csv-tag-import.json");
        TagMapper mappedTag = new TagMapper();
        String[] headers= new String[]{"company_name", "device_name",  "device_code", "type",         "city", "ram", "tags"};
        String[] data = new String[]{  "Samsoon",      "Palaxy",       "PX",          "Mobile Phone", "Auckland", "32mb", "phone,thing,other"};

        Map<String,Object> json = mappedTag.setData(Transformer.convertToMap(headers, data, new ExtractProfileHandler(params)), params);
        assertNotNull (json);
        Map<String, Collection<TagInputBean>> allTargets = mappedTag.getTargets();
        assertNotNull(allTargets);
        assertEquals(3, allTargets.size());
        assertEquals("Should have overridden the column name of device_name", "Device", mappedTag.getLabel());
        assertEquals("Name value should be that of the defined column", "Palaxy", mappedTag.getName());
        assertEquals("PX", mappedTag.getCode());
        assertEquals("Device", mappedTag.getLabel());
        assertNotNull(mappedTag.getProperties().get("RAM"));

        TagInputBean makes = allTargets.get("makes").iterator().next();
        assertEquals("Manufacturer", makes.getLabel());
        assertEquals("Nested City tag not found", 1, makes.getTargets().size());
        TagInputBean city = makes.getTargets().get("located").iterator().next();
        assertEquals("Auckland", city.getCode());


        assertEquals("Samsoon", makes.getCode());
        assertEquals("Should be using the column name", "type", allTargets.get("of-type").iterator().next().getLabel());
        assertEquals(3, allTargets.get("mentions").size());

    }
}
