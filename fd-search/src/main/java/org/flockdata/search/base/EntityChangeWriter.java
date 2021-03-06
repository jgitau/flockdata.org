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

package org.flockdata.search.base;

import org.flockdata.model.Entity;
import org.flockdata.search.model.EntitySearchChange;

import java.io.IOException;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 26/04/13
 * Time: 12:26 PM
 */
public interface EntityChangeWriter {

    /**
     * Rewrites an existing document
     *
     * @param searchChange values to update from
     */
    EntitySearchChange handle(EntitySearchChange searchChange) throws IOException;

    /**
     * locates a document by LogResultBean.searchKey
     *
     *
     * @param entity auditHeader
     * @return document context as bytes
     */
    Map<String, Object> findOne(Entity entity);

    void purgeCache() ;

    boolean delete(EntitySearchChange searchChange);

    /**
     * Locates a specific key monitored by the entity.
     * <p/>
     * If ID is null then the call is the same as findOne(entity)
     * where the searchKey is taken to be LogResultBean.searchKey
     *
     * @return found track change or null if none
     */
    Map<String, Object> findOne(Entity entity, String id);

    Map<String, Object> ping();

}
