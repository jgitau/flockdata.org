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

package org.flockdata.profile.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.profile.ExtractProfileDeserializer;

/**
 * Created by mike on 28/01/16.
 */
@JsonDeserialize(using = ExtractProfileDeserializer.class)
public interface ExtractProfile {

    ContentType getContentType();

    char getDelimiter();

    Boolean hasHeader();

    String getHandler();

    String getPreParseRowExp();

    String getQuoteCharacter();

    ExtractProfile setHeader(boolean header);

    ExtractProfile setContentType(ContentType contentType);

    void setPreParseRowExp(String expression);

    ExtractProfile setDelimiter(String delimiter);

    ExtractProfile setQuoteCharacter(String quoteCharacter);

    ContentModel getContentModel();

    enum ContentType {CSV, JSON, XML}
}
