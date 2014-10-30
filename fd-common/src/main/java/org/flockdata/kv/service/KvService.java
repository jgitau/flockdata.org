/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.kv.service;

import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityContent;
import org.flockdata.track.bean.DeltaBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.Log;

import java.io.IOException;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 12:07 PM
 */
public interface KvService {
    String ping();

    void purge(String indexName);

    void doKvWrite(TrackResultBean resultBean) throws IOException;

    Log prepareLog(Log log, ContentInputBean content) throws IOException;

    EntityContent getContent(Entity entity, Log log);

    void delete(Entity entity, Log change);

    boolean isSame(Entity entity, Log compareFrom, Log compareTo);

    boolean sameJson(EntityContent compareFrom, EntityContent compareWith);

    DeltaBean getDelta(Entity entity, Log from, Log to);

    public enum KV_STORE {REDIS, RIAK}
}