/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.search.functional;

import org.flockdata.helper.TagHelper;
import org.flockdata.model.Alias;
import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.model.Tag;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.FdSearch;
import org.flockdata.search.model.TagSearchChange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mike on 16/05/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(FdSearch.class)
public class TestIndexTagChanges extends ESBase {
    @Test
    public void testSimpleTagIndexes() throws Exception {
        Company company = new Company("testCompany");
        Fortress fortress = new Fortress(new FortressInputBean("testFortress"), company);
        TagInputBean tagInputBean = new TagInputBean("aCode", "SomeLabel");
        tagInputBean.setProperty("user property", "UDFValue");

        Tag tag = new Tag(tagInputBean);
        tag.setName("A Name To Find");

        String key = TagHelper.parseKey(tagInputBean.getCode()+"Alias");
        Alias alias = new Alias(tagInputBean.getLabel(), new AliasInputBean(tagInputBean.getCode()+"Alias", "someAliasDescription"),key, tag);
        tag.addAlias( alias);

        String indexName = indexManager.getIndexRoot(company, tag);
        assertNotNull (indexName);
        deleteEsIndex(indexName);
        assertTrue(indexName.contains(".tags."));
        assertTrue(indexName.endsWith("."+ indexManager.parseType(tagInputBean.getLabel())));

        TagSearchChange tagSearchChange=  new TagSearchChange(indexName, tag);

        indexMappingService.ensureIndexMapping(tagSearchChange);

        tagWriter.handle(tagSearchChange);
        Thread.sleep(1000);
        // Find by code
        doQuery( indexName, tag.getLabel().toLowerCase(), tag.getCode(), 1);
        // By Name
        doQuery( indexName, tag.getLabel().toLowerCase(), tag.getName(), 1);
        // Alias
        doQuery( indexName, tag.getLabel().toLowerCase(), alias.getName(), 1);
    }
}
