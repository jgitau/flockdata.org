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

package org.flockdata.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.math.NumberUtils;
import org.flockdata.helper.TagHelper;
import org.flockdata.model.EntityTag;
import org.flockdata.model.SubTag;
import org.flockdata.model.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.flockdata.helper.TagHelper.isSystemKey;

/**
 * Creates a representation of a tag, plug it's geo content, suitable for representation
 * in fd-search
 * <p>
 * Created by mike on 7/02/15.
 */
public class SearchTag {
    String code;
    String name;
    Map<String, Object> properties;
    Map<String, Object> rlx;
    Map<String, Object> geo = null;
    Map<String, String> points = new HashMap<>();

    //@JsonDeserialize(using = SearchSubTagsDeserializer.class)
    Map<String, Collection<SearchTag>> parent = new HashMap<>();

    String geoDesc;

    SearchTag() {
    }

    public SearchTag(EntityTag entityTag) {
        this();
        this.code = entityTag.getTag().getCode();
        this.name = entityTag.getTag().getName();

        if (this.name != null && this.name.equalsIgnoreCase(code))
            this.name = null; // Prefer code over name if they are the same

        // DAT-446 - ignore the code if it it is numeric, short and we have a textual name
        if (NumberUtils.isNumber(this.code) && this.code.length() < 3 && this.name != null)
            this.code = null;

        for (String key : entityTag.getTag().getProperties().keySet()) {
            if (!TagHelper.isSystemKey(key)) {
                if ( properties == null )
                    properties= new HashMap<>();
                this.properties.put(key, entityTag.getTag().getProperty(key));
            }
        }
        handleSubTags(entityTag);


        if (entityTag.getGeoData() != null) {
            if (geo == null)
                geo = new HashMap<>();
            for (String s : entityTag.getGeoData().getGeoBeans().keySet()) {
                Object geoCode = entityTag.getGeoData().getGeoBeans().get(s).getCode();
                if (geoCode != null)
                    geo.put(s + "Code", geoCode);
                if (entityTag.getGeoData().getGeoBeans().get(s).getName() != null)
                    geo.put(s + "Name", entityTag.getGeoData().getGeoBeans().get(s).getName());
                if (entityTag.getGeoData().getPoints() != null) {
                    geo.put("points", entityTag.getGeoData().getPoints());
                }
                this.geoDesc = entityTag.getGeoData().getDescription();
            }

            //this.geoDesc =entityTag.getGeoData().getDescription();
        }
        if (entityTag.getProperties() != null && !entityTag.getProperties().isEmpty()) {
            this.rlx = new HashMap<>();
            // Know one will want to see these column values. Applicable for a graph viz.
            entityTag.getProperties().keySet().stream().filter
                    (key -> !isSystemKey(key)).
                    forEach(key -> {
                        rlx.put(key, entityTag.getProperties().get(key));
                    });
        }

    }

    private void handleSubTags(EntityTag entityTag) {
        for (String key : entityTag.getTag().getSubTags().keySet()) {
            //if ( !TagHelper.isSystemKey(key))
            Collection<Tag> subTags = entityTag.getTag().getSubTags(key);
            for (Tag subTag : subTags) {
                SearchTag searchSubTag = new SearchTag(new SubTag(subTag, subTag.getLabel()));
                Collection<SearchTag>searchTags = this.parent.get(key);
                if (searchTags == null ) {
                    searchTags = new ArrayList<>();
                    this.parent.put(key, searchTags);
                }
                searchTags.add(searchSubTag);
            }

        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCode() {
        return code;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getName() {
        return name;
    }

    /**
     * @return Tags user defined properties
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Object> getRlx() {
        return rlx;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getGeo() {
        return geo;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "SearchTag{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getGeoDesc() {
        return geoDesc;
    }

    @JsonIgnore
    public boolean hasSingleProperty() {
        return ((properties == null || properties.isEmpty()) && (rlx == null || rlx.isEmpty()) && name == null);

    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Collection<SearchTag>> getParent() {
        return parent;
    }


}
