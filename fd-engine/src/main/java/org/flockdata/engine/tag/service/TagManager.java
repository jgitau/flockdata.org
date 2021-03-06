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

package org.flockdata.engine.tag.service;

import org.flockdata.engine.dao.TagRepo;
import org.flockdata.helper.TagHelper;
import org.flockdata.model.Tag;
import org.flockdata.track.TagKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Put in plaec to enable caching. This could may be serve better as a Neo4j extenstion?
 * Created by mike on 24/03/16.
 */
@Repository
public class TagManager {
    private final TagRepo tagRepo;

    private final Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TagManager.class);

    @Autowired
    public TagManager(Neo4jTemplate template, TagRepo tagRepo) {
        this.template = template;
        this.tagRepo = tagRepo;
    }

    /**
     * Attempts to find tag.key by prefix.tagcode. If that doesn't exist, then it will
     * attempt to locate the alias based on tagcode
     * <p>
     * ToDo: A version to located by user defined AliasLabel
     *
     * @param tagKey   Properties that represent a unique key
     * @return resolved tag
     */
    @Cacheable(value = "tag", unless = "#result== null")
    public Tag tagByKey(TagKey tagKey) {
        if (tagKey.getPrefix() != null && tagKey.getPrefix().contains(":"))
            throw new AmqpRejectAndDontRequeueException(String.format("Unresolved indirection %s %s for %s", tagKey.getLabel(), tagKey.getCode(), tagKey.getPrefix()));
        String parsedKey = TagHelper.parseKey(tagKey.getPrefix(), tagKey.getCode());
        StopWatch watch = getWatch(tagKey.getLabel()+ " / " + parsedKey);

        Collection<Tag> tags = tagRepo.findByKey(parsedKey);

        if (tags.size() == 1) {
            Tag tag = tags.iterator().next();
            if (tag.getLabel().equals(tagKey.getLabel()) || (tagKey.getLabel().equals(Tag.DEFAULT_TAG) || tagKey.getLabel().equals("_" + Tag.DEFAULT_TAG))) {
                stopWatch(watch, tagKey.getLabel(), tagKey.getCode());
                return tag;
            }
        }

      //  logger.trace("{} Not found by key {}", multiTennantedLabel, tagKey);

        // See if the tagKey is unique for the requested label
        Tag tResult = null;
        for (Tag tag : tags) {
            if (tag.getLabel().equalsIgnoreCase(tagKey.getLabel())) {
                if (tResult == null) {
                    tResult = tag;
                } else {
                    // Deleting tags that should not exist here
                    template.delete(tag); // Concurrency issue under load ?
                }
            }
        }
        if (tResult != null) {
            stopWatch(watch, "byKey", tagKey.getLabel(), tagKey.getCode());
            return tResult;
        }

        logger.trace("Locating by alias {}, {}", tagKey.getLabel(), tagKey.getCode());

        String query;

        query = "match (:`" + tagKey.getLabel() + "Alias` {key:{tagKey}})<-[HAS_ALIAS]-(a:`" + tagKey.getLabel() + "`) return a";

        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", TagHelper.parseKey(tagKey.getCode()));
        Iterable<Map<String, Object>> result = template.query(query, params);
        Iterator<Map<String, Object>> results = result.iterator();
        Tag tagResult = null;
        while (results.hasNext()) {
            Map<String, Object> mapResult = results.next();

            if (mapResult != null && tagResult == null) {
                tagResult = getTag(mapResult);
            } else {
                Tag toDelete = getTag(mapResult);
                logger.debug("Deleting duplicate {}", toDelete);
                if (toDelete != null)
                    template.delete(toDelete);
            }

        }
        if (tagResult == null)
            logger.trace("Not found {}, {}", tagKey.getLabel(), tagKey.getCode());
        else
            stopWatch(watch, "byAlias", tagKey.getLabel(), tagKey.getCode());

        return tagResult;
    }

    StopWatch getWatch(String id) {
        StopWatch watch = null;

        if (logger.isDebugEnabled()) {
            watch = new StopWatch(id);
            watch.start(id);
        }
        return watch;
    }

    private void stopWatch(StopWatch watch, Object... args) {
        if (watch == null)
            return;

        watch.stop();
        logger.info(watch.prettyPrint());
    }

    private Tag getTag(Map<String, Object> mapResult) {
        Tag tagResult;
        Object o = null;
        if (mapResult.get("a") != null)
            o = mapResult.get("a");
        else if (mapResult.get("t") != null) { // Tag found by alias
            o = mapResult.get("t");
        }

        tagResult = (o == null ? null : template.projectTo(o, Tag.class));
        return tagResult;
    }

    @CacheEvict(value = "tag", key = "#p0")
    public Tag save(TagKey tagKey) {
        return save(tagKey.getTag());
    }

    public Tag save(Tag startTag) {
        return template.save(startTag);
    }
}
