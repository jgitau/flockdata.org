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

package org.flockdata.search.service;

import org.flockdata.search.model.SearchResults;
import org.springframework.integration.annotation.Gateway;

/**
 * User: Mike Holdsworth
 * Since: 13/07/13
 */
public interface EngineGateway {
    @Gateway(requestChannel = "searchReply")
    void handleSearchResult(SearchResults searchResult);

//    @Gateway(requestChannel = "validateAPI")
//    public String validateApiKey(String apiKey);

}