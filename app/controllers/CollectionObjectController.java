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


package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.parameterTypes.MyPlayList;
import controllers.parameterTypes.StringTuple;
import controllers.thesaurus.CollectionIndexController;
import db.DB;
import model.annotations.ContextData;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.CollectionObject;
import model.resources.CollectionObject.CollectionAdmin;
import model.resources.CollectionObject.CollectionAdmin.CollectionType;
import model.resources.RecordResource;
import model.resources.WithResource;
import model.resources.WithResource.WithResourceType;
import model.usersAndGroups.Organization;
import model.usersAndGroups.Page;
import model.usersAndGroups.Project;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import model.usersAndGroups.UserOrGroup;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.F;
import play.libs.F.Function0;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import sources.EuropeanaCollectionSpaceSource;
import sources.EuropeanaSpaceSource;
import sources.core.CommonQuery;
import sources.core.SourceResponse;
import sources.core.Utils;
import utils.AccessManager;
import utils.AccessManager.Action;
import utils.Locks;
import utils.Tuple;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

/**
 * @author mariaral
 *
 */
public class CollectionObjectController extends WithResourceController {

	public static final ALogger log = Logger
			.of(CollectionObjectController.class);

	
	public static Promise<Result> importSearch(){
		JsonNode json = request().body().asJson();
		if (json == null) {
			return Promise.pure((Result)badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				ObjectNode resultInfo = Json.newObject();
				ObjectId creatorDbId = new ObjectId(session().get("user"));
				final CommonQuery q = Utils.parseJson(json.get("query"));
				final String cname = json.get("collectionName").toString();
				final int limit = (json.has("limit"))?json.get("limit").asInt():-1;
				CollectionObject ccid = null;
				if (!isCollectionCreated(creatorDbId, cname)){
					CollectionObject collection = new CollectionObject();
					collection.getDescriptiveData().setLabel(new MultiLiteral(cname).fillDEF());
					boolean success = internalAddCollection(collection, CollectionType.SimpleCollection, creatorDbId, resultInfo);
					if (!success)
						return Promise.pure((Result)badRequest("Expecting Json query"));
					ccid  = collection;
				} else {
					 List<CollectionObject> col = DB.getCollectionObjectDAO().getByLabel(Language.DEFAULT, cname);
					 ccid = col.get(0);
				}

				EuropeanaSpaceSource src = new EuropeanaSpaceSource();
				src.setUsingCursor(true);
				
				return internalImport(src, ccid, q, limit, resultInfo, true, true);
				
			} catch (Exception e) {
				e.printStackTrace();
				return Promise.pure((Result)badRequest(e.getMessage()));
			}
		}
	}
	
	/**
	 * creates a new collection corresponding to a collection in Europeana and
	 * collects all its items.
	 *
	 * @param id
	 * @return
	 */
	public static Promise<Result> createAndFillEuropeanaCollection(String id, int limit) {
		CollectionObject collection = new CollectionObject();
		collection.getDescriptiveData()
				.setLabel(new MultiLiteral(id).fillDEF());
		ObjectNode resultInfo = Json.newObject();
		ObjectId creatorDbId = new ObjectId(session().get("user"));
		boolean success = internalAddCollection(collection,
				CollectionType.SimpleCollection, creatorDbId, resultInfo);
		if (!success)
			return Promise.pure((Result)badRequest(resultInfo));
		CommonQuery q = new CommonQuery();
		q.page = "1";
		q.pageSize = "20";
		EuropeanaCollectionSpaceSource src = new EuropeanaCollectionSpaceSource(
				id);

		q.page = 1 + "";
		SourceResponse result = src.getAllResults(q);

		int total = result.totalCount;
		for (WithResource<?, ?> item : result.items.getCulturalCHO()) {
			WithResourceController.internalAddRecordToCollection(collection
					.getDbId().toString(), (RecordResource) item, F.Option
					.None(), resultInfo);
		}
		;

		Promise<Integer> promiseOfInt = Promise
				.promise(new Function0<Integer>() {
					public Integer apply() {
						SourceResponse result;
						System.out.println("more pages?");
						int page = 1;
						int pageSize = 20;
						while ((page * pageSize) < total) {
							page++;
							q.page = page + "";
							result = src.getAllResults(q);
							for (WithResource<?, ?> item : result.items
									.getCulturalCHO()) {
								WithResourceController
										.internalAddRecordToCollection(
												collection.getDbId().toString(),
												(RecordResource) item,
												F.Option.None(), resultInfo);
							}
							;
							System.out.println("more pages? " + page + " of "
									+ result.totalCount);
						}
						System.out.println("Done? " + page);
						return 0;
					}
				});

		if (resultInfo.has("error"))
			return badRequest(resultInfo);
		return ok(resultInfo);
	}

