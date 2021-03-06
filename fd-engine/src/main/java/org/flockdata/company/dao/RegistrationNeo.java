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

package org.flockdata.company.dao;

import org.flockdata.integration.KeyGenService;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.dao.RegistrationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:40 PM
 */
@Repository
public class RegistrationNeo implements RegistrationDao {
    private final SystemUserRepository suRepo;

    private final KeyGenService keyGenService;

    @Autowired
    public RegistrationNeo(SystemUserRepository suRepo, KeyGenService keyGenService) {
        this.suRepo = suRepo;
        this.keyGenService = keyGenService;
    }

    public SystemUser save(SystemUser systemUser) {
        return suRepo.save(systemUser);
    }

    @Cacheable(value = "sysUserApiKey", unless = "#result==null")
    public SystemUser findByApiKey(String apiKey) {
        if (apiKey == null)
            return null;
        return suRepo.findBySchemaPropertyValue("apiKey", apiKey);
    }

    @Override
    public SystemUser save(RegistrationBean regBean) {
        SystemUser su = new SystemUser(regBean);
        su.setApiKey(keyGenService.getUniqueKey());
        su = save(su);
        return su;


    }

    public SystemUser findSysUserByName(String name) {
        return suRepo.getSystemUser(name);
    }


}
