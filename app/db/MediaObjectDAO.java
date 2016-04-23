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


package db;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.MediaObject;
import model.resources.RecordResource;
import model.usersAndGroups.Organization;
import model.usersAndGroups.Page;
import model.usersAndGroups.Project;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import play.Logger;
import play.Logger.ALogger;

public class MediaObjectDAO {
	public static final ALogger log = Logger.of(MediaObjectDAO.class);

	/**
	 * Converts GridFSDBFile to MediaObject. We use this method for convertion
	 * because we cannot cast directly to Media. We also cannot add Media as a
	 * subclass to GridFSDBFile because otherwise we will have to "_id" fields
	 * and the Morphia document mapper will fail.
	 *
	 * @param gridfsDbFile
	 * @return
	 */
	private MediaObject gridFsDbFileToMediaObj(GridFSDBFile gridfsDbFile) {

		if (gridfsDbFile == null)
			return null;

		// some things are not quite right, so we repair those

		try {
			MediaObject media = DB.getMorphia().getMapper().fromDBObject(
					MediaObject.class, gridfsDbFile, new DefaultEntityCache());
			media.setMediaBytes(
					IOUtils.toByteArray(gridfsDbFile.getInputStream()));
			return media;
		} catch (IOException e) {
			log.error(
					"Error transforming media file's InputStream to raw bytes",
					e);
		} catch (Exception e) {
			log.error("Error setting properties to Media object", e);
		}
		return null;
	}

	/**
	 * Retrieve a MediaObject from GridFS using the dbId
	 * 
	 * @param dbId
	 * @return
	 */
	public MediaObject findById(ObjectId dbId) {
		GridFSDBFile media = null;
		try {
			media = DB.getGridFs().find(dbId);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + dbId);
		}

		if (media == null)
			log.debug("Cannot find Media document with ID: " + dbId);
		else
			log.debug("Succesfully found Media GridFS document");

