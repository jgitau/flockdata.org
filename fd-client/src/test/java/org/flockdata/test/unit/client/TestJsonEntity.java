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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.model.Company;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.json.JsonEntityMapper;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 10/12/14
 * Time: 1:43 PM
 */
public class TestJsonEntity extends AbstractImport{

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(TestCsvEntity.class);

    @Test
    public void entity_JsonStructure() throws Exception {
        ContentModel params = ContentModelDeserializer.getContentModel("/model/gov.json");
        JsonEntityMapper entity = new JsonEntityMapper();

        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream file = TestCsvEntity.class.getResourceAsStream("/model/object-example.json");
            JsonNode theMap = mapper.readTree(file);
            entity.setData(theMap, params);
            assertEquals(11, entity.getTags().size());
            assertEquals("hr4015-113", entity.getCode());
            assertEquals("hr", entity.getDocumentType().getName());
            assertNotNull(entity.getName());
            for (TagInputBean tagInputBean : entity.getTags()) {
                if ( tagInputBean.getEntityTagLinks().get("sponsors")!=null ) {
                    assertNotNull(tagInputBean.getTargets().get("located"));
                    Collection<TagInputBean> states = tagInputBean.getTargets().get("located");
                    for (TagInputBean state : states) {
                        assertEquals("TX", state.getCode());
                        assertNotNull(state.getTargets().get("represents"));
                    }
                }

                else if ( tagInputBean.getEntityTagLinks().get("cosponsors")!=null )
                    //assertEquals(5,  );
                    logger.info("Validate CoSponsor {}", tagInputBean.toString());

            }
        } catch (IOException e) {

            logger.error("Error writing exceptions", e);
            throw new RuntimeException("IO Exception ", e);
        }



    }

    @Test
    public void object_ImportJsonEntity() throws Exception{
        ContentModel model= ContentModelDeserializer.getContentModel("/model/gov.json");
        ExtractProfile extractProfile = new ExtractProfileHandler(model);
        extractProfile.setContentType(ExtractProfile.ContentType.JSON);

        model.setFortress(new FortressInputBean("testing"));

        Company company = Mockito.mock(Company.class);
        company.setName("Testing");
        long rows = fileProcessor.processFile(extractProfile, "/model/object-example.json");
        assertEquals("Should have processed the file as a single JSON object", 1, rows);
    }

    @Test
    public void array_ImportJsonEntities() throws Exception{
        ContentModel model= ContentModelDeserializer.getContentModel("/model/gov.json");
        ExtractProfile extractProfile = new ExtractProfileHandler(model);
        extractProfile.setContentType(ExtractProfile.ContentType.JSON);
        model.setFortress(new FortressInputBean("testing"));
//        profile.setTagOrEntity(ContentModel.DataType.ENTITY);

        Company company = Mockito.mock(Company.class);
        company.setName("Testing");
        long rows = fileProcessor.processFile(extractProfile, "/model/gov-array-example.json");
        assertEquals("Should have processed the file as an array of JSON objects", 1, rows);
    }



}
