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
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.CollectionRecord;
import model.User;
import model.User.Access;

import org.bson.types.ObjectId;

import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class ExhibitionController extends Controller {

	public static Result createExhibition() {

		ObjectNode result = Json.newObject();
		List<String> userIds = AccessManager.effectiveUserIds(session().get(
				"effectiveUserIds"));
		if (userIds.isEmpty()) {
			result.put("error", "Must specify user for the exhibition");
			return forbidden(result);
		}
		String userId = userIds.get(0);
		User owner = DB.getUserDAO().get(new ObjectId(userId));

		Collection newExhibition = new Collection();
		newExhibition.setCreated(new Date());
		newExhibition.setLastModified(new Date());
		newExhibition.setOwnerId(new ObjectId(userId));
		newExhibition.setExhibition(true);
		newExhibition.setTitle(getAvailableTitle(owner));
		newExhibition.setDescription("Description");

		Set<ConstraintViolation<Collection>> violations = Validation
				.getValidator().validate(newExhibition);
		for (ConstraintViolation<Collection> cv : violations) {
			result.put("error",
					"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			return badRequest(result);
		}
		if (DB.getCollectionDAO().makePermanent(newExhibition) == null) {
			result.put("error", "Cannot save Exhibition to database");
			return internalServerError(result);
		}
		owner.addExhibitionsCreated();
		DB.getUserDAO().makePermanent(owner);
		ObjectNode c = (ObjectNode) Json.toJson(newExhibition);
		c.put("access", Access.OWN.toString());
		User user = DB.getUserDAO().getById(newExhibition.getOwnerId(),
				new ArrayList<String>(Arrays.asList("username")));
		c.put("owner", user.getUsername());
		return ok(c);
	}

	/* Find a unique dummy title for the user exhibition */
	private static String getAvailableTitle(User user) {
		int exhibitionNum = user.getExhibitionsCreated();
		return "Exhibition" + exhibitionNum;
	}

	public static Result listMyExhibitions(int offset, int count) {

		ArrayNode result = Json.newObject().arrayNode();
		try {
			List<String> userIds = AccessManager.effectiveUserIds(session()
					.get("effectiveUserIds"));
			if (userIds.isEmpty()) {
				return forbidden(Json
						.parse("{\"error\" : \"User not specified\"}"));
			}
			String userId = userIds.get(0);
			List<Collection> exhibitions = DB.getCollectionDAO()
					.getExhibitionsByOwner(new ObjectId(userId), offset, count);
			for (Collection exhibition : exhibitions) {
				ObjectNode c = (ObjectNode) Json.toJson(exhibition);
				result.add(c);
			}
			return ok(result);
		} catch (Exception e) {
			return internalServerError(Json.parse("{\"error\":\""
					+ e.getMessage() + "\""));
		}
	}
}
