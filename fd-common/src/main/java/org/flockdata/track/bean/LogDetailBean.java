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

package org.flockdata.track.bean;

import org.flockdata.model.EntityLog;
import org.flockdata.store.StoredContent;

/**
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
public class LogDetailBean {

    private LogDetailBean() {
    }

    public LogDetailBean(EntityLog log, StoredContent what) {
        this();
        this.log = log;
        this.what = what;

    }

    private EntityLog log;
    private StoredContent what;

    public EntityLog getLog() {
        return this.log;
    }

    public StoredContent getWhat() {
        return this.what;
    }

}
