/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.tag.dao;

import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.TagPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;

/**
 * Wrap calls to Neo4j server extension. Figuring this out....
 *
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:33 PM
 */
@Repository
public class TagDaoNeo4j {

    @Autowired
    TagWrangler tagWrangler;

    public Collection<TagResultBean> save(TagPayload payload) {
        return tagWrangler.save(payload);
    }

    public void createAlias(String suffix, Tag tag, String label, AliasInputBean aliasInput) {
        tagWrangler.createAlias(suffix, tag, label, aliasInput);
    }

    public Collection<Tag> findDirectedTags(String tagSuffix, Tag startTag, Company company) {
        return tagWrangler.findDirectedTags(tagSuffix, startTag, company);
    }

    public Collection<Tag> findTags(String label) {
        return tagWrangler.findTags(label);
    }


    public Collection<AliasInputBean> findTagAliases(Tag sourceTag) throws NotFoundException {

        return tagWrangler.findTagAliases(sourceTag);
    }

    public Map<String, Collection<TagResultBean>> findAllTags(Tag sourceTag, String relationship, String targetLabel) {
        return tagWrangler.findAllTags(sourceTag, relationship, targetLabel);

    }

    public Tag findTagNode(String suffix, String label, String tagCode, boolean inflate) {
        return tagWrangler.findTagNode(suffix, label, tagCode, inflate);
    }
}
