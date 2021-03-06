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

package org.flockdata.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.model.EntityTagRelationshipInput;
import org.flockdata.model.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * All data necessary to create a simple Tag. If no tagLabel is provided then the tag is
 * created under the default tagLabel of _Tag
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 1:20 PM
 */
public class TagInputBean implements org.flockdata.transform.UserProperties {

    private Map<String, Object> properties; // stored on the tag
    private Map<String, EntityTagRelationshipInput> entityTagLinks; // between entity and this tag
    private String name;
    private String code;
    private String description; // Optional description of the Label
    private boolean reverse = false;
    private Map<String, Collection<TagInputBean>> targets;
    private String label = Tag.DEFAULT_TAG;
    private boolean mustExist = false;
    private String serviceMessage; // A message to return to the caller
    private Collection<AliasInputBean> aliases;
    private String notFoundCode;
    private boolean since;
    private String keyPrefix = null;
    private boolean merge = false;


    public TagInputBean() {
    }

    /**
     * associates a tag to the Entity
     *
     * @param tagCode  Unique name for a tag (if exists will be reused)
     * @param tagLabel The "type" of tag
     */
    public TagInputBean(String tagCode, String tagLabel) {
        this(tagCode);
        setLabel(tagLabel);

    }

    /**
     * associates a tag to the entity giving it an optional tagLabel tagLabel to categorize it by
     *
     * @param tagCode  Unique name for a tag (if exists will be reused)
     * @param tagLabel optional tagLabel tagLabel to give the Tag.
     * @param etri     name of relationship to the Entity
     */
    public TagInputBean(String tagCode, String tagLabel, EntityTagRelationshipInput etri) {
        this(tagCode, tagLabel);
        addEntityTagLink(etri);


    }

    /**
     * Unique name by which this tag will be known
     * <p/>
     * You can pass this in as Name:Type and AB will additionally
     * recognize the tag as being of the supplied Type
     * <p/>
     * This tag will not be associated with an Entity (it has no tagLabel)
     * <p/>
     * Code value defaults to the tag name
     *
     * @param tagCode unique name
     */
    public TagInputBean(String tagCode) {
        this();
        if (tagCode == null)
            throw new IllegalArgumentException("The code of a tag cannot be null");
        this.code = tagCode.trim();

        //this.code = this.name;
    }

    /**
     * Tag to use with a relationship to an Entity if being tracked via an EntityInputBean
     *
     * @param tagCode          Identifier for a Tag instance
     * @param tagLabel         Classifier
     * @param relationshipName Name used to create an EntityTagRelationshipInput
     */
    public TagInputBean(String tagCode, String tagLabel, String relationshipName) {
        this (tagCode, tagLabel, new EntityTagRelationshipInput((relationshipName==null ?Tag.UNDEFINED:relationshipName)));
    }

