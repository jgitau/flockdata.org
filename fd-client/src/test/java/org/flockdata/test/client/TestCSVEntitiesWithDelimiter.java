/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.flockdata.client.Configure;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by mike on 27/01/15.
 */
public class TestCSVEntitiesWithDelimiter extends AbstractImport {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/profile/no-header-entities.json");
        ClientConfiguration configuration = Configure.getConfiguration(file);
        assertNotNull(configuration);
        configuration.setLoginUser("test");

        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/no-header-entities.json");
        //assertEquals('|', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        long rows = fileProcessor.processFile(params, "/data/no-header.txt", getFdWriter(), null, configuration);
        int expectedRows = 6;
        assertEquals(expectedRows, rows);

        List<EntityInputBean>entities = getFdWriter().getEntities();
        for (EntityInputBean entity : entities) {
            assertNotNull ( "Remapping column name to target", entity.getContent().getData().get("institution"));
            // DAT-528
            assertNull("Column 11 is flagged as false for persistence", entity.getContent().getData().get("11"));

            assertEquals(3, entity.getTags().size());
            List<TagInputBean> tagInputBeans = entity.getTags();
            for (TagInputBean tagInputBean : tagInputBeans) {
                if( tagInputBean.getLabel().equals("Year")) {
                    assertEquals("2012", tagInputBean.getCode());
                } else  if ( tagInputBean.getLabel().equals("Institution"))  {
                    assertFalse(tagInputBean.getCode().contains("|"));
                    assertFalse(tagInputBean.getName().contains("|"));
                    assertEquals("Institution", tagInputBean.getLabel());
                    TestCase.assertEquals(1, tagInputBean.getTargets().size());
                    Collection<TagInputBean> targets = tagInputBean.getTargets().get("represents");
                    for (TagInputBean represents : targets) {
                        assertFalse(represents.getCode().contains("|"));
                        assertTrue(represents.isMustExist());
                    }
                } else if ( tagInputBean.getLabel().equals("ZipCode")){
                    assertEquals("Data type was not preserved as a string", "01", tagInputBean.getCode());
                }
            }

        }
        // Check that the payload will serialize
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(entities);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }

    }


}
