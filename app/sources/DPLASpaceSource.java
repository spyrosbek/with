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

import com.fasterxml.jackson.databind.JsonNode;

import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.ExternalBasicRecord;
import model.ExternalBasicRecord.ItemRights;
import model.ExternalBasicRecord.RecordType;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.WithResource;
import play.libs.Json;
import sources.core.CommonFilterLogic;
import sources.core.CommonFilters;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.RecordJSONMetadata;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.RecordJSONMetadata.Format;
import sources.core.Utils.Pair;
import sources.formatreaders.DPLARecordFormatter;
import sources.formatreaders.EuropeanaItemRecordFormatter;
import utils.ListUtils;

public class DPLASpaceSource extends ISpaceSource {

	public String getHttpQuery(CommonQuery q) {
		// q=zeus&api_key=SECRET_KEY&sourceResource.creator=Zeus
		QueryBuilder builder = new QueryBuilder("http://api.dp.la/v2/items");
		builder.addSearchParam("api_key", apiKey);
		builder.addQuery("q", q.searchTerm);
		builder.addSearchParam("page", q.page);
		builder.addSearchParam("page_size", q.pageSize);
		builder.addSearchParam("facets",
				"provider.name,sourceResource.type,sourceResource.contributor,sourceResource.spatial.country,dataProvider");
		return addfilters(q, builder).getHttp();
	}

	public DPLASpaceSource() {
		super();
		LABEL = Sources.DPLA.toString();
		apiKey = "SECRET_KEY";
		addDefaultWriter(CommonFilters.TYPE.getId(), fwriter("sourceResource.type"));
		addDefaultWriter(CommonFilters.COUNTRY.getId(), fwriter("sourceResource.spatial.country"));
		addDefaultWriter(CommonFilters.CREATOR.getId(), fwriter("sourceResource.creator"));
		addDefaultWriter(CommonFilters.CONTRIBUTOR.getId(), fwriter("sourceResource.contributor"));
		addDefaultWriter(CommonFilters.PROVIDER.getId(), fwriter("provider.name"));
		addDefaultWriter(CommonFilters.TYPE.getId(), fwriter("sourceResource.type"));
		addDefaultComplexWriter(CommonFilters.YEAR.getId(), qfwriterYEAR());

		/**
		 * TODO check this
		 */

		addDefaultWriter(CommonFilters.RIGHTS.getId(), fwriter("sourceResource.rights"));
		addMapping(CommonFilters.RIGHTS.getId(), WithMediaRights.Commercial, ".*creative(?!.*nc).*");
		// ok RIGHTS:*creative* AND NOT RIGHTS:*nd*
		addMapping(CommonFilters.RIGHTS.getId(), WithMediaRights.Modify, ".*creative(?!.*nd).*");

		addMapping(CommonFilters.RIGHTS.getId(), WithMediaRights.Creative_Not_Commercial, ".*creative.*nc.*",
				".*non-commercial.*");

		addMapping(CommonFilters.RIGHTS.getId(), WithMediaRights.RRPA, ".*rr-p.*");
		addMapping(CommonFilters.RIGHTS.getId(), WithMediaRights.RRRA, ".*rr-r.*");
		addMapping(CommonFilters.RIGHTS.getId(), WithMediaRights.RRFA, ".*rr-f.*");

		addMapping(CommonFilters.RIGHTS.getId(), WithMediaRights.RRFA, ".*unknown.*");

		addMapping(CommonFilters.RIGHTS.getId(), WithMediaRights.Creative_Not_Modify, ".*creative.*nd.*");

		addMapping(CommonFilters.RIGHTS.getId(), WithMediaRights.Creative, ".*(creative).*");

		addMapping(CommonFilters.TYPE.getId(), WithMediaType.IMAGE, "image");
		addMapping(CommonFilters.TYPE.getId(), WithMediaType.VIDEO, "moving image");
		addMapping(CommonFilters.TYPE.getId(), WithMediaType.AUDIO, "sound");
		addMapping(CommonFilters.TYPE.getId(), WithMediaType.TEXT, "text");

		formatreader = new DPLARecordFormatter(vmap);

		// TODO: what to do with physical objects?
	}

	private Function<List<String>, List<Pair<String>>> qfwriterYEAR() {
		Function<String, String> function = (String s) -> {
			return "%22" + Utils.spacesFormatQuery(s, "%20") + "%22";
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
		Function<String, String> function = (String s) -> {
			return "%22" + Utils.spacesFormatQuery(s, "%20") + "%22";
		};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>(parameter, Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		CommonFilterLogic type = new CommonFilterLogic(CommonFilters.TYPE);
		CommonFilterLogic provider = new CommonFilterLogic(CommonFilters.PROVIDER);
		CommonFilterLogic dataProvider = new CommonFilterLogic(CommonFilters.DATA_PROVIDER);
		CommonFilterLogic creator = new CommonFilterLogic(CommonFilters.CREATOR);
		CommonFilterLogic country = new CommonFilterLogic(CommonFilters.COUNTRY);
		CommonFilterLogic contributor = new CommonFilterLogic(CommonFilters.CONTRIBUTOR);
		if (checkFilters(q)) {
			try {
				response = HttpConnector.getURLContent(httpQuery);
				// System.out.println(response.toString());
				JsonNode docs = response.path("docs");
				res.totalCount = Utils.readIntAttr(response, "count", true);
				res.count = docs.size();
				res.startIndex = Utils.readIntAttr(response, "start", true);

				for (JsonNode item : docs) {

					// String t = Utils.readAttr(item.path("sourceResource"),
					// "type", false);
					// countValue(type, t);

					res.addItem(formatreader.readObjectFrom(item));
					// countValue(creator, obj.getCreator());

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

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return res;
	}

	private void readList(JsonNode json, CommonFilterLogic filter) {
		// System.out.println(json);
		for (JsonNode f : json.path("terms")) {
			String label = f.path("term").asText();
			int count = f.path("count").asInt();
			countValue(filter, label, count);
		}
	}

	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector.getURLContent("http://api.dp.la/v2/items?id=" + recordId + "&api_key=" + apiKey);
			JsonNode record = response.get("docs").get(0);
			if (record != null){
				jsonMetadata.add(new RecordJSONMetadata(Format.JSONLD_DPLA, record.toString()));
				String json = Json.toJson(formatreader.readObjectFrom(record)).toString();
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
			}
			return jsonMetadata;
		} catch (Exception e) {
			e.printStackTrace();
			return jsonMetadata;
		}
	}

}
