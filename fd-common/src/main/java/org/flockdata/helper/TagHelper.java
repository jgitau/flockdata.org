/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.helper;

import org.flockdata.model.Tag;

/**
 * Created by mike on 20/06/15.
 */
public class TagHelper {

    public static String suffixLabel(String label, String tagSuffix) {
        if (label.startsWith(":"))
            label = label.substring(1);

        if ("".equals(tagSuffix))
            return label;
        return label + tagSuffix;
    }

    public static String parseKey(String key) {
        return key.toLowerCase();
    }

    public static boolean isDefault(String name) {
        return name == null || Tag.DEFAULT_TAG.equals(name) ||Tag.DEFAULT.equals(name);
    }


}