	public static Result sortCollectionObject(String collectionId) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(collectionId);
			int entryCount = DB
					.getCollectionObjectDAO()
					.getById(collectionDbId,
							Arrays.asList("administrative.entryCount"))
					.getAdministrative().getEntryCount();
			Logger.info("Sorting collection "+collectionId);
			List<RecordResource> records = DB.getRecordResourceDAO()
					.getByCollectionBetweenPositions(collectionDbId, 0,
							Math.min(entryCount, 1000));
			RecordResource<?>[] array = records.toArray(new RecordResource<?>[]{});
			Arrays.sort(array,Utils.compareThumbs);
			Logger.info("Items sorted based on image quality");
			for (int i = 0, pos = 0; i < array.length; i++) {
				RecordResource<?> recordResource = array[i];
				for (CollectionInfo ci : recordResource.getCollectedIn()) {
					if (ci.getCollectionId().equals(collectionDbId)) {
						ci.setPosition(pos++);
					}
				}
				DB.getRecordResourceDAO().makePermanent(recordResource);
				Logger.info(pos +"th item was updated");
			}
			return ok();
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * Creates a new Collection from the JSON body
	 *
	 * @param collectionType
	 *            the collection type that can take values of :
	 *            {SimpleCollection, Exhibition }
	 * @return the newly created collection
	 */
	public static Result createCollectionObject() {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		CollectionType colType = null;
		JsonNode adm = json.get("administrative");
		if (adm != null) {
			JsonNode ct = adm.get("collectionType");
			if (ct != null)
				colType = CollectionType.valueOf(ct.asText());
		}
		try {
			if ((colType == null) && (json == null)) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			if (session().get("user") == null) {
				error.put("error", "No rights for WITH resource creation");
				return forbidden(error);
			}
			ObjectId creatorDbId = new ObjectId(session().get("user"));
			CollectionObject collection = Json.fromJson(json,
					CollectionObject.class);
			boolean success = internalAddCollection(collection, colType,
					creatorDbId, error);
			if (!success)
				return badRequest(error);
			return ok(Json.toJson(collectionWithMyAccessData(
					collection,
					AccessManager.effectiveUserIds(session().get(
							"effectiveUserIds")))));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}
	
	private static boolean isCollectionCreated(ObjectId creatorDbId, String name){
		return DB.getCollectionObjectDAO().existsForOwnerAndLabel(creatorDbId, null,Arrays.asList(name));
	}

	private static boolean internalAddCollection(CollectionObject collection,
			CollectionType colType, ObjectId creatorDbId, ObjectNode error) {
		if (collection.getDescriptiveData().getLabel() == null) {
			error.put("error", "Missing collection title");
			return false;
		}
		if (collection.getDescriptiveData().getLabel().isEmpty()) {
			error.put("error", "Missing collection title");
			return false;
		}
		if (DB.getCollectionObjectDAO().existsForOwnerAndLabel(
				creatorDbId,
				null,
				collection.getDescriptiveData().getLabel()
						.get(Language.DEFAULT))) {
			error.put("error", "Not unique collection title");
			return false;
		}
		Set<ConstraintViolation<CollectionObject>> violations = Validation
				.getValidator().validate(collection);
		if (!violations.isEmpty()) {
			ArrayNode properties = Json.newObject().arrayNode();
			for (ConstraintViolation<CollectionObject> cv : violations) {
				properties.add(Json.parse("{\"" + cv.getPropertyPath()
						+ "\":\"" + cv.getMessage() + "\"}"));
			}
			error.put("error", properties);
			return false;
		}
		// Fill with all the administrative metadata
		collection.getAdministrative().setCollectionType(colType);
		collection.setResourceType(WithResourceType.CollectionObject);
		collection.getAdministrative().setWithCreator(creatorDbId);
		collection.getAdministrative().setCreated(new Date());
		collection.getAdministrative().setLastModified(new Date());
		if (collection.getAdministrative() instanceof CollectionAdmin) {
			collection.getAdministrative().setEntryCount(0);
		}
		DB.getCollectionObjectDAO().makePermanent(collection);
		DB.getCollectionObjectDAO().updateWithURI(collection.getDbId(),
				"/collection/" + collection.getDbId());
		return true;
	}

	/* Find a unique dummy title for the user exhibition */
	/**
	 * @param user
	 * @return
	 */
	private static MultiLiteral createExhibitionDummyTitle() {
		return new MultiLiteral(Language.DEFAULT, "New Exhibition ("
				+ new Date() + ")");
	}

	/**
	 * Retrieve a resource metadata. If the format is defined the specific
	 * serialization of the object is returned
	 *
	 * @param id
	 *            the resource id
	 * @return the resource metadata
	 */
	public static Result getCollectionObject(String id) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.READ,
					collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().get(
						new ObjectId(id));
				/*
				 * List<RecordResource> firstEntries =
				 * DB.getCollectionObjectDAO() .getFirstEntries(collectionDbId,
				 * 3); result = (ObjectNode) Json.toJson(collection);
				 * result.put("firstEntries", Json.toJson(firstEntries));
				 */
				return ok(Json.toJson(collection));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * Deletes all resource metadata
	 *
	 * @param id
	 *            the resource id
	 * @return success message
	 */
	// TODO: cascaded delete (if needed)
	public static Result deleteCollectionObject(String id) {
		ObjectNode result = Json.newObject();
		Locks locks = null;
		try {
			locks = Locks.create().write("Collection #" + id).acquire();
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.DELETE,
					collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().get(
						collectionDbId);
				// TODO: have to test that this works
				DB.getRecordResourceDAO().removeAllRecordsFromCollection(
						collectionDbId);
				DB.getCollectionObjectDAO().makeTransient(collection);

				result.put("message", "Resource was deleted successfully");
				return ok(result);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	/**
	 * Edits the WITH resource according to the JSON body. For every field
	 * mentioned in the JSON body it either edits the existing one or it adds it
	 * (in case it doesn't exist)
	 *
	 * @param id
	 * @return the edited resource
	 */
	public static Result editCollectionObject(String id) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId collectionDbId = new ObjectId(id);
		try {
			Result response = errorIfNoAccessToCollection(Action.EDIT,
					collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			if (json == null) {
				result.put("error", "Invalid JSON");
				return badRequest(result);
			}
			CollectionObject collectionChanges = Json.fromJson(json,
					CollectionObject.class);
			ObjectId creatorDbId = new ObjectId(session().get("user"));
			if ((collectionChanges.getDescriptiveData().getLabel() != null)
					&& DB.getCollectionObjectDAO().existsOtherForOwnerAndLabel(
							creatorDbId,
							null,
							collectionChanges.getDescriptiveData().getLabel()
									.get(Language.DEFAULT), collectionDbId)) {
				ObjectNode error = Json.newObject();
				error.put("error", "Not unique collection title");
				return badRequest(error);
			}
			DB.getCollectionObjectDAO().editCollection(collectionDbId, json);
			return ok(Json.toJson(DB.getCollectionObjectDAO().get(
					collectionDbId)));
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result countMyAndShared() {
		ObjectNode result = Json.newObject().objectNode();
		List<String> effectiveUserIds = AccessManager
				.effectiveUserIds(session().get("effectiveUserIds"));
		if (effectiveUserIds.isEmpty()) {
			return badRequest("You should be signed in as a user.");
		} else {
			result = DB.getCollectionObjectDAO().countMyAndSharedCollections(
					AccessManager.toObjectIds(effectiveUserIds));
			return ok(result);
		}
	}

	public static Result list(Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<MyPlayList> recursivelyAccessedByUserOrGroup,
			Option<String> creator, Option<Boolean> isPublic,
			Option<Boolean> isExhibition, Boolean collectionHits, int offset,
			int count) {
		ObjectNode result = Json.newObject().objectNode();
		ArrayNode collArray = Json.newObject().arrayNode();
		List<CollectionObject> userCollections;
		List<String> effectiveUserIds = AccessManager
				.effectiveUserIds(session().get("effectiveUserIds"));
		List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = accessibleByUserOrGroup(
				directlyAccessedByUserOrGroup, recursivelyAccessedByUserOrGroup);
		Boolean isExhibitionBoolean = isExhibition.isDefined() ? isExhibition
				.get() : null;
		ObjectId creatorId = null;
		if (creator.isDefined()) {
			User creatorUser = DB.getUserDAO().getByUsername(creator.get());
			if (creatorUser != null)
				creatorId = creatorUser.getDbId();
		}
		if (effectiveUserIds.isEmpty()
				|| (isPublic.isDefined() && (isPublic.get() == true))) {
			// if not logged or ask for public collections, return all public
			// collections
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info = DB
					.getCollectionObjectDAO().getPublicAndByAcl(
							accessedByUserOrGroup, creatorId,
							isExhibitionBoolean, collectionHits, offset, count);
			userCollections = info.x;
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}
			for (CollectionObject collection : userCollections) {
				ObjectId withCreator = collection.getAdministrative()
						.getWithCreator();
				ObjectNode c = (ObjectNode) Json.toJson(collection);
				if (effectiveUserIds.isEmpty())
					c.put("access", Access.READ.toString());
				collArray.add(c);
			}
			result.put("collectionsOrExhibitions", collArray);
			return ok(result);
		} else { // logged in, check if super user, if not, restrict query to
					// accessible by effectiveUserIds
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info;
			if (!AccessManager.isSuperUser(effectiveUserIds.get(0)))
				info = DB.getCollectionObjectDAO().getByLoggedInUsersAndAcl(
						AccessManager.toObjectIds(effectiveUserIds),
						accessedByUserOrGroup, creatorId, isExhibitionBoolean,
						collectionHits, offset, count);
			else
				info = DB.getCollectionObjectDAO().getByAcl(
						accessedByUserOrGroup, creatorId, isExhibitionBoolean,
						collectionHits, offset, count);
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}
			List<ObjectNode> collections = collectionsWithMyAccessData(info.x,
					effectiveUserIds);
			for (ObjectNode c : collections)
				collArray.add(c);
			result.put("collectionsOrExhibitions", collArray);
			return ok(result);
		}
	}

	public static Result listShared(Boolean direct,
			Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<MyPlayList> recursivelyAccessedByUserOrGroup,
			Option<Boolean> isExhibition, boolean collectionHits, int offset,
			int count) {
		ObjectNode result = Json.newObject().objectNode();
		ArrayNode collArray = Json.newObject().arrayNode();
		List<String> effectiveUserIds = AccessManager
				.effectiveUserIds(session().get("effectiveUserIds"));
		Boolean isExhibitionBoolean = isExhibition.isDefined() ? isExhibition
				.get() : null;
		if (effectiveUserIds.isEmpty()) {
			return forbidden(Json
					.parse("\"error\", \"Must specify user for the collection\""));
		} else {
			ObjectId userId = new ObjectId(effectiveUserIds.get(0));
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = new ArrayList<List<Tuple<ObjectId, Access>>>();
			accessedByUserOrGroup = accessibleByUserOrGroup(
					directlyAccessedByUserOrGroup,
					recursivelyAccessedByUserOrGroup);
			List<Tuple<ObjectId, Access>> accessedByLoggedInUser = new ArrayList<Tuple<ObjectId, Access>>();
			if (direct) {
				accessedByLoggedInUser.add(new Tuple<ObjectId, Access>(userId,
						Access.READ));
				accessedByUserOrGroup.add(accessedByLoggedInUser);
			} else {// indirectly: include collections for which user has access
					// via userGoup sharing
				for (String effectiveId : effectiveUserIds) {
					accessedByLoggedInUser.add(new Tuple<ObjectId, Access>(
							new ObjectId(effectiveId), Access.READ));
				}
				accessedByUserOrGroup.add(accessedByLoggedInUser);
			}
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info = DB
					.getCollectionObjectDAO().getSharedAndByAcl(
							accessedByUserOrGroup, userId, isExhibitionBoolean,
							collectionHits, offset, count);
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}

			List<ObjectNode> collections = collectionsWithMyAccessData(info.x,
					effectiveUserIds);
			for (ObjectNode c : collections)
				collArray.add(c);
			result.put("collectionsOrExhibitions", collArray);
			return ok(result);
		}
	}

	// input parameter lists' (directlyAccessedByUserOrGroup etc) intended
	// meaning is AND of its entries
	// returned list of lists accessedByUserOrGroup represents AND of OR entries
	// i.e. each entry in directlyAccessedByUserName for example has to be
	// included in a separate list!
	private static List<List<Tuple<ObjectId, Access>>> accessibleByUserOrGroup(
			Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<MyPlayList> recursivelyAccessedByUserOrGroup) {
		List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = new ArrayList<List<Tuple<ObjectId, Access>>>();
		if (directlyAccessedByUserOrGroup.isDefined()) {
			MyPlayList directlyUserNameList = directlyAccessedByUserOrGroup
					.get();
			for (StringTuple userAccess : directlyUserNameList.list) {
				List<Tuple<ObjectId, Access>> directlyAccessedByUser = new ArrayList<Tuple<ObjectId, Access>>();
				UserOrGroup userOrGroup = getUserOrGroup(userAccess.x);
				if (userOrGroup != null) {
					directlyAccessedByUser.add(new Tuple<ObjectId, Access>(
							userOrGroup.getDbId(), Access.valueOf(userAccess.y
									.toUpperCase())));
					accessedByUserOrGroup.add(directlyAccessedByUser);
				}
			}
		}
		// TODO: add support for userGroups in recursively!!!!!
		if (recursivelyAccessedByUserOrGroup.isDefined()) {
			MyPlayList recursivelyUserNameList = recursivelyAccessedByUserOrGroup
					.get();
			for (StringTuple userAccess : recursivelyUserNameList.list) {
				List<Tuple<ObjectId, Access>> recursivelyAccessedByUser = new ArrayList<Tuple<ObjectId, Access>>();
				User user = DB.getUserDAO().getByUsername(userAccess.x);
				ObjectId userId = user.getDbId();
				Access access = Access.valueOf(userAccess.y.toUpperCase());
				recursivelyAccessedByUser.add(new Tuple<ObjectId, Access>(
						userId, access));
				Set<ObjectId> groupIds = user.getUserGroupsIds();
				for (ObjectId groupId : groupIds) {
					recursivelyAccessedByUser.add(new Tuple<ObjectId, Access>(
							groupId, access));
				}
				accessedByUserOrGroup.add(recursivelyAccessedByUser);
			}
		}
		return accessedByUserOrGroup;
	}

	private static UserOrGroup getUserOrGroup(String username) {
		User user = DB.getUserDAO().getByUsername(username);
		UserOrGroup userOrGroup = null;
		if (user != null) {
			userOrGroup = user;
		} else {
			UserGroup userGroup = DB.getUserGroupDAO().getByName(username);
			if (userGroup != null) {
				userOrGroup = userGroup;
			}
		}
		return userOrGroup;
	}

	private static List<ObjectNode> collectionsWithMyAccessData(
			List<CollectionObject> userCollections,
			List<String> effectiveUserIds) {
		List<ObjectNode> collections = new ArrayList<ObjectNode>(
				userCollections.size());
		for (CollectionObject collection : userCollections) {
			// List<String> titles = collection.getDescriptiveData().getLabel()
			// .get(Language.DEFAULT);
			// if ((titles != null) && !titles.get(0).equals("_favorites")) {
			collections.add(collectionWithMyAccessData(collection,
					effectiveUserIds));
			// }
		}
		return collections;
	}

	private static ObjectNode collectionWithMyAccessData(
			CollectionObject userCollection, List<String> effectiveUserIds) {
		ObjectNode c = (ObjectNode) Json.toJson(userCollection);
		Access maxAccess = AccessManager.getMaxAccess(userCollection
				.getAdministrative().getAccess(), effectiveUserIds);
		if (maxAccess.equals(Access.NONE))
			maxAccess = Access.READ;
		c.put("myAccess", maxAccess.toString());
		return c;
	}

	public static void addCollectionToList(int index,
			List<CollectionObject> collectionsOrExhibitions,
			List<ObjectId> colls, List<String> effectiveUserIds) {
		if (index < colls.size()) {
			ObjectId id = colls.get(index);
			CollectionObject c = DB.getCollectionObjectDAO().getById(id);
			if (effectiveUserIds.isEmpty()) {
				if (c.getAdministrative().getAccess().getIsPublic())
					collectionsOrExhibitions.add(c);
			} else {
				Access maxAccess = AccessManager.getMaxAccess(c
						.getAdministrative().getAccess(), effectiveUserIds);
				if (!maxAccess.equals(Access.NONE))
					collectionsOrExhibitions.add(c);
			}
		}
	}

	// If isExhibition is undefined, returns (max) countPerType collections and
	// countPerType exhibitions, i.e. (max) 2*countPerType
	// collectionsOrExhibitions
	public static Result getFeatured(String userOrGroupName,
			Option<Boolean> isExhibition, int offset, int countPerType) {
		Page page = null;
		UserGroup userGroup = DB.getUserGroupDAO().getByName(userOrGroupName);
		if (userGroup != null) {
			if (userGroup instanceof Organization)
				page = ((Organization) userGroup).getPage();
			else if (userGroup instanceof Project)
				page = ((Project) userGroup).getPage();
		} else {
			User user = DB.getUserDAO().getByUsername(userOrGroupName);
			if (user != null) {
				page = user.getPage();
			}
		}
		if (page != null) {
			List<String> effectiveUserIds = AccessManager
					.effectiveUserIds(session().get("effectiveUserIds"));
			ObjectNode result = Json.newObject().objectNode();
			int start = offset * countPerType;
			int collectionsSize = page.getFeaturedCollections().size();
			int exhibitionsSize = page.getFeaturedExhibitions().size();
			List<CollectionObject> collectionsOrExhibitions = new ArrayList<CollectionObject>();
			if (!isExhibition.isDefined()) {
				for (int i = start; (i < (start + countPerType))
						&& (i < collectionsSize); i++) {
					addCollectionToList(i, collectionsOrExhibitions,
							page.getFeaturedCollections(), effectiveUserIds);
					addCollectionToList(i, collectionsOrExhibitions,
							page.getFeaturedExhibitions(), effectiveUserIds);
				}
			} else {
				if (!isExhibition.get()) {
					for (int i = start; (i < (start + countPerType))
							&& (i < collectionsSize); i++)
						addCollectionToList(i, collectionsOrExhibitions,
								page.getFeaturedCollections(), effectiveUserIds);
				} else {
					for (int i = start; (i < (start + countPerType))
							&& (i < exhibitionsSize); i++)
						addCollectionToList(i, collectionsOrExhibitions,
								page.getFeaturedExhibitions(), effectiveUserIds);
				}
			}
			ArrayNode collArray = Json.newObject().arrayNode();
			List<ObjectNode> collections = collectionsWithMyAccessData(
					collectionsOrExhibitions, effectiveUserIds);
			for (ObjectNode c : collections)
				collArray.add(c);
			result.put("totalCollections", collectionsSize);
			result.put("totalExhibitions", exhibitionsSize);
			result.put("collectionsOrExhibitions", collArray);
			// TODO: put collection and exhibition hits in response
			return ok(result);
		} else
			return badRequest("User or group with name " + userOrGroupName
					+ " does not exist or has no specified page.");

	}

	/**
	 * @return
	 */
	public static Result getFavoriteCollection() {
		if (session().get("user") == null) {
			return forbidden();
		}
		ObjectId userId = new ObjectId(session().get("user"));
		CollectionObject favorite;
		ObjectId favoritesId;
		if ((favorite = DB.getCollectionObjectDAO().getByOwnerAndLabel(userId,
				null, "_favorites")) == null) {
			favoritesId = createFavorites(userId);
		} else {
			favoritesId = favorite.getDbId();
		}
		return getCollectionObject(favoritesId.toString());

	}

	public static ObjectId createFavorites(ObjectId userId) {
		CollectionObject fav = new CollectionObject();
		fav.getAdministrative().setCreated(new Date());
		fav.getAdministrative().setWithCreator(userId);
		fav.getDescriptiveData().setLabel(
				new MultiLiteral(Language.DEFAULT, "_favorites"));
		DB.getCollectionObjectDAO().makePermanent(fav);
		return fav.getDbId();
	}

	/**
	 * List all Records from a Collection using a start item and a page size
	 */
	public static Result listRecordResources(String collectionId,
			String contentFormat, int start, int count) {
		ObjectNode result = Json.newObject();
		ObjectId colId = new ObjectId(collectionId);
		Locks locks = null;
		try {
			locks = Locks.create().read("Collection #" + collectionId)
					.acquire();
			Result response = errorIfNoAccessToCollection(Action.READ, colId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {

				/*
				 * List<String> retrievedFields = new ArrayList<String>(
				 * Arrays.asList("descriptiveData.label",
				 * "descriptiveData.description", "media", "collectedIn"));
				 */
				List<RecordResource> records = DB.getRecordResourceDAO()
						.getByCollectionBetweenPositions(colId, start,
								start + count);
				if (records == null) {
					result.put("message",
							"Cannot retrieve records from database!");
					return internalServerError(result);
				}
				ArrayNode recordsList = Json.newObject().arrayNode();
				int position = start;
				for (RecordResource e : records) {
					// filter out records to which the user has no read access
					if (!response.toString().equals(ok().toString())) {
						recordsList.add(Json.toJson(new RecordResource(e
								.getDbId())));
					} else {
						// filter out all context annotations that do not refer
						// to this collection-position
						List<ContextData> contextAnns = e.getContextData();
						List<ContextData> filteredContextAnns = new ArrayList<ContextData>();
						for (ContextData ca : contextAnns) {
							if (ca.getTarget().getCollectionId().equals(colId)
									&& (ca.getTarget().getPosition() == position)) {
								filteredContextAnns.add(ca);
								break;
							}
						}
						e.setContextData(filteredContextAnns);
						if (e.getContent() != null) {
							if (contentFormat.equals("contentOnly")
									&& (e.getContent() != null)) {
								recordsList.add(Json.toJson(e.getContent()));
							} else if (contentFormat.equals("noContent")) {
								e.getContent().clear();
							} else if (e.getContent()
									.containsKey(contentFormat)) {
								HashMap<String, String> newContent = new HashMap<String, String>(
										1);
								newContent.put(contentFormat, (String) e
										.getContent().get(contentFormat));
								e.setContent(newContent);
							}
						}
						recordsList.add(Json.toJson(e));
					}
					position += 1;
				}
				result.put(
						"entryCount",
						DB.getCollectionObjectDAO()
								.getById(
										colId,
										new ArrayList<String>(
												Arrays.asList("administrative.entryCount")))
								.getAdministrative().getEntryCount());
				result.put("records", recordsList);
				return ok(result);
			}
		} catch (Exception e1) {
			result.put("error", e1.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}
	
	
	/**
	 * List all Records from a Collection using a start item and a page size
	 */
	public static Result xlistRecordResources(String collectionId, String contentFormat, int start, int count) {
		ObjectNode result = Json.newObject();
		ObjectId colId = new ObjectId(collectionId);
		Locks locks = null;
		
		JsonNode json = request().body().asJson();
//		log.info("JSON " + json.toString());
		try {
			
			locks = Locks.create().read("Collection #" + collectionId).acquire();

			Result response = errorIfNoAccessToCollection(Action.READ, colId);

			if (!response.toString().equals(ok().toString())) {
				return response;
			} else {
//				System.out.println("QUERYING FOR VIEW");
				ElasticSearcher es = new ElasticSearcher();
				
				QueryBuilder query = CollectionIndexController.getIndexCollectionQuery(colId, json);
				
//				log.info("QUERY " + query.toString());
//				log.info("QUERC " + start + " " + count);
//				
				SearchOptions so = new SearchOptions(start, start + count);
				
				SearchResponse res = es.execute(query, so);
				SearchHits sh = res.getHits();
				
				long totalHits = sh.getTotalHits();
				
//				System.out.println("RESULTS " + sh.getHits().length);

				List<String> retrievedFields = new ArrayList<String>(
						Arrays.asList("descriptiveData.label",
								"descriptiveData.description", "media", "collectedIn"));
				
				List<String> ids = new ArrayList<>();
				for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
					SearchHit hit = iter.next();
					ids.add(hit.getId());
//					log.info("ID " + hit.getId());
				}
				
				List<RecordResource> records = DB.getRecordResourceDAO().getByCollectionIds(colId, ids);
				
//				System.out.println("RESULTS DB " + records.size());
//				for (RecordResource rr : records) {
//					log.info("DB " + rr.getDbId());
//				}

				if (records == null) {
					result.put("message",
							"Cannot retrieve records from database!");
					return internalServerError(result);
				}
				ArrayNode recordsList = Json.newObject().arrayNode();
				int position = start;
				for (RecordResource e : records) {
					// filter out records to which the user has no read access
					if (!response.toString().equals(ok().toString())) {
						recordsList.add(Json.toJson(new RecordResource(e
								.getDbId())));
					} else {
						// filter out all context annotations that do not refer
						// to this collection-position
						List<ContextData> contextAnns = e.getContextData();
						List<ContextData> filteredContextAnns = new ArrayList<ContextData>();
						for (ContextData ca : contextAnns) {
							if (ca.getTarget().getCollectionId().equals(colId)
									&& (ca.getTarget().getPosition() == position))
								filteredContextAnns.add(ca);
						}
						e.setContextData(filteredContextAnns);
						if (e.getContent() != null) {
							if (contentFormat.equals("contentOnly")
									&& (e.getContent() != null)) {
								recordsList.add(Json.toJson(e.getContent()));
							} else if (contentFormat.equals("noContent")) {
								e.getContent().clear();
							} else if (e.getContent()
									.containsKey(contentFormat)) {
								HashMap<String, String> newContent = new HashMap<String, String>(
										1);
								newContent.put(contentFormat, (String) e
										.getContent().get(contentFormat));
								e.setContent(newContent);
							}
						}
						recordsList.add(Json.toJson(e));
					}
					position += 1;
				}
				result.put("entryCount", totalHits);
				result.put("records", recordsList);
				return ok(result);
			}
		} catch (Exception e1) {
			result.put("error", e1.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	public static Result listUsersWithRights(String collectionId) {
		ArrayNode result = Json.newObject().arrayNode();
		List<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("administrative.access"));
		CollectionObject collection = DB.getCollectionObjectDAO().getById(
				new ObjectId(collectionId), retrievedFields);
		WithAccess access = collection.getAdministrative().getAccess();
		for (AccessEntry ae : access.getAcl()) {
			if(ae.getLevel().ordinal() > 0) {
				ObjectId userId = ae.getUser();
				User user = DB.getUserDAO().getById(userId, null);
				Access accessRights = ae.getLevel();
				if (user != null) {
					result.add(userOrGroupJson(user, accessRights));
				} else {
					UserGroup usergroup = DB.getUserGroupDAO().get(userId);
					if (usergroup != null)
						result.add(userOrGroupJson(usergroup, accessRights));
					else
						return internalServerError("User with id " + userId
								+ " cannot be retrieved from db");
				}
			}
		}
		return ok(result);
	}

	private static ObjectNode userOrGroupJson(UserOrGroup user,
			Access accessRights) {
		ObjectNode userJSON = Json.newObject();
		userJSON.put("userId", user.getDbId().toString());
		userJSON.put("username", user.getUsername());
		if (user instanceof User) {
			userJSON.put("category", "user");
			userJSON.put("firstName", ((User) user).getFirstName());
			userJSON.put("lastName", ((User) user).getLastName());
		} else
			userJSON.put("category", "group");
		String image = UserAndGroupManager.getImageBase64(user);
		userJSON.put("accessRights", accessRights.toString());
		if (image != null) {
			userJSON.put("image", image);
		}
		return userJSON;
	}
	
	public static Result getCollectionIndex(String id) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.READ,
					collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().get(
						new ObjectId(id));
				/*
				 * List<RecordResource> firstEntries =
				 * DB.getCollectionObjectDAO() .getFirstEntries(collectionDbId,
				 * 3); result = (ObjectNode) Json.toJson(collection);
				 * result.put("firstEntries", Json.toJson(firstEntries));
				 */
				return ok(Json.toJson(collection));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}



	private static Integer doTheImport(ObjectNode resultInfo, final CommonQuery q, final String cid,
			EuropeanaSpaceSource src, int total, int firstPageCount1) {
		SourceResponse result;
		int page = 1;
		int pageSize = 20;
		int itemsCount = firstPageCount1;
		while (itemsCount < total) {
			page++;
			q.page = page + "";
			result = src.getResults(q);
			for (WithResource<?, ?> item : result.items.getCulturalCHO()) {
				WithResourceController.internalAddRecordToCollection(cid,
						(RecordResource) item, F.Option.None(), resultInfo,true);
				itemsCount++;
			}
		}
		return 0;
	}
}
