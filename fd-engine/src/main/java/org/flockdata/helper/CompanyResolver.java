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

package org.flockdata.helper;

import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.flockdata.model.Company;

import javax.servlet.http.HttpServletRequest;

/**
 * User: mike
 * Date: 28/08/14
 * Time: 10:27 AM
 */
public class CompanyResolver {
    public static Company resolveCompany(HttpServletRequest request) throws FlockException {
        Company company = (Company) request.getAttribute(ApiKeyInterceptor.COMPANY);
        if (company == null )
            // If you're seeing this, then check that ApiKeyInterceptor is configured to handle
            // the endpoint you are requesting
            throw new NotFoundException("Unable to identify any Company that you are authorised to work with");
        return company;
    }

    public static String resolveCallerApiKey(HttpServletRequest request)  throws FlockException {
        String apiKey= (String )  request.getAttribute(ApiKeyInterceptor.API_KEY);
        if (apiKey == null )
            throw new NotFoundException("Unable to identify the ApiKey that you are calling with");
        return apiKey;
    }
}
