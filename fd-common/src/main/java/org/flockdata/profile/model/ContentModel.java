/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.profile.model;

import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.transform.ColumnDefinition;

import java.util.Map;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:51 PM
 */
public interface ContentModel extends ImportFile {

    DocumentTypeInputBean getDocumentType();

    String getName();

    void setContent(Map<String, ColumnDefinition> columns);

    Map<String, ColumnDefinition> getContent();

    FortressInputBean getFortress() ;

    String getFortressUser();

    boolean isEntityOnly();

    boolean isArchiveTags();

    String getEvent();

    ColumnDefinition getColumnDef(String column);

    void setFortressName(String fortressName);

    ContentModel setFortress(FortressInputBean fortress);

    void setDocumentName(String name);

    String getSegmentExpression();

    ContentModel setDocumentType(DocumentTypeInputBean documentType);

    enum DataType {ENTITY, TAG}

    void setTagOrEntity(DataType dataType);
}