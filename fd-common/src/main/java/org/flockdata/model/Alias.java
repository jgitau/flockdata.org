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

package org.flockdata.model;

import org.flockdata.registration.AliasInputBean;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Labels;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.ArrayList;

/**
 * Created by mike on 1/04/15.
 */
@NodeEntity // Only in place to support projection
@TypeAlias("Alias")
public class Alias {

    @GraphId
    Long id;

    String description;
    String name;
    String key;

    @Labels
    private ArrayList<String> labels = new ArrayList<>();

    @RelatedTo(elementClass = Tag.class, type = "HAS_ALIAS", direction = Direction.INCOMING)
    //    @Relationship(type = "HAS_ALIAS", direction = Relationship.INCOMING)
    private Tag tag ;

    Alias(){
        // ToDo: Remove with SDN4
    }

    public Alias(String theLabel, AliasInputBean aliasInput, String key, Tag tag) {
        this();
        // ToDo: This should be provided by the caller
        labels.add(theLabel+"Alias");
        labels.add("Alias");
        labels.add("_Alias");
        this.key = key;
        this.name = aliasInput.getCode();
        this.description = aliasInput.getDescription();
        this.tag = tag;
    }

    public String getLabel() {
        for (String label : labels) {
            if (! (label.equals("Alias") || label.equals("_Alias")))
                return label;
        }
        return null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tag getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return "AliasNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public String getKey() {
        return key;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }
}
