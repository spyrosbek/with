/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import model.User.Access;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.AccessManager;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;

@Entity
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(value=JsonInclude.Include.NON_NULL)
public class Collection {
	private static final int EMBEDDED_CAP = 20;


	@Id
	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	@NotNull
	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	private ObjectId ownerId;

	@NotNull
	@NotBlank
	//@Indexed(name="indexed_title", unique=true, dropDups=true)
	private String title;
	private String description;

	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	private ObjectId thumbnail;

	private int itemCount;
	private boolean isPublic;
	private Date created;
	private Date lastModified;
	private String category;

	// fixed-size list of entries
	// those will be as well in the CollectionEntry table
	@Embedded
	private List<CollectionRecord> firstEntries = new ArrayList<CollectionRecord>();

	private final Map<ObjectId, Access> rights = new HashMap<ObjectId, Access>();

	public ObjectId getDbId() {
		return this.dbId;
	}

	@JsonProperty
	public void setDbId(ObjectId id) {
		this.dbId = id;
	}


	/**
	 * Get the embeddable Metadata part
	 * @return
	 */
	public CollectionMetadata collectMetadata() {
		CollectionMetadata cm = new CollectionMetadata();
		cm.setCollectionId(this.dbId);
		cm.setDescription(description);
		cm.setThumbnail(thumbnail);
		cm.setTitle(title);

		return cm;
	}

	// Getter setters
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public boolean isPublic() {
		return isPublic;
	}
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public User retrieveOwner() {
		return	DB.getUserDAO().getById(this.ownerId, null);
	}

	public ObjectId getOwnerId() {
		return this.ownerId;
	}

	@JsonProperty
	public void setOwnerId(ObjectId ownerId) {
		if( ownerId.equals(this.ownerId) ) {
			this.ownerId = ownerId;
			Map<ObjectId, Access> ownerRights = new HashMap<ObjectId, Access>();
			ownerRights.put(this.ownerId, Access.OWN);
			AccessManager.addRight(rights, ownerRights);

			//create a new collection metadata for owner
			User owner = DB.getUserDAO().get(ownerId);
			owner.getCollectionMetadata().add(collectMetadata());
			//save the new owner
			DB.getUserDAO().makePermanent(owner);
		}
	}

	public void setOwnerId(User owner) {
		//set owner to collection
		if( !owner.getDbId().equals(this.ownerId) ) {
			this.ownerId = owner.getDbId();
			Map<ObjectId, Access> ownerRights = new HashMap<ObjectId, Access>();
			ownerRights.put(this.ownerId, Access.OWN);
			AccessManager.addRight(rights, ownerRights);

			//create a new collection metadata for owner
			owner.getCollectionMetadata().add(collectMetadata());

			//save the new owner
			DB.getUserDAO().makePermanent(owner);
		}
	}

	public List<CollectionRecord> getFirstEntries() {
		return firstEntries;
	}

	public void setFirstEntries(List<CollectionRecord> firstEntries) {
		this.firstEntries = firstEntries;
	}

	public String getThumbnailUrl() {

		if(firstEntries.size() > 0)
			return 	firstEntries.get(0).getThumbnailUrl();
		return null;

	}

	public Media retrieveThumbnail() {
		Media media =
				DB.getMediaDAO().findById(this.thumbnail);
		return media;
	}

	public ObjectId getThumbnail() {
		return this.thumbnail;
	}

	//@JsonProperty
	@JsonIgnore
	public void setThumbnail(ObjectId thumbId) {
		this.thumbnail = thumbId;
	}

	@JsonIgnore
	public void setThumbnail(Media thumbnail) {
		this.thumbnail = thumbnail.getDbId();
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public void itemCountIncr() {
		this.itemCount++;
	}

	public void itemCountDiscr() {
		this.itemCount--;
	}

	public Map<ObjectId, Access> getRights() {
		return rights;
	}

}
