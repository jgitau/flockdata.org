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

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileDeserializer;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.registration.TagInputBean;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * tags from files with no headers
 * Created by mike on 27/01/15.
 */
public class TestCSVTagsWithDelimiter extends AbstractImport {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        String file = "/model/no-header.json";

        ContentModel contentModel = ContentModelDeserializer.getContentModel(file);
        ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/import/csv-tags-with-delimiter.json",contentModel);
        //assertEquals('|', params.getDelimiter());
        assertEquals(false, extractProfile.hasHeader());
        long rows = fileProcessor.processFile(extractProfile, "/data/no-header.txt");
        int expectedRows = 6;
        assertEquals(expectedRows, rows);
        List<TagInputBean> tagInputBeans = getFdBatcher().getTags();
        TestCase.assertEquals(expectedRows, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            assertFalse(tagInputBean.getCode().contains("|"));
            assertFalse(tagInputBean.getName().contains("|"));
            TestCase.assertEquals(1, tagInputBean.getTargets().size());
            assertFalse("non-persistent mapping was not ignored", tagInputBean.hasTagProperties());
            Collection<TagInputBean> targets = tagInputBean.getTargets().get("represents");
            for (TagInputBean represents : targets ) {
                assertFalse(represents.getCode().contains("|"));
                assertTrue(represents.isMustExist());

            }
        }

        // Check that the payload will serialize
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(tagInputBeans);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }

    }


}
