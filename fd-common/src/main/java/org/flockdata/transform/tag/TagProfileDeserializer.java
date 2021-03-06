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

package org.flockdata.transform.tag;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import org.flockdata.helper.FdJsonObjectMapper;

import java.io.IOException;
import java.util.ArrayList;

/**
 * User: mike
 * Date: 27/05/14
 * Time: 4:25 PM
 */
public class TagProfileDeserializer extends JsonDeserializer<ArrayList<TagProfile>> {

    private static final ObjectMapper objectMapper = new ObjectMapper( new FdJsonObjectMapper())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(JsonParser.Feature.ALLOW_COMMENTS);

    @Override
    public ArrayList<TagProfile> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ArrayList<TagProfile> values = new ArrayList<>();
        JsonNode node = jp.getCodec().readTree(jp);
        for (JsonNode jsonNode : node) {
            values.add(objectMapper.readValue(jsonNode.toString(), TagProfile.class));

        }
        return values;
    }
}