		return gridFsDbFileToMediaObj(media);
	}

	/**
	 * Delete a MediaObject file from GridFS using it's dbId
	 * 
	 * @param dbId
	 */
	public void deleteById(ObjectId dbId) {
		try {
			DB.getGridFs().remove(dbId);
		} catch (Exception e) {
			log.error("Cannot delete Media document from GridFS", e);
			throw e;
		}
	}

	/**
	 * Stores the MediaObject to GridFS filesystem. This method internally
	 * converts the MediaObject POJO to a GridFSFile and then stores it to
	 * GridFS.
	 * 
	 * @param media
	 * @throws Exception
	 */
	public void makePermanent(MediaObject media) throws Exception {
		GridFSFile mediaGridFsFile;

		try {

			if (media.getDbId() != null) {
				mediaGridFsFile = DB.getGridFs().find(media.getDbId());
			} else {
				if (media.getMediaBytes() == null) {
					String tmp = new String(); // an empty string

					mediaGridFsFile = DB.getGridFs().createFile(
							new ByteArrayInputStream(tmp.getBytes()));
				} else
					mediaGridFsFile = DB.getGridFs()
							.createFile(media.getMediaBytes());
			}
			DBObject mediaDbObj = DB.getMorphia().getMapper().toDBObject(media);
			// remove stuff we don't want on the media object
			mediaDbObj.removeField("className");
			mediaDbObj.removeField("data");
			mediaDbObj.removeField("_id");

			for (String k : mediaDbObj.keySet()) {
				mediaGridFsFile.put(k, mediaDbObj.get(k));
			}
			mediaGridFsFile.save();
			media.setDbId((ObjectId) mediaGridFsFile.getId());

			// save the file
		} catch (Exception e) {
			log.error("Cannot save Media document to GridFS", e);
			throw e;
		}
	}

	/**
	 * Deletes a MediaObject from GridFS.
	 * 
	 * @param media
	 */
	public void makeTransient(MediaObject media) {
		try {
			DB.getGridFs().remove(media.getDbId());
		} catch (Exception e) {
			log.error("Cannot delete Media document from GridFS", e);
			throw e;
		}
	}

	/**
	 * Retrieve a MediaObject from GridFS according to it's external url.
	 * According to the value specified, we return either the thumbnail or the
	 * original media.
	 * 
	 * @param externalUrl
	 * @param thumbnail
	 * @return
	 */
	public MediaObject getByThumbnailUrl(String withThumbUrl) {
		GridFSDBFile media = null;
		try {
			// here I have to ask where we will use 'url' or 'withUrl' or both
			BasicDBObject query = new BasicDBObject("withThumbnailUrl",
					withThumbUrl);
			media = DB.getGridFs().findOne(query);
			if (media.containsField("mediaBytes"))
				media.removeField("mediaBytes");
			return gridFsDbFileToMediaObj(media);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + withThumbUrl);
			return null;
		}
	}

	/**
	 * Retrieve a MediaObject from GridFS according to it's external url.
	 * According to the value specified, we return either the thumbnail or the
	 * original media.
	 * 
	 * @param externalUrl
	 * @param thumbnail
	 * @return
	 */
	public MediaObject getByUrl(String withUrl) {
		GridFSDBFile media = null;
		try {
			// here I have to ask where we will use 'url' or 'withUrl' or both
			BasicDBObject query = new BasicDBObject("url", withUrl);
			media = DB.getGridFs().findOne(query);
			if (media.containsField("thumbnailBytes"))
				media.removeField("thumbnailBytes");
			return gridFsDbFileToMediaObj(media);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + withUrl);
			return null;
		}
	}

	public MediaObject getByUrlAndVersion(String url, MediaVersion version) {
		GridFSDBFile media = null;
		try {
			// here I have to ask where we will use 'url' or 'withUrl' or both
			BasicDBObject query = new BasicDBObject("url", url);
			query.append("mediaVersion", version.toString());
			media = DB.getGridFs().findOne(query);
			return gridFsDbFileToMediaObj(media);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + url);
			return null;
		}
	}

	/**
	 * Count all documents that match the query specified
	 * 
	 * @param query
	 * @return
	 */
	public int countAll(BasicDBObject query) {
		return DB.getGridFs().find(query).size();
	}

	/**
	 * Delete all MediaObject entities from GridFs
	 */
	public void deleteCached() {
		try {
			BasicDBObject query = new BasicDBObject();
			query.containsField("url");
			DB.getGridFs().remove(query);
		} catch (Exception e) {
			log.error("Cannot delete files from GridFS", e);
			throw e;
		}
	}

	/*
	 * Delete all media objects which are neither avatar, not cover, not in
	 * embedded media objects in records. All these records are referenced
	 * through their urls
	 */
	public void deleteOrphanMediaObjects() {
		// Find all media urls of medias that are needed: user and group
		// avatars, cover images for organizations/projects and images of
		// stored records at the database
		Set<String> urls = new HashSet<String>();
		findUrlsFromAvatars(urls);
		findUrlsFromCovers(urls);
		findUrlsFromRecords(urls);
		// A query at the database for deleting media objects whose url is not
		// in this list ($nin) would be very convenient. Unfortunately, this
		// list is expected to be very big (hundreds of thousand urls) which
		// makes the query impossible to send.
		DBCursor mediaList = DB.getGridFs().getFileList(new BasicDBObject(),
				new BasicDBObject("_id", 1));
		int mediaCount = mediaList.size();
		int i = 1;
		for (DBObject media : mediaList) {
			System.out
					.println("Check media " + i + " of "
							+ mediaCount + " - " + new DecimalFormat("##.##")
									.format((float) 100 * i / mediaCount)
							+ "%");
			existsReferenceToMediaUrl(media.get("url").toString());
		}
	}

	private void findUrlsFromAvatars(Set<String> urls) {
		Iterator<User> userIterator = DB.getUserDAO().createQuery().iterator();
		while (userIterator.hasNext()) {
			User user = userIterator.next();
			if (user.getAvatar() != null && !user.getAvatar().isEmpty())
				urls.addAll(user.getAvatar().values());
		}
		Iterator<UserGroup> groupIterator = DB.getUserGroupDAO().createQuery()
				.iterator();
		while (groupIterator.hasNext()) {
			UserGroup group = groupIterator.next();
			if (group.getAvatar() != null && !group.getAvatar().isEmpty())
				urls.addAll(group.getAvatar().values());
		}
	}

	private void findUrlsFromCovers(Set<String> urls) {
		Iterator<UserGroup> groupIterator = DB.getUserGroupDAO().createQuery()
				.iterator();
		while (groupIterator.hasNext()) {
			UserGroup group = groupIterator.next();
			Page page = null;
			if (group instanceof Organization)
				page = ((Organization) group).getPage();
			if (group instanceof Project)
				page = ((Project) group).getPage();
			if (page == null || page.getCover() == null
					|| page.getCover().isEmpty())
				continue;
			urls.addAll(page.getCover().values());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void findUrlsFromRecords(Set<String> urls) {
		Iterator<RecordResource> recordIterator = DB.getRecordResourceDAO()
				.createQuery().iterator();
		while (recordIterator.hasNext()) {
			RecordResource record = recordIterator.next();
			List<HashMap<MediaVersion, EmbeddedMediaObject>> mediaList = record
					.getMedia();
			for (HashMap<MediaVersion, EmbeddedMediaObject> media : mediaList) {
				Collection<EmbeddedMediaObject> mediaObjects = media.values();
				for (EmbeddedMediaObject mediaObject : mediaObjects) {
					urls.add(mediaObject.getUrl());
				}
			}
		}
	}

	private boolean existsReferenceToMediaUrl(String url) {
		if (url == null)
			return false;
		return false;
	}
}
