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

package org.flockdata.transform;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Object has support for nested properties
 *
 * Created by mike on 30/07/15.
 */
public interface UserProperties {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, Object> getProperties();

    UserProperties setProperty(String key, Object value);

    Object getProperty(String key);
}
