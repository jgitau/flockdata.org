/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.transform.xml;


import com.auditbucket.helper.FlockException;
import com.auditbucket.profile.model.Mappable;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.transform.FdReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * User: Mike Holdsworth
 * Since: 20/11/13
 */
public interface XmlMappable extends Mappable {
    @JsonIgnore
    public String getDataType();

    XmlMappable newInstance(boolean simulateOnly);

    public ContentInputBean setXMLData(XMLStreamReader xsr, FdReader FdReader) throws JAXBException, JsonProcessingException, FlockException;

    /**
     * Some XML files require skipping.
     * Use this to position the reader to the collection of elements
     *
     * @param xsr stream
     * @throws XMLStreamException
     */
    void positionReader(XMLStreamReader xsr) throws XMLStreamException;
}