    public void setServiceMessage(String getServiceMessage) {
        this.serviceMessage = getServiceMessage;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String setServiceMessage() {
        return serviceMessage;
    }

    public String getName() {
        return name;
    }

    public TagInputBean setName(String name) {
        this.name = name;
        return this;
    }

    /**
     *
     * @return optional description of the Tag's Label - used for describing the tag in the conceptual model
     */
    public String getDescription() {
        return description;
    }

    public TagInputBean setDescription(String description) {
        this.description = description;
        return this;
    }

    @JsonIgnore
    public Long getId() {
        return null;
    }

    public String getCode() {
        return code;
    }

    public TagInputBean setCode(String code) {
        this.code = code;
        return this;
    }

    public TagInputBean setTargets(String tagRelationship, TagInputBean tagInputBean) {
        ArrayList<TagInputBean> val = new ArrayList<>();
        val.add(tagInputBean);
        setTargets(tagRelationship, val);
        return this;

    }

    public TagInputBean setTargets(String relationshipName, Collection<TagInputBean> fromThoseTags) {
        if (targets == null)
            targets = new HashMap<>();
        Collection<TagInputBean> theseTags = targets.get(relationshipName);
        if (theseTags == null)
            targets.put(relationshipName, fromThoseTags);
        else {
            for (TagInputBean tagToAdd : fromThoseTags) {
                if (!theseTags.contains(tagToAdd))
                    theseTags.add(tagToAdd);
            }
        }
        return this;

    }

    public TagInputBean mergeTags(TagInputBean mergeFrom) {
        if (mergeFrom.hasTargets()) {
            for (String next : mergeFrom.getTargets().keySet()) {
                setTargets(next, mergeFrom.getTargets().get(next));
            }
        }
        return this;
    }

    public Map<String, Collection<TagInputBean>> getTargets() {
        return this.targets;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public TagInputBean setProperty(String key, Object value) {
        if (properties == null)
            properties = new HashMap<>();
        if (key != null && value != null)
            properties.put(key, value);
        return this;
    }

    @Override
    public Object getProperty(String key) {
        if (properties == null)
            return null;
        return properties.get(key);
    }

    public boolean isReverse() {
        return reverse;
    }

    public TagInputBean setReverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public TagInputBean addEntityTagLink(EntityTagRelationshipInput entityTagRelationshipInput) {
        if (entityTagRelationshipInput != null)
            return this.addEntityTagLink(entityTagRelationshipInput.getRelationshipName(), entityTagRelationshipInput);
        return this;
    }

    /**
     * Associates this tag with the Entity
     *
     * @param relationshipName name of the relationship to the Entity
     * @param properties       properties to store against the relationship
     */
    public TagInputBean addEntityTagLink(String relationshipName, Map<String, Object> properties) {
        return addEntityTagLink(relationshipName, new EntityTagRelationshipInput(relationshipName, properties));
    }

    /**
     * Prirmary function for carrying the relationship data describing the connection between
     * this Tag and the Entity it is being connected to
     * <p>
     * You can't have multiple relationships with the same name. Names are usually verbs
     *
     * @param relationshipName           name
     * @param entityTagRelationshipInput FD specific processing instructions and user defined properties
     * @return this
     */
    public TagInputBean addEntityTagLink(String relationshipName, EntityTagRelationshipInput entityTagRelationshipInput) {
        if (entityTagLinks == null)
            entityTagLinks = new HashMap<>();
//        if ( entityTagLinks.get(relationshipName) == null )

        this.entityTagLinks.put(relationshipName, entityTagRelationshipInput);
        return this;
    }

    /**
     * Defines the named relationship to be created between this tag and the EntityInput payload
     * it is being carried in.
     *
     * @param relationshipName simple name with no properties
     * @return this
     */
    public TagInputBean addEntityTagLink(String relationshipName) {
        if (relationshipName.equals("located"))
            setReverse(true);
        return addEntityTagLink(relationshipName, new EntityTagRelationshipInput(relationshipName));
    }

    public Map<String, EntityTagRelationshipInput> getEntityTagLinks() {
        return entityTagLinks;
    }

    public void setEntityTagLinks(Map<String, EntityTagRelationshipInput> entityLinks) {
        this.entityTagLinks = entityLinks;
    }

    @Override
    public String toString() {
        return "TagInputBean{" +
                "label='" + label + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", keyPrefix='" + keyPrefix + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagInputBean)) return false;

        TagInputBean that = (TagInputBean) o;

        if (reverse != that.reverse) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (code != null ? !code.equalsIgnoreCase(that.code) : that.code != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        return !(keyPrefix != null ? !keyPrefix.equals(that.keyPrefix) : that.keyPrefix != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (reverse ? 1 : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (keyPrefix != null ? keyPrefix.hashCode() : 0);
        return result;
    }

    public boolean isMustExist() {
        return mustExist;
    }

    public TagInputBean setMustExist(boolean mustExist) {
        this.mustExist = mustExist;
        return this;
    }

    @JsonIgnore
    public boolean isDefault() {
        return label == null || Tag.DEFAULT_TAG.equals(label);
    }

    /**
     * @return Default tag tagLabel or the name to assign
     */
    private String getLabelValue() {
        if ("".equals(label))
            return "_Tag";
        else
            return label;
    }

    public String getLabel() {
        String thisLabel = getLabelValue();
        if (thisLabel.startsWith(":"))
            thisLabel = thisLabel.substring(1, thisLabel.length());
        return thisLabel;
    }

    /**
     * Index name cannot contain spaces and will begin with a single :
     * Will add the leading : if it is missing
     */
    public TagInputBean setLabel(String label) {
        if (label == null)
            return this;

        if (!label.startsWith(":"))
            this.label = ":" + label.trim();
        else
            this.label = label.trim();

        return this;

    }

    public boolean hasAliases() {
        return (aliases != null && !aliases.isEmpty());
    }

    public Collection<AliasInputBean> getAliases() {
        return aliases;
    }

    public void setAliases(Collection<AliasInputBean> aliases) {
        this.aliases = aliases;
    }

    @JsonIgnore
    /**
     * determines if this payload has the requested relationship. Does not walk the tree!!
     */
    public boolean hasEntityRelationship(String relationship) {
        return (entityTagLinks.containsKey(relationship));
    }

    /**
     * @param mustExist don't "just create" this tag
     * @param notFound  create and link to this tag if notfound
     * @return this
     */
    public TagInputBean setMustExist(boolean mustExist, String notFound) {
        setMustExist(mustExist);
        return setNotFoundCode(notFound);
    }

    public String getNotFoundCode() {
        return notFoundCode;
    }

    public TagInputBean setNotFoundCode(String notFoundCode) {
        this.notFoundCode = notFoundCode;
        return this;
    }

    public boolean isSince() {
        return since;
    }

    public TagInputBean setSince(boolean since) {
        this.since = since;
        return this;
    }

    public void addAlias(AliasInputBean alias) {
        if (aliases == null)
            aliases = new ArrayList<>();
        aliases.add(alias);
    }

    @JsonIgnore
    // Determines if a valid NotFoundCode is set
    public boolean hasNotFoundCode() {
        return !(notFoundCode == null || notFoundCode.equals(""));
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    /**
     * Codes can have duplicate values in a Label but the key must be unique.
     * When being created, the code is used as part of the key. Setting this property
     * will prefix the key with this value.
     * <p>
     * Useful in geo type scenarios. Take Cambridge - a city in England and in New Zealand
     * <p>
     * With a tagPrefix we have uk.cambridge and nz.cambridge both with a code of Cambridge. Each
     * tag is distinct but they share human readable properties
     *
     * @param keyPrefix default is not set.
     */
    public TagInputBean setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        return this;
    }

    /**
     * Default == false
     * if true, then properties in this payload will be added to an existing tag
     *
     * @return caller wants to merge properties
     */
    public boolean isMerge() {
        return merge;
    }

    public TagInputBean setMerge(boolean merge) {
        this.merge = merge;
        return this;
    }

    public boolean contains(String code, String label, String keyPrefix) {
        return findTargetTag(code, label, keyPrefix) != null;
    }

    /**
     * determines if the targets Map contains a TagInputBean with requested properties
     * Tags are uniquely identified by either Label+Code or keyPrefix+"-"+code within a Label
     *
     * @param code      case-insensitive - mandatory
     * @param label     case-sensitive - mandatory
     * @param keyPrefix case-insensitive - optional
     * @return  TagInputBean if found
     * @since DAT-491
     */
    public TagInputBean findTargetTag(String code, String label, String keyPrefix) {
        if (!hasTargets())
            return null;

        for (String key : targets.keySet()) {
            for (TagInputBean tagInputBean : targets.get(key)) {
                if (tagInputBean.getCode().equalsIgnoreCase(code) && tagInputBean.getLabel().equals(label)) {
                    if (keyPrefix == null || keyPrefix.length() == 0)
                        return tagInputBean;// Ignoring the keyPrefix
                    if (tagInputBean.getKeyPrefix().equalsIgnoreCase(keyPrefix))
                        return tagInputBean;
                }
                TagInputBean found = tagInputBean.findTargetTag(code, label, keyPrefix);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    public boolean contains(String code, String label) {
        return contains(code, label, null);
    }

    public boolean hasTargets() {
        return this.targets != null && !targets.isEmpty();
    }

    public boolean hasTagProperties() {
        return this.properties != null && !properties.isEmpty();
    }


}
