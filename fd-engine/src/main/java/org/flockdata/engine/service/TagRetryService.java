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

package org.flockdata.engine.service;

import org.flockdata.engine.repo.neo4j.dao.TagDaoNeo4j;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User: mike
 * Date: 26/09/14
 * Time: 6:43 PM
 */

@EnableRetry
@Service
@Transactional
public class TagRetryService {

    @Autowired
    private TagDaoNeo4j tagDao;

    @Retryable(include = Exception.class, maxAttempts = 12, backoff = @Backoff(delay = 50, maxDelay = 400))
    public void track(Company company, List<TagInputBean> tagInputBeans) {
        tagDao.save(company, tagInputBeans, true);


    }

}
