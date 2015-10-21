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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.ElasticUpdater;
import model.Collection;
import model.Rights.Access;
import model.UserGroup;
import model.UserOrGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;

public class RightsController extends Controller {
	public static final ALogger log = Logger.of(CollectionController.class);

	/**
	 * Set access rights for object for user.
	 *
	 * @param colId
	 *            the internal Id of the object you wish to share (or unshare)
	 * @param right
	 *            the right to give ("none" to withdraw previously given right)
	 * @param username
	 *            the username of user to give rights to (or take away from)
	 * @return OK or Error with JSON detailing the problem
	 *
	 */
	public static Result setRights(String colId, String right, String username) {

		ObjectNode result = Json.newObject();
		Collection collection = null;
		try {
			collection = DB.getCollectionDAO().get(new ObjectId(colId));
		} catch (Exception e) {
			log.error("Cannot retrieve collection from database!", e);
			result.put("error", "Cannot retrieve collection from database!");
			return internalServerError(result);
		}
		List<String> userIds = AccessManager.effectiveUserIds(session().get("effectiveUserIds"));
		ObjectId userId = new ObjectId(AccessManager.effectiveUserId(session().get("effectiveUserIds")));
		if (!AccessManager.checkAccess(collection.getRights(), userIds, Action.DELETE)) {
			result.put("error", "Sorry! You do not own this collection so you cannot set rights. "
					+ "Please contact the owner of this collection");
			return forbidden(result);
		}
		// set rights
		// the receiver can be either a User or a UserGroup
		Map<ObjectId, Access> rightsMap = new HashMap<ObjectId, Access>();
		UserOrGroup userOrGroup;
		ObjectId userOrGroupId = null;
		Access access = Access.valueOf(right);
		if (username != null) {
			if ((userOrGroup = DB.getUserDAO().getByUsername(username)) != null) {
				userOrGroupId = userOrGroup.getDbId();
			} else if ((userOrGroup = DB.getUserGroupDAO().getByName(username)) != null) {
				UserGroup group = (UserGroup) userOrGroup;
				userOrGroupId = group.getDbId();
				userOrGroupId = userOrGroup.getDbId();
				if (AccessManager.increasedAccess(collection.getRights().get(userOrGroupId), access)
						&& !group.getUsers().contains(userId)) {
					collection.addForModeration(userOrGroupId);
				} else {
					collection.removeFromModeration(userOrGroupId);
				}
			} else {
				result.put("error", "No user or userGroup with given username");
				return badRequest(result);
			}
		}
		if (right.equals("NONE")) {
			collection.getRights().remove(userOrGroupId);
		} else {
			rightsMap.put(userOrGroupId, Access.valueOf(right));
			collection.getRights().putAll(rightsMap);
		}
		if (DB.getCollectionDAO().makePermanent(collection) == null) {
			result.put("message", "Cannot store collection to database!");
			return internalServerError(result);
		}

		// update collection rights in index
		ElasticUpdater updater = new ElasticUpdater(collection);
		updater.updateCollectionRights();
		result.put("message", "OK");
		return ok(result);
	}

}
