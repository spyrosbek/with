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

import java.util.Set;

import javax.validation.ConstraintViolation;

import model.resources.WithResource;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class WithResourceController extends Controller {
	public static final ALogger log = Logger.of(WithResourceController.class);

	/**
	 * Retrieve a resource metadata. If the format is defined the specific
	 * serialization of the object is returned
	 *
	 * @param id
	 *            the resource id
	 * @param format
	 *            the resource serialization
	 * @return the resource metadata
	 */
	public static Result getWithResource(String id, Option<String> format) {
		ObjectNode result = Json.newObject();
		try {
			WithResource resource = DB.getWithResourceDAO().get(
					new ObjectId(id));
			if (resource == null) {
				log.error("Cannot retrieve resource from database");
				result.put("error", "Cannot retrieve resource from database");
				return internalServerError(result);
			}
			if (!AccessManager.checkAccess(resource.getAdministrative()
					.getAccess(), session().get("effectiveUserIds"),
					Action.READ)) {
				result.put("error",
						"User does not have read-access for the resource");
				return forbidden(result);
			}
			if (format.isDefined() && resource.getContent().containsKey(format)) {
				return ok(resource.getContent().get(format).toString());
			}
			return ok(Json.toJson(resource));
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}

	}

	/**
	 * Deletes all resource metadata or just a serialization
	 *
	 * @param id
	 *            the resource id
	 * @param format
	 *            the resource serialization
	 * @return success message
	 */
	// TODO: cascaded delete (if needed)
	public static Result deleteWithResource(String id, Option<String> format) {
		ObjectNode result = Json.newObject();
		try {
			WithResource resource = DB.getWithResourceDAO().get(
					new ObjectId(id));
			if (resource == null) {
				log.error("Cannot retrieve resource from database");
				result.put("error", "Cannot retrieve resource from database");
				return internalServerError(result);
			}
			if (!AccessManager.checkAccess(resource.getAdministrative()
					.getAccess(), session().get("effectiveUserIds"),
					Action.DELETE)) {
				result.put("error",
						"User does not have the right to delete the resource");
				return forbidden(result);
			}
			if (format.isDefined() && resource.getContent().containsKey(format)) {
				resource.getContent().remove(format);
				result.put("message",
						"Serialization of resource was deleted successfully");
				return ok(result);
			}
			DB.getWithResourceDAO().makeTransient(resource);
			result.put("message", "Resource was deleted successfully");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	// TODO check restrictions (unique fields e.t.c)
	public static Result createWithResource() {

		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			if (session().get("user") == null) {
				error.put("error", "No rights for WITH resource creation");
				return forbidden(error);
			}
			ObjectId creator = new ObjectId(session().get("user"));
			String resourceType = json.get("resourceType").asText();
			Class<?> clazz = Class.forName("model.resources." + resourceType);
			WithResource resource = (WithResource) Json.fromJson(json, clazz);
			Set<ConstraintViolation<WithResource>> violations = Validation
					.getValidator().validate(resource);
			if (!violations.isEmpty()) {
				ArrayNode properties = Json.newObject().arrayNode();
				for (ConstraintViolation<WithResource> cv : violations) {
					properties.add(Json.parse("{\"" + cv.getPropertyPath()
							+ "\":\"" + cv.getMessage() + "\"}"));
				}
				error.put("error", properties);
				return badRequest(error);
			}
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
		return TODO;
	}

}