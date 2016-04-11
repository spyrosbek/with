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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;

import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataType;
import model.basicDataTypes.WithAccess.Access;
import model.resources.CollectionObject;
import model.resources.RecordResource;
import model.resources.WithResource;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

/**
 * @author mariaral
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RecordResourceController extends WithResourceController {

	public static final ALogger log = Logger.of(RecordResourceController.class);

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
	public static Result getRecordResource(String id, Option<String> format) {
		ObjectNode result = Json.newObject();
		try {
			RecordResource record = DB.getRecordResourceDAO().get(
					new ObjectId(id));
			Result response = errorIfNoAccessToRecord(Action.READ,
					new ObjectId(id));
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				// filter out all context annotations refering to collections to
				// which the user has no read access rights
				//filterContextData(record);
				if (format.isDefined()) {
					if (format.equals("contentOnly")) {
						return ok(Json.toJson(record.getContent()));
					} else {
						if (format.equals("noContent")) {
							record.getContent().clear();
							return ok(Json.toJson(record));
						} else if (record.getContent() != null
								&& record.getContent().containsKey(format)) {
							return ok(record.getContent().get(format)
									.toString());
						} else {
							result.put("error",
									"Resource does not contain representation for format"
											+ format);
							return play.mvc.Results.notFound(result);
						}
					}
				} else
					return ok(Json.toJson(record));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
/*
	public static void filterContextData(WithResource record) {
		List<ContextData> contextAnns = record.getContextData();
		List<ContextData> filteredContextAnns = new ArrayList<ContextData>();
		List<CollectionObject> accessibleCols = DB.getCollectionObjectDAO()
				.getAtLeastCollections(
						AccessManager.effectiveUserDbIds(session().get(
								"effectiveUserIds")), Access.READ, 0,
						Integer.MAX_VALUE);
		List<ObjectId> accessibleColIds = accessibleCols.stream()
				.map(e -> e.getDbId()).collect(Collectors.toList());
		for (ContextData contextAnn : contextAnns) {
			if (accessibleColIds.contains(contextAnn.getTarget()
					.getCollectionId()))
				filteredContextAnns.add(contextAnn);
		}
		record.setContextData(filteredContextAnns);
	}*/

	/**
	 * Edits the WITH resource according to the JSON body. For every field
	 * mentioned in the JSON body it either edits the existing one or it adds it
	 * (in case it doesn't exist)
	 *
	 * @param id
	 * @return the edited resource
	 */
	// TODO check restrictions (unique fields e.t.c)
	// TODO: edit contextData separately ONLY for collections for which the user
	// has access
	public static Result editRecordResource(String id) {
		ObjectNode error = Json.newObject();
		ObjectId recordDbId = new ObjectId(id);
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			} else {
				Result response = errorIfNoAccessToRecord(Action.EDIT,
						new ObjectId(id));
				if (!response.toString().equals(ok().toString()))
					return response;
				else {
					// TODO Check the JSON
					DB.getRecordResourceDAO().editRecord("", recordDbId, json);
					return ok("Record edited.");
				}
			}
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	public static Result editContextData() throws Exception {
		ObjectNode error = Json.newObject();
		ObjectNode json = (ObjectNode) request().body().asJson();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest(error);
		} else {
			String contextDataType = null;
			if (json.has("contextDataType")) {
				contextDataType = json.get("contextDataType").asText();
				ContextDataType dataType;
				if (contextDataType != null
						& (dataType = ContextDataType.valueOf(contextDataType)) != null) {
					Class clazz;
					try {
						int position = ((ObjectNode) json.get("target")).remove("position").asInt();
						if (dataType.equals(ContextDataType.ExhibitionData)) {
							clazz = Class.forName("model.annotations."
									+ contextDataType);
							ContextData newContextData = (ContextData) Json
									.fromJson(json, clazz);
							ObjectId collectionId = newContextData.getTarget()
									.getCollectionId();
							// int position =
							// newContextData.getTarget().getPosition();
							if (collectionId != null
									&& DB.getCollectionObjectDAO()
											.existsEntity(collectionId)) {
								// filterContextData(record);
								Result response = errorIfNoAccessToCollection(
										Action.EDIT, collectionId);
								if (!response.toString()
										.equals(ok().toString()))
									return response;
								else {
									DB.getCollectionObjectDAO()
											.updateContextData(newContextData,
													position);

								}
							}
						}
						return ok("Edited context data.");
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return internalServerError(error);
					}
				} else
					return badRequest("Context data type should be one of supported types: ExhibitionData.");
			} else
				return badRequest("The contextDataType should be defined.");
		}
	}

	// TODO: Are we checking rights for every record alone?
	public static Result list(String collectionId, int start, int count) {
		List<RecordResource> records = DB.getRecordResourceDAO()
				.getByCollectionBetweenPositions(new ObjectId(collectionId),
						start, start + count);
		return ok(Json.toJson(records));
	}

	// TODO: Remove favorites

	/**
	 * @return
	 */
	public static Result getFavorites() {
		ObjectNode result = Json.newObject();
		if (session().get("user") == null) {
			return forbidden();
		}
		ObjectId userId = new ObjectId(session().get("user"));
		CollectionObject favorite;
		ObjectId favoritesId;
		if ((favorite = DB.getCollectionObjectDAO().getByOwnerAndLabel(userId,
				null, "_favorites")) == null) {
			favoritesId = CollectionObjectController.createFavorites(userId);
		} else {
			favoritesId = favorite.getDbId();
		}
		List<RecordResource> records = DB.getRecordResourceDAO()
				.getByCollection(favoritesId);
		if (records == null) {
			result.put("error", "Cannot retrieve records from database");
			return internalServerError(result);
		}
		ArrayNode recordsList = Json.newObject().arrayNode();
		for (RecordResource record : records) {
			recordsList.add(record.getAdministrative().getExternalId());
		}
		return ok(recordsList);
	}

	public static ObjectNode validateRecord(RecordResource record) {
		ObjectNode result = Json.newObject();
		Set<ConstraintViolation<RecordResource>> violations = Validation
				.getValidator().validate(record);
		if (!violations.isEmpty()) {
			ArrayNode properties = Json.newObject().arrayNode();
			for (ConstraintViolation<RecordResource> cv : violations) {
				properties.add(Json.parse("{\"" + cv.getPropertyPath()
						+ "\":\"" + cv.getMessage() + "\"}"));
			}
			result.put("error", properties);
			return result;
		} else {
			return null;
		}
	}
}
