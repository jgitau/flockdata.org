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

package org.flockdata.search.endpoint;

import org.flockdata.helper.FlockException;
import org.flockdata.search.model.ContentStructure;
import org.flockdata.search.model.QueryParams;
import org.flockdata.search.service.ContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Access to content structures stored on ElasticSearch
 *
 * Created by mike on 31/08/16.
 */
@RequestMapping("${org.fd.search.system.api:api}/v1/content")
@RestController
public class ContentEP {

    private ContentService contentService;

    @Autowired
    private void setContentService(ContentService contentService){
        this.contentService = contentService;
    }

    @RequestMapping(value = "/{company}/{fortress}/{type}", produces = "application/json",
            method = RequestMethod.GET)
    public ContentStructure simpleQuery(@PathVariable("company") String company, @PathVariable("type") String type, @PathVariable("fortress") String fortress) throws FlockException {
        QueryParams queryParams = new QueryParams()
                .setCompany(company.toLowerCase())
                .setFortress(fortress.toLowerCase())
                .setTypes(type.toLowerCase());
        return contentService.getStructure(queryParams);
    }

    @RequestMapping(value = "/", consumes = "application/json", produces = "application/json",
            method = RequestMethod.POST)
    public ContentStructure simpleQuery(@RequestBody QueryParams queryParams) throws FlockException {
        return contentService.getStructure(queryParams);
    }

}
