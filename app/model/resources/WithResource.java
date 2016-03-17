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


package model.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.annotations.Annotation;
import model.annotations.ContextData;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Version;
import org.mongodb.morphia.utils.IndexType;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;
import elastic.ElasticUtils;

@SuppressWarnings("rawtypes")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity("RecordResource")
@Indexes({
		@Index(fields = @Field(value = "resourceType", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "collectedIn", type = IndexType.ASC), options = @IndexOptions()) })
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class WithResource<T extends DescriptiveData, U extends WithResource.WithAdmin> {

	// TODO: compound indexes for common queries on multiple fields
	@Indexes({
			@Index(fields = @Field(value = "withCreator", type = IndexType.ASC), options = @IndexOptions()),
			@Index(fields = @Field(value = "externalId", type = IndexType.ASC), options = @IndexOptions(unique = true)),
			@Index(fields = @Field(value = "access.user,access.level")) // compound/multikey
																		// index
																		// for
																		// access
	})
	public static class WithAdmin {

		// index
		@JsonSerialize(using = Serializer.WithAccessSerializer.class)
		@JsonDeserialize(using = Deserializer.WithAccessDeserializer.class)
		private WithAccess access = new WithAccess();

		/*
		 * withCreator is empty in cases of records imported from external
		 * resources. For resources uploaded by a user, it links to the userId
		 * who uploaded that resource. For collections, it links to the userId
		 * who created the collection.
		 */
		// index
		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
		private ObjectId withCreator = null;

		// uri that this resource has in the rdf repository
		private String withURI;

		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		private Date created;

		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		@Version
		private Date lastModified;

		@Embedded
		// @JsonSerialize(using = Serializer.AccessMapSerializer.class)
		// @JsonDeserialize(using = Deserializer.AccessMapDeserializer.class)
		// private final Map<ObjectId, Access> underModeration = new
		// HashMap<ObjectId, Access>();
		// recordId of last entry of provenance chain id the resource has been
		// imported from external resource
		// dbId if uploaded by user
		protected String externalId;

		public WithAccess getAccess() {
			return access;
		}

		@JsonIgnore
		public void setAccess(WithAccess access) {
			this.access = access;
		}

		public String getWithURI() {
			return withURI;
		}

		public void setWithURI(String withURI) {
			this.withURI = withURI;
		}

		public Date getCreated() {
			return created;
		}

		public void setCreated(Date created) {
			this.created = created;
			if (this.lastModified == null) {
				this.lastModified = created;
			}
		}

		public Date getLastModified() {
			return lastModified;
		}

		public void setLastModified(Date lastModified) {
			this.lastModified = lastModified;
		}

		public ObjectId getWithCreator() {
			return withCreator;
		}

		public void setWithCreator(ObjectId creatorId) {
			// OWN rights from old creator are not withdrawn (ownership is not
			// identical to creation role)
			this.withCreator = creatorId;
			if (withCreator != null)
				this.getAccess().addToAcl(creatorId, Access.OWN);
		}

		public User retrieveWithCreator() {
			ObjectId userId = getWithCreator();
			if (userId != null)
				return DB.getUserDAO().getById(userId);
			else
				return null;
		}

		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

	}

	public static class Usage {
		// in how many favorites is it
		private int likes;

		// in how many user collections is it
		private int collected;
		// how many modified versions exist
		private int annotated;

		// how often is it viewed, don't count count api calls,
		// count UI messages for viewing
		private int viewCount;

		// implementation detail, put any tag on the record twice,
		// with userID prepended and without. This will allow for people to look
		// for their own tags.
		private ArrayList<String> tags;

		public int getLikes() {
			return likes;
		}

		public void setLikes(int likes) {
			this.likes = likes;
		}

		public void incLikes() {
			this.likes++;
		}

		public void decLikes() {
			this.likes--;
		}

		public int getCollected() {
			return collected;
		}

		public void setCollected(int collected) {
			this.collected = collected;
		}

		public void incCollected() {
			this.collected++;
		}

		public void decCollected() {
			this.collected--;
		}

		public int getAnnotated() {
			return annotated;
		}

		public void setAnnotated(int annotated) {
			this.annotated = annotated;
		}

		public int getViewCount() {
			return viewCount;
		}

		public void setViewCount(int viewCount) {
			this.viewCount = viewCount;
		}

		public ArrayList<String> getTags() {
			return tags;
		}

		public void setTags(ArrayList<String> tags) {
			this.tags = tags;
		}
	}

	/**
	 * If we know about collections from our sources, the info goes here For
	 * single records, fill in the position or next in sequence, for general
	 * collection linking, omit it (i.e. if the resource is of type collection,
	 * the colletionUri refers to the external "equivalent" collection).
	 *
	 */
	public static class ExternalCollection {
		// known sources only
		private String source;
		private String collectionUri;
		private String nextInSequenceUri;
		private int position;
		private String title;
		private String description;

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getCollectionUri() {
			return collectionUri;
		}

		public void setCollectionUri(String collectionUri) {
			this.collectionUri = collectionUri;
		}

		public String getNextInSequenceUri() {
			return nextInSequenceUri;
		}

		public void setNextInSequenceUri(String nextInSequenceUri) {
			this.nextInSequenceUri = nextInSequenceUri;
		}

		public int getPosition() {
			return position;
		}

		public void setPosition(int position) {
			this.position = position;
		}

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
	}

	public static enum WithResourceType {
		WithResource, CollectionObject, RecordResource, CulturalObject, EuScreenObject, EventObject, PlaceObject, TimespanObject, ThesaurusObject, AgentObject;
	}

	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	public ObjectId getDbId() {
		return dbId;
		// TODO: fill in withURI (with postLoad?)
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	@Embedded
	protected U administrative;

	@Embedded
	@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
	private List<ObjectId> collectedIn;

	@Embedded
	private Usage usage;

	@Embedded
	// external collections to which the resource may belong to
	private List<ExternalCollection> externalCollections;

	@Embedded
	// depending on the source, we know which entry is the dataProvider and
	// which the provider
	private List<ProvenanceInfo> provenance;

	// enum of classes that are derived from DescriptiveData
	protected WithResourceType resourceType;

	// metadata
	@Embedded
	protected T descriptiveData;

	// All the available content serializations
	// all keys in here should be understood by the WITH system
	private HashMap<String, String> content;

	// all attached media Objects (their embedded part)
	@Embedded
	private List<HashMap<MediaVersion, EmbeddedMediaObject>> media;

	@Embedded
	@JsonDeserialize(using = Deserializer.ContextDataDeserializer.class)
	private List<ContextData> contextData;

	private List<Annotation> annotations;

	public WithResource() {
		this.usage = new Usage();
		this.provenance = new ArrayList<ProvenanceInfo>();
		this.collectedIn = new ArrayList<ObjectId>();
		this.contextData = new ArrayList<ContextData>();
		this.media = new ArrayList<>();
		HashMap<MediaVersion, EmbeddedMediaObject> embedded = new HashMap<MediaVersion, EmbeddedMediaObject>();
		embedded.put(MediaVersion.Thumbnail, new EmbeddedMediaObject());
		this.media.add(embedded);
	}

	public WithResource(Class<?> clazz) {
		this.usage = new Usage();
		this.provenance = new ArrayList<ProvenanceInfo>();
		this.collectedIn = new ArrayList<ObjectId>();
		this.media = new ArrayList<>();
		this.contextData = new ArrayList<ContextData>();
		HashMap<MediaVersion, EmbeddedMediaObject> embedded = new HashMap<MediaVersion, EmbeddedMediaObject>();
		embedded.put(MediaVersion.Thumbnail, new EmbeddedMediaObject());
		this.media.add(embedded);
		this.resourceType = WithResourceType.valueOf(clazz.getSimpleName());
	}

	/*
	 * Getters/Setters
	 */
	public U getAdministrative() {
		return administrative;
	}

	public void setAdministrative(U administrative) {
		this.administrative = administrative;
	}

	public List<ObjectId> getCollectedIn() {
		return collectedIn;
	}

	public void setCollectedIn(List<ObjectId> collectedIn) {
		this.collectedIn = collectedIn;
	}

	public void addPositionToCollectedIn(ObjectId colId) {
		if (collectedIn == null)
			collectedIn = new ArrayList<ObjectId>();
		collectedIn.add(colId);

	}

	public void removePositionFromCollectedIn(ObjectId colId, Integer position) {
		collectedIn.remove(new CollectionInfo(colId, position));
	}

	public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}

	public List<ExternalCollection> getExternalCollections() {
		return externalCollections;
	}

	public void setExternalCollections(
			ArrayList<ExternalCollection> externalCollections) {
		this.externalCollections = externalCollections;
	}

	public List<ProvenanceInfo> getProvenance() {
		return provenance;
	}

	public void setProvenance(List<ProvenanceInfo> provenance) {
		this.provenance = provenance;
	}

	public void addToProvenance(ProvenanceInfo provInfo) {
		this.provenance.add(provInfo);
	}

	public void addToProvenance(ProvenanceInfo provInfo, int position) {
		provenance.add(position, provInfo);
	}

	public WithResourceType getResourceType() {
		return resourceType;
	}

	public void setResourceType(WithResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public T getDescriptiveData() {
		return descriptiveData;
	}

	public void setDescriptiveData(T descriptiveData) {
		this.descriptiveData = descriptiveData;
	}

	public HashMap<String, String> getContent() {
		return content;
	}

	public void setContent(HashMap<String, String> content) {
		this.content = content;
	}

	public List<HashMap<MediaVersion, EmbeddedMediaObject>> getMedia() {
		return media;
	}

	public void setMedia(List<HashMap<MediaVersion, EmbeddedMediaObject>> media) {
		this.media = media;
	}

	public void addMediaView(MediaVersion mediaVersion,
			EmbeddedMediaObject media, int viewIndex) {
		media.setMediaVersion(mediaVersion);
		this.media.get(viewIndex).put(mediaVersion, media);
	}

	public void addMediaView(MediaVersion mediaVersion,
			EmbeddedMediaObject media) {
		HashMap<MediaVersion, EmbeddedMediaObject> e = new HashMap<>();
		e.put(mediaVersion, media);
		media.setMediaVersion(mediaVersion);
		this.media.add(e);
	}

	public void addMedia(MediaVersion mediaVersion, EmbeddedMediaObject media) {
		media.setMediaVersion(mediaVersion);
		this.media.get(0).put(mediaVersion, media);
	}

	public List<ContextData> getContextData() {
		return contextData;
	}

	public void setContextData(List<ContextData> contextData) {
		this.contextData = contextData;
	}

	public void addContextData(ContextData contextData) {
		if (this.contextData == null)
			this.contextData = new ArrayList<ContextData>();
		this.contextData.add(contextData);
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(ArrayList<Annotation> annotations) {
		this.annotations = annotations;
	}

	public User getWithCreatorInfo() {
		if (administrative.getWithCreator() != null)
			return DB.getUserDAO().getById(
					this.administrative.getWithCreator(),
					new ArrayList<String>(Arrays.asList("username")));
		else
			return null;
	}

	/* Elastic Transformations */

	/*
	 * Currently we are indexing only Resources that represent collected records
	 */
	public Map<String, Object> transformWR() {
		return ElasticUtils.basicTransformation(this);
	}
}
