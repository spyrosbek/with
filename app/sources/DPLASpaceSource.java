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


package sources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.http.conn.ConnectTimeoutException;

import com.fasterxml.jackson.databind.JsonNode;

import model.resources.RecordResource;
import model.resources.WithResource;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import search.FiltersFields;
import search.Sources;
import search.Response.Failure;
import sources.core.CommonFilterLogic;
import sources.core.CommonQuery;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.RecordJSONMetadata;
import sources.core.RecordJSONMetadata.Format;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.Utils.Pair;
import sources.formatreaders.DPLARecordFormatter;
import sources.utils.FunctionsUtils;
import sources.utils.JsonContextRecord;

public class DPLASpaceSource extends ISpaceSource {
	public static final ALogger log = Logger.of( DPLASpaceSource.class);
	
	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder("http://api.dp.la/v2/items");
		builder.addSearchParam("api_key", apiKey);
		builder.setQuery("q", q.searchTerm);
		builder.addSearchParam("page", q.page);
		builder.addSearchParam("page_size", q.pageSize);
		builder.addSearchParam("facets",
				"provider.name,sourceResource.type,sourceResource.contributor,sourceResource.spatial.country,dataProvider");
		return addfilters(q, builder).getHttp();
	}

	public DPLASpaceSource() {
		super(Sources.DPLA);

		addDefaultWriter(FiltersFields.TYPE.getFilterId(), fwriter("sourceResource.type"));
		addDefaultWriter(FiltersFields.COUNTRY.getFilterId(), fwriter("sourceResource.spatial.country"));
		addDefaultWriter(FiltersFields.CREATOR.getFilterId(), fwriter("sourceResource.creator"));
		addDefaultWriter(FiltersFields.CONTRIBUTOR.getFilterId(), fwriter("sourceResource.contributor"));
		addDefaultWriter(FiltersFields.PROVIDER.getFilterId(), fwriter("provider.name"));
//		addDefaultWriter(FiltersFields.TYPE.getFilterId(), fwriter("sourceResource.type"));
		addDefaultComplexWriter(FiltersFields.YEAR.getFilterId(), qfwriterYEAR());
		addDefaultWriter(FiltersFields.RIGHTS.getFilterId(), fwriter("sourceResource.rights"));
		
		
		formatreader = new DPLARecordFormatter();

		// TODO: what to do with physical objects?
	}

	private Function<List<String>, List<Pair<String>>> qfwriterYEAR() {
		Function<String, String> function = (String s) -> {
			return "\"" + s+ "\"";
		};
		return new Function<List<String>, List<Pair<String>>>() {
			@Override
			public List<Pair<String>> apply(List<String> t) {
				String start = "", end = "";
				if (t.size() == 1) {
					start = t.get(0) + "-01-01";
					end = next(t.get(0)) + "-01-01";
				} else if (t.size() > 1) {
					start = t.get(0) + "-01-01";
					end = next(t.get(1)) + "-01-01";
				}

				return Arrays.asList(new Pair<String>("sourceResource.date.after", start),
						new Pair<String>("sourceResource.date.before", end));

			}

			private String next(String string) {
				return "" + (Integer.parseInt(string) + 1);
			}
		};
	}

	private Function<List<String>, Pair<String>> fwriter(String parameter) {
		return FunctionsUtils.toORList(parameter);
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName().toString();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		CommonFilterLogic type = new CommonFilterLogic(FiltersFields.TYPE);
		CommonFilterLogic rights = new CommonFilterLogic(FiltersFields.RIGHTS);
		CommonFilterLogic provider = new CommonFilterLogic(FiltersFields.PROVIDER);
		CommonFilterLogic dataProvider = new CommonFilterLogic(FiltersFields.DATA_PROVIDER);
		CommonFilterLogic creator = new CommonFilterLogic(FiltersFields.CREATOR);
		CommonFilterLogic country = new CommonFilterLogic(FiltersFields.COUNTRY);
		CommonFilterLogic contributor = new CommonFilterLogic(FiltersFields.CONTRIBUTOR);
		if (checkFilters(q)) {
			try {
				response = getHttpConnector().getURLContent(httpQuery);
				JsonNode docs = response.path("docs");
				res.totalCount = Utils.readIntAttr(response, "count", true);
				res.count = docs.size();
				res.startIndex = Utils.readIntAttr(response, "start", true);

				for (JsonNode item : docs) {

					// String t = Utils.readAttr(item.path("sourceResource"),
					// "type", false);
					// countValue(type, t);

					WithResource myitem;
					res.addItem(myitem = formatreader.readObjectFrom(item));
					List<String> stringArrayValue = new JsonContextRecord(item).getStringArrayValue("sourceResource.rights");
					if (Utils.hasInfo(stringArrayValue)) {
						countValue(rights, stringArrayValue);
					} else {
						countValue(rights, "UNKNOWN");
					}
					
					List<String> cr = new JsonContextRecord(item).getStringArrayValue(false,"sourceResource.creator","originalRecord.creator");
					countValue(creator, cr);
				}
				res.facets = response.path("facets");
				res.filtersLogic = new ArrayList<>();

				readList(response.path("facets").path("provider.name"), provider);

				readList(response.path("facets").path("dataProvider"), dataProvider);

				readList(response.path("facets").path("sourceResource.type"), type);

				readList(response.path("facets").path("sourceResource.contributor"), contributor);

				readList(response.path("facets").path("sourceResource.spatial.country"), country);

				res.filtersLogic = new ArrayList<>();
				res.filtersLogic.add(type);
				res.filtersLogic.add(provider);
				res.filtersLogic.add(dataProvider);
				res.filtersLogic.add(creator);
				res.filtersLogic.add(country);
				res.filtersLogic.add(contributor);
//				res.filtersLogic.add(rights);
				res.filtersLogic.addAll(vmap.getRestrictionsAsFilters(q,res.count));

			} catch (ConnectTimeoutException ce) {
				res.error = Failure.TIMEOUT;
				// TODO Auto-generated catch block
				log.error( "", ce );
			} catch (Exception e ) {
				log.error( "", e );				
			}
		}
		return res;
	}

	private void readList(JsonNode json, CommonFilterLogic filter) {
		for (JsonNode f : json.path("terms")) {
			String label = f.path("term").asText();
			int count = f.path("count").asInt();
			countValue(filter, label, count);
		}
	}

	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId, RecordResource fullRecord) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = getHttpConnector().getURLContent("http://api.dp.la/v2/items?id=" + recordId + "&api_key=" + apiKey);
			JsonNode record = response.get("docs").get(0);
			if (record != null) {
				jsonMetadata.add(new RecordJSONMetadata(Format.JSONLD_DPLA, record.toString()));
				String json = Json.toJson(formatreader.overwriteObjectFrom(fullRecord,record)).toString();
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
			}
			return jsonMetadata;
		} catch (Exception e) {
			log.error("",e );
			return jsonMetadata;
		}
	}

	@Override
	public String apiConsole() {
		return "http://api.dp.la/";
	}
}
