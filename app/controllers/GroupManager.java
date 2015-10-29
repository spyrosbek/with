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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.Rights.Access;
import model.User;
import model.UserGroup;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.Tuple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class GroupManager extends Controller {

	public static final ALogger log = Logger.of(UserGroup.class);

	public enum GroupType {
		All, Organization, Project, UserGroup
	}

	/**
	 * Creates a {@link UserGroup} with the specified user as administrator and
	 * with the given body as JSON.
	 * <p>
	 * The name of the group must be unique. If the administrator is not
	 * provided as a parameter the administrator of the group becomes the user
	 * who made the call.
	 *
	 * @param adminId
	 *            the administrator id
	 * @param adminUsername
	 *            the administrator username
	 * @return the JSON of the new group
	 */
	public static Result createGroup(String adminId, String adminUsername,
			String groupType) {

		ObjectId admin;
		UserGroup newGroup = null;
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				return badRequest("Invalid JSON");
			}
			if (!json.has("username")) {
				return badRequest("Must specify name for the group");
			}
			if (!uniqueGroupName(json.get("username").asText())) {
				return badRequest("Group name already exists! Please specify another name.");
			}
			Class<?> clazz = Class.forName("model."
					+ groupType);
			newGroup = (UserGroup) Json.fromJson(json, clazz);
			if (adminId != null) {
				admin = new ObjectId(adminId);
			} else if (adminUsername != null) {
				admin = DB.getUserDAO().getByUsername(adminUsername).getDbId();
			} else {
				if ((adminId = AccessManager.effectiveUserId(session().get(
						"effectiveUserIds"))).isEmpty()) {
					return internalServerError("Must specify administrator of group");
				}
				admin = new ObjectId(AccessManager.effectiveUserId(session()
						.get("effectiveUserIds")));
			}
			newGroup.addAdministrator(admin);
			newGroup.getUsers().add(admin);
			Set<ConstraintViolation<UserGroup>> violations = Validation
					.getValidator().validate(newGroup);
			for (ConstraintViolation<UserGroup> cv : violations) {
				return badRequest("[" + cv.getPropertyPath() + "] "
						+ cv.getMessage());
			}
			try {
				DB.getUserGroupDAO().makePermanent(newGroup);
				Set<ObjectId> parentGroups = newGroup.getParentGroups();
				parentGroups.add(newGroup.getDbId());
				User user = DB.getUserDAO().get(admin);
				user.addUserGroups(parentGroups);
				DB.getUserDAO().makePermanent(user);
			} catch (Exception e) {
				log.error("Cannot save group to database!", e);
				return internalServerError("Cannot save group to database!");
			}
			return ok(Json.toJson(newGroup));
		} catch (Exception e) {
			return internalServerError(e.getMessage());
		}
	}

	private static boolean uniqueGroupName(String name) {
		return (DB.getUserGroupDAO().getByName(name) == null);
	}

	private static String capitalizeFirst(String str) {
		return str.substring(0, 1).toUpperCase()
				+ str.substring(1).toLowerCase();
	}

	/**
	 * Edits group metadata and updates them according to the POST body.
	 * <p>
	 * Only the administrator of the group and the superuser have the right to
	 * edit the group.
	 * 
	 * @param groupId
	 *            the group id
	 * @return the updated group metadata
	 */
	public static Result editGroup(String groupId) {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		String adminId = AccessManager.effectiveUserId(session().get(
				"effectiveUserIds"));
		if ((adminId == null) || (adminId.equals(""))) {
			return forbidden("Only administrator of group has the right to edit the group");
		}
		try {
			User admin = DB.getUserDAO().get(new ObjectId(adminId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (group == null) {
				return internalServerError("Cannot retrieve group from database!");
			}
			if (!group.getAdminIds().contains(new ObjectId(adminId))
					&& (!admin.isSuperUser())) {
				return forbidden("Only administrator of group has the right to edit the group");
			}
			UserGroup oldVersion = group;
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectReader updator = objectMapper.readerForUpdating(oldVersion);
			UserGroup newVersion;
			newVersion = updator.readValue(json);
			Set<ConstraintViolation<UserGroup>> violations = Validation
					.getValidator().validate(newVersion);
			for (ConstraintViolation<UserGroup> cv : violations) {
				result.put("message",
						"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			}
			if (!violations.isEmpty()) {
				return badRequest(result);
			}
			if (!uniqueGroupName(newVersion.getUsername())) {
				return badRequest("Group name already exists! Please specify another name.");
			}
			// update group on mongo
			if (DB.getUserGroupDAO().makePermanent(newVersion) == null) {
				log.error("Cannot save group to database!");
				return internalServerError("Cannot save group to database!");
			}
			return ok(Json.toJson(newVersion));
		} catch (IOException e) {
			e.printStackTrace();
			return internalServerError(e.getMessage());
		}
	}

	/**
	 * Deletes a group from the database. The users who participate are not
	 * deleted as well.
	 *
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	public static Result deleteGroup(String groupId) {

		try {
			DB.getUserGroupDAO().deleteById(new ObjectId(groupId));
		} catch (Exception e) {
			log.error("Cannot delete group from database!", e);
			return internalServerError("Cannot delete group from database!");
		}
		return ok("Group deleted succesfully from database");
	}

	/**
	 * Gets the group.
	 *
	 * @param groupId
	 *            the group id
	 * @return the group
	 */
	public static Result getGroup(String groupId) {
		try {
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			return ok(Json.toJson(group));
		} catch (Exception e) {
			log.error("Cannot retrieve group from database!", e);
			return internalServerError("Cannot retrieve group from database!");
		}
	}

	public static Result getUserOrGroupThumbnail(String id) {
		try {
			User user = DB.getUserDAO().getById(new ObjectId(id), null);
			if (user != null) {
				ObjectId photoId = user.getThumbnail();
				return MediaController.getMetadataOrFile(photoId.toString(),
						true);
			} else {
				UserGroup userGroup = DB.getUserGroupDAO()
						.get(new ObjectId(id));
				if (userGroup != null) {
					ObjectId photoId = user.getThumbnail();
					return MediaController.getMetadataOrFile(
							photoId.toString(), true);
				} else
					return badRequest(Json
							.parse("{\"error\":\"User does not exist\"}"));
			}
		} catch (Exception e) {
			return badRequest(Json.parse("{\"error\":\"" + e.getMessage()
					+ "\"}"));
		}
	}

	/**
	 * @param name
	 *            the group name
	 * @return the result
	 */
	public static Result findByGroupName(String name, String collectionId) {
		Function<UserGroup, Status> getGroupJson = (UserGroup group) -> {
			ObjectNode groupJSON = Json.newObject();
			groupJSON.put("groupId", group.getDbId().toString());
			groupJSON.put("username", group.getUsername());
			groupJSON.put("about", group.getAbout());
			if (collectionId != null) {
				Collection collection = DB.getCollectionDAO().getById(
						new ObjectId(collectionId));
				if (collection != null) {
					Access accessRights = collection.getRights().get(
							group.getDbId());
					if (accessRights != null)
						groupJSON.put("accessRights", accessRights.toString());
					else
						groupJSON.put("accessRights", Access.NONE.toString());
				}
			}
			return ok(groupJSON);
		};
		UserGroup group = DB.getUserGroupDAO().getByName(name);
		return getGroupJson.apply(group);
	}

	public static ArrayNode groupsAsJSON(List<UserGroup> groups, ObjectId restrictedById, boolean collectionHits) {
		ArrayNode result = Json.newObject().arrayNode();
		for (UserGroup group : groups) {
			ObjectNode g = (ObjectNode) Json.toJson(group);
			if (collectionHits) {
				Query<Collection> q = DB.getCollectionDAO().createQuery();
				CriteriaContainer[] criteria =  new CriteriaContainer[3];
				criteria[0] = DB.getCollectionDAO().createQuery().criteria("rights." + restrictedById.toHexString()).greaterThanOrEq(1);
				criteria[1] = DB.getCollectionDAO().createQuery().criteria("rights." + group.getDbId().toHexString()).equal(3);
				criteria[2] = DB.getCollectionDAO().createQuery().criteria("rights.isPublic").equal(true);
				q.and(criteria);
				Tuple<Integer, Integer> hits = DB.getCollectionDAO().getHits(q, null);
				g.put("totalCollections", hits.x);
				g.put("totalExhibitions", hits.y);
			}
			result.add(g);
		}
		return result;
	}
	
	public static Set<UserGroup> recursiveDescendants(Set<UserGroup> list, GroupType type) {
		Set<UserGroup> descendantGroups = new HashSet<UserGroup>();
		for (UserGroup group: list) {
			List<UserGroup> descendants = DB.getUserGroupDAO().findByParent(group.getDbId(), type);
			if ( descendants != null) {	
				Set<UserGroup> descendantsSet = recursiveDescendants(new HashSet<UserGroup>(descendants), type);
				descendantGroups.addAll(descendantsSet);
			}
			descendantGroups.add(group);
		}
		return descendantGroups;
	}

	// TODO check user rights for these groups
	public static Result getDescendantGroups(String groupId, String groupType,
			boolean direct, boolean collectionHits) {
		List<UserGroup> childrenGroups;
		List<UserGroup> descendantGroups;
		ObjectId parentId = new ObjectId(groupId);
		GroupType type = GroupType.valueOf(capitalizeFirst(groupType));
		childrenGroups = DB.getUserGroupDAO().findByParent(parentId, type);
		if (childrenGroups != null) {
			if (direct) {
				return ok(groupsAsJSON(childrenGroups, new ObjectId(groupId), collectionHits));
			}
			else {
				descendantGroups = new ArrayList<UserGroup>(recursiveDescendants(new HashSet<UserGroup>(childrenGroups), type));
				return ok(groupsAsJSON(descendantGroups, new ObjectId(groupId), collectionHits));
			}	
		}
		else 
			return ok();
	}
}
