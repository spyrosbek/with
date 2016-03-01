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


package controllers.thesaurus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import model.basicDataTypes.Language;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Result;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.CollectionObjectController;
import controllers.WithResourceController;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

public class CollectionIndexController extends WithResourceController	{

	public static final ALogger log = Logger.of(CollectionObjectController.class);

	private static String[] fields = new String[] { "keywords.uri", "dctype.uri" };
	
	public static Result getCollectionIndex(String id) {
		ObjectNode result = Json.newObject();
		
		try {
			JsonNode json = request().body().asJson();

			ElasticSearcher es = new ElasticSearcher();
			
//			MatchQueryBuilder query = QueryBuilders.matchQuery("collectedIn.collectionId", id);
			QueryBuilder query = CollectionObjectController.getIndexCollectionQuery(new ObjectId(id), json);

			SearchResponse res = es.execute(query, new SearchOptions(0, Integer.MAX_VALUE), fields);
			SearchHits sh = res.getHits();

			List<String[]> list = new ArrayList<>();

			for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
				SearchHit hit = iter.next();

				List<Object> olist = new ArrayList<>();
				
				for (String field : fields) {
					SearchHitField shf = hit.field(field);
				
					if (shf != null) {
						olist.addAll(shf.getValues());
					}				
				}				
				
				if (olist.size() > 0 ) {
					list.add(olist.toArray(new String[] {}));
				}
			}
			
			Set<String> selected = new HashSet<>();
			if (json != null) {
				for (Iterator<JsonNode> iter = json.get("terms").elements(); iter.hasNext();) {
					selected.add(iter.next().get("top").asText());
				}
			}
			
			ThesaurusFacet tf = new ThesaurusFacet();
			tf.create(list, selected);
			
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.READ, collectionDbId);
			
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				return ok(tf.toJSON(Language.EN));
			}
		} catch (Exception e) {
//			e.printStackTrace();
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
}
