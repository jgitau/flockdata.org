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

package org.flockdata.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.model.Entity;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.ContentInputBean;

import java.io.IOException;
import java.util.Map;

/**
 * User: mike
 * Date: 17/09/14
 * Time: 1:01 PM
 */
@JsonDeserialize(as=StorageBean.class)
public interface StoredContent {

    String getAttachment();

    Map<String, Object> getData() ;

    String getChecksum() throws IOException;

    /**
     *
     * @return primary key for this content
     */
    Object getId();

    ContentInputBean getContent();

    void setStore(String store);

    String getStore();

    String getType();

    Entity getEntity();
}
