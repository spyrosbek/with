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


package espace.core;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.RecordJSONMetadata.Format;
import espace.core.sources.FilterValuesMap;
import espace.core.sources.TypeValues;

public abstract class ISpaceSource {

	public ISpaceSource() {
		super();
		vmap = new FilterValuesMap();
	}

	public abstract String getSourceName();

	public String getHttpQuery(CommonQuery q) {
		return "";
	};

	public abstract SourceResponse getResults(CommonQuery q);

	public String autocompleteQuery(String term, int limit) {
		return "";
	}

	public AutocompleteResponse autocompleteResponse(String response) {
		return new AutocompleteResponse();
	}

	public ArrayList<RecordJSONMetadata> getRecordFromSource(
			String recordId) {
		return new ArrayList<RecordJSONMetadata>();
	}

	private FilterValuesMap vmap;

	protected void countValue(CommonFilterResponse type, String t) {
		type.addValue(vmap.translateToCommon(type.filterID, t));
	}

	protected void addMapping(String filterID, String commonValue,
			String specificValue, String querySegment) {
		vmap.addMap(filterID, commonValue, specificValue, querySegment);
	}

	protected List<String> translateToSpecific(String filterID, String value) {
		return vmap.translateToSpecific(filterID, value);
	}

	protected List<String> translateToCommon(String filterID, String value) {
		return vmap.translateToCommon(filterID, value);
	}

	protected List<String> translateToQuery(String filterID, String value) {
		return vmap.translateToQuery(filterID, value);
	}

	protected String addfilters(CommonQuery q, String qstr) {
		if (q.filters != null) {
			for (CommonFilter filter : q.filters) {
				for (String subq : translateToQuery(filter.filterID,
						filter.value)) {
					qstr += subq;
				}
			}
		}
		return qstr;
	}

}
