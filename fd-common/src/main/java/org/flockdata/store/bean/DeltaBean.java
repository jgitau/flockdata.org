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

package org.flockdata.store.bean;

import org.flockdata.model.Log;
import org.flockdata.store.LogRequest;

/**
 * Created by mike on 17/02/16.
 */
public class DeltaBean {
    DeltaBean(){}

    private LogRequest logRequest;
    private Log preparedLog;

    public DeltaBean(LogRequest logRequest, Log preparedLog) {
        this();
        this.logRequest = logRequest;
        this.preparedLog = preparedLog;
    }

    public LogRequest getLogRequest() {
        return logRequest;
    }

    public Log getPreparedLog() {
        return preparedLog;
    }
}
