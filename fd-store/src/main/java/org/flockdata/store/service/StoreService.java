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

package org.flockdata.store.service;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 12:07 PM
 */
public interface StoreService {
    String ping(Store store);

    StoredContent doRead(Store store, String index, String type, String id);

    //StoredContent doRead(LogRequest logRequest);

    void delete(Entity entity, Log change);

    void doWrite( StorageBean kvBean) throws FlockException;


}
