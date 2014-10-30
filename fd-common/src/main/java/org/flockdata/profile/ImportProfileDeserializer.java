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

package org.flockdata.profile;

import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.transform.ColumnDefinition;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 8:45 AM
 */
public class ImportProfileDeserializer extends JsonDeserializer<ImportProfile> {
    @Override
    public ImportProfile deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ImportProfile importProfile = new ImportProfile();
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode column = node.get("documentName");
        if ( column == null )
            column = node.get("documentType");
        if (column != null)
            importProfile.setDocumentName(column.asText());

        column = node.get("clazz");
        if (column != null && !column.isNull())
            importProfile.setClazz(column.asText());

        column = node.get("fortressName");
        if (column != null&& !column.isNull())
            importProfile.setFortressName(column.asText());

        column = node.get("tagOrEntity");
        if ( column == null  )
            column = node.get("tagOrTrack");

        if (column != null)
            importProfile.setTagOrEntity(column.asText());

        column = node.get("fortressUser");
        if (column != null&& !column.isNull())
            importProfile.setFortressUser(column.asText());

        column = node.get("staticDataClazz");
        if (column != null&& !column.isNull())
            importProfile.setStaticDataClazz(column.asText());


        column = node.get("entityOnly");
        if ( column != null&& !column.isNull() )
            column = node.get("metaOnly"); // legacy value
        if (column != null&& !column.isNull())
            importProfile.setEntityOnly(Boolean.parseBoolean(column.asText()));

        column = node.get("header");
        if (column != null)
            importProfile.setHeader(Boolean.parseBoolean(column.asText()));

        column = node.get("delimiter");
        if (column != null&& !column.isNull())
            importProfile.setDelimiter(column.toString().charAt(1));

        column = node.get("entityKey");
        if ( column == null )
            column = node.get("metaHeader");// legacy value

        if ( column !=null && !column.isNull() )
            importProfile.setEntityKey(column.asText());

        column = node.get("event");
        if ( column != null && !column.isNull() )
            importProfile.setEvent(column.asText());


        if ( column!=null) {
            importProfile.setEntityKey(column.asText());
        }


        column = node.get("contentType");
        if (column != null) {
            switch (column.textValue().toLowerCase()) {
                case "csv":
                    importProfile.setContentType(ProfileConfiguration.ContentType.CSV);
                    break;
                case "xml":
                    importProfile.setContentType(ProfileConfiguration.ContentType.XML);
                    break;
                case "json":
                    importProfile.setContentType(ProfileConfiguration.ContentType.JSON);
                    break;
            }
        }
        column = node.get("content");
        if ( column !=null ){
            ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();
            Iterator<Map.Entry<String,JsonNode>> columns = column.fields();
            Map<String,ColumnDefinition>content = new HashMap<>();
            while (columns.hasNext()) {
                Map.Entry<String, JsonNode> next = columns.next();
                String colName = next.getKey();
                ColumnDefinition columnDefinition = mapper.readValue(next.getValue().toString(), ColumnDefinition.class);
                content.put(colName, columnDefinition);
            }
            importProfile.setContent(content);
        }
        return importProfile;  //To change body of implemented methods use File | Settings | File Templates.
    }
    public static ImportProfile getImportParams(String profile) throws IOException {
        ImportProfile importProfile;
        ObjectMapper om = FlockDataJsonFactory.getObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            importProfile = om.readValue(fileIO, ImportProfile.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                importProfile = om.readValue(stream, ImportProfile.class);
            } else
                // Defaults??
                importProfile = new ImportProfile();
        }
        //importParams.setRestClient(restClient);
        return importProfile;
    }

}