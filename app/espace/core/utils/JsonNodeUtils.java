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


package espace.core.utils;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonNodeUtils {
	
	public static String asString(JsonNode node) {
		if (node!=null && !node.isMissingNode()){
			if (node.isArray()){
				return node.get(0).asText();
			} else
				return node.asText();
		}
		return null;
	}
	public static List<String> asStringArray(JsonNode node) {
		if (node!=null && !node.isMissingNode()){
			ArrayList<String> res = new ArrayList<>();
			if (node.isArray()){
				for (int i = 0; i < node.size(); i++) {
					res.add(node.get(i).asText());
				}
			} else{
				res.add(node.asText());
			}
		}
		return null;
	}

}
