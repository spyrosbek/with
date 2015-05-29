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

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.SourceResponse.Lang;

public class Utils {

	public static String toLower(String text) {
		if (text != null) {
			return text.toLowerCase();
		}
		return text;
	}

	public static String spacesFormatQuery(String text, String space) {
		return text.replaceAll("(\\s)+", space);
	}

	public static String spacesPlusFormatQuery(String text) {
		return text.replaceAll("(\\s)+", "+");
	}

	public static CommonQuery parseJson(JsonNode json) throws Exception {
		CommonQuery q1 = Json.fromJson(json, CommonQuery.class);
		return q1;
	}

	public static String readAttr(JsonNode json, String string, boolean force) throws Exception {
		return readAttr(json, string, force, null);
	}

	public static int readIntAttr(JsonNode json, String string, boolean force) throws Exception {
		return readIntAttr(json, string, force, 0);
	}

	public static String readAttr(JsonNode json, String string, boolean force, String def) throws Exception {
		if (json == null)
			return null;
		String res = json.path(string).asText();
		if (res == null) {
			if (force)
				throw new Exception("Missing " + string);
			else
				return null;
		}
		return res;
	}

	public static List<String> readArrayAttr(JsonNode json, String string, boolean force) throws Exception {
		if (json == null)
			return null;
		JsonNode a = json.path(string);
		if (a == null) {
			if (force)
				throw new Exception("Missing " + string);
			else
				return null;
		} else {
			List<String> res = new ArrayList<String>(a.size());
			if (a.isArray()) {
				for (int i = 0; i < a.size(); i++) {
					res.add(a.get(i).textValue());
				}
			} else {
				res.add(a.asText());
			}
			return res;
		}
	}

	public static List<Lang> readLangAttr(JsonNode json, String string, boolean force) throws Exception {
		if (json == null)
			return null;
		JsonNode a = json.path(string);
		if (a == null) {
			if (force)
				throw new Exception("Missing " + string);
			else
				return null;
		} else {
			List<Lang> res = new ArrayList<Lang>(a.size());
			if (a.isArray()) {
				for (int i = 0; i < a.size(); i++) {
					res.add(new Lang(null, a.get(i).textValue()));
				}
			} else {
				res.add(new Lang(null, a.asText()));
			}
			return res;
		}
	}

	public static int readIntAttr(JsonNode json, String string, boolean force, int def) throws Exception {
		if (json == null)
			return def;
		String readAttr = readAttr(json, string, force, "" + def);
		if (readAttr == null || readAttr.equals(""))
			return def;
		// System.out.println("Int? " + readAttr);
		return Integer.parseInt(readAttr);
	}

	public static Pair<String> getAttr(String value, String attrName) {
		if (value != null)
			return new Pair<String>(attrName, value);
		return null;
	}

	public static Pair<String> getFacetsAttr(List<String> values, String NAME) {
		if (values != null) {
			return new Pair<String>(NAME, getORList(values));
		}
		return null;
	}

	public static Pair<String> getFacetsAttr(String value, String NAME) {
		if (value != null) {
			return new Pair<String>(NAME, value);
		}
		return null;
	}

	public static String getORList(List<String> values) {
		String res = "";
		if (values.size() > 1) {
			res += "(" + spacesPlusFormatQuery(values.get(0));
			for (int i = 1; i < values.size(); i++) {
				res += "+OR+" + spacesPlusFormatQuery(values.get(i));
			}
			res += ")";
		} else {
			res += spacesPlusFormatQuery(values.get(0));
		}
		return res;
	}

	public static class Pair<T> {
		public T first;
		public T second;

		public Pair() {
			super();
		}

		public Pair(T first, T second) {
			super();
			this.first = first;
			this.second = second;
		}

		public String getHttp() {
			return first + "=" + spacesFormatQuery(second.toString(),"%20");
		}
		
	}
	
	public static class LongPair<T> extends Pair<T>{
		
		public LongPair() {
			super();
		}

		public LongPair(T first, T second) {
			super(first, second);
		}

		public String getHttp() {
			return first + "=%22" + spacesFormatQuery(second.toString(),"%20")+"%22";
		}
	}

	public static JsonNode findNode(JsonNode path, Pair<String>... pair) {
		boolean found;
		// System.out.println(path);
		for (JsonNode node : path) {
			found = true;
			for (Pair<String> p : pair) {
				String value = node.path(p.first).asText();
				// System.out.println(value + "?=" + p.second);
				if (!p.second.contains(value)) {
					found = false;
					break;
				}
			}
			if (found) {
				// System.out.println("got it");
				return node;
			}
		}
		return null;
	}

	public static boolean hasAny(String term) {
		return term != null && !term.equals("");
	}

}
