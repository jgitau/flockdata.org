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

import org.flockdata.engine.FdServerWriter;
import org.flockdata.engine.repo.neo4j.dao.ProfileDao;
import org.flockdata.engine.repo.neo4j.model.ProfileNode;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.profile.service.ImportProfileService;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.model.DocumentType;
import org.flockdata.track.service.SchemaService;
import org.flockdata.transform.FileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:43 PM
 */
@Service
public class ProfileServiceNeo4j implements ImportProfileService {

    @Autowired
    ProfileDao profileDao;

    @Autowired
    FortressService fortressService;

    @Autowired
    SchemaService schemaService;

    @Autowired
    FdServerWriter fdServerWriter;

    static final ObjectMapper objectMapper = FlockDataJsonFactory.getObjectMapper();

    public ProfileConfiguration get(Fortress fortress, DocumentType documentType) throws FlockException {
        ProfileNode profile = profileDao.find(fortress, documentType);

        if (profile == null)
            throw new NotFoundException(String.format("Unable to locate and import profile for [%s], [%s]", fortress.getCode(), documentType.getCode()));
        //return profile;
        String json = profile.getContent();
        try {
            ImportProfile iProfile=  objectMapper.readValue(json, ImportProfile.class);
            iProfile.setFortressName(fortress.getName());
            iProfile.setDocumentName(documentType.getName());
            return iProfile;
        } catch (IOException e) {
            throw new FlockException(String.format("Unable to obtain content from ImportProfile {%d}", profile.getId()), e);
        }
    }

    public void save(Fortress fortress, DocumentType documentType, ProfileConfiguration profileConfig) throws FlockException {
        //objectMapper.
        ProfileNode profile = profileDao.find(fortress, documentType);
        if (profile == null) {
            profile = new ProfileNode(fortress, documentType);
        }
        try {
            profile.setContent(objectMapper.writeValueAsString(profileConfig));

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new FlockException("Json error", e);
        }

        // ToDo: Track the change against a FlockData system account
        profileDao.save(profile);
    }

    @Override
    public void save(Company company, String fortressCode, String documentCode, ImportProfile profile) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        DocumentType documentType = schemaService.resolveByDocCode(fortress, documentCode, false);
        if (documentType == null )
            throw new NotFoundException("Unable to resolve document type ");
        save(fortress, documentType, profile);
    }

    /**
     * Does not validate the arguments.
     */
    @Override
    @Async
    public void processAsync(Company company, String fortressCode, String documentCode, String file) throws ClassNotFoundException, FlockException, InstantiationException, IOException, IllegalAccessException {
        process(company, fortressCode, documentCode, file, true);
    }

    @Override
    public void process(Company company, String fortressCode, String documentCode, String file, boolean async) throws FlockException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        DocumentType documentType = schemaService.resolveByDocCode(fortress, documentCode, false);
        if (documentType == null )
            throw new NotFoundException("Unable to resolve document type ");
        process(company, fortress, documentType, file, async);
    }

    public void process(Company company, Fortress fortress, DocumentType documentType, String file, Boolean async) throws FlockException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
        ProfileConfiguration profile = get(fortress, documentType);
        profile.setFortressName(fortress.getName());
        profile.setDocumentName(documentType.getName());
        FileProcessor fileProcessor = new FileProcessor(fdServerWriter);
        FileProcessor.validateArgs(file);
        fileProcessor.processFile(profile, file, 0, fdServerWriter, company, async);
    }

    @Override
    public void validateArguments(Company company, String fortressCode, String documentCode, String fileName) throws NotFoundException, IOException {
        if ( !FileProcessor.validateArgs(fileName)) {
            throw new NotFoundException("Unable to process filename "+ fileName);
        }
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if ( fortress == null )
            throw new NotFoundException("Unable to locate the fortress " + fortressCode);
        DocumentType documentType = schemaService.resolveByDocCode(fortress, documentCode, false);
        if (documentType == null )
            throw new NotFoundException("Unable to resolve document type " + documentCode);


    }

    @Override
    public ProfileConfiguration get(Company company, String fortressCode, String documentCode) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if ( fortress == null )
            throw new NotFoundException("Unable to locate the fortress " + fortressCode);
        DocumentType documentType = schemaService.resolveByDocCode(fortress, documentCode, false);
        if (documentType == null )
            throw new NotFoundException("Unable to resolve document type " + documentCode);

        return get(fortress, documentType);
    }


}