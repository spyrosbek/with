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


package model.resources.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.DescriptiveData;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.basicDataTypes.MultiLiteralOrResource;
import model.resources.WithResource;
import model.resources.WithResource.WithAdmin;
import model.resources.WithResource.WithResourceType;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

@Entity("CollectionObject")
public class CollectionObject<T extends CollectionObject.CollectionDescriptiveData> 
	extends WithResource<T, CollectionObject.CollectionAdmin> {

	public CollectionObject() {
		super();
		this.administrative = new CollectionAdmin();
		//this.descriptiveData = new CollectionDescriptiveData();
		this.resourceType = WithResourceType.valueOf(this.getClass()
				.getSimpleName());
		this.collectedResources = new ArrayList<ContextData<ContextDataBody>>();
	}

	@Embedded
	private List<ContextData<ContextDataBody>> collectedResources;


	public List<ContextData<ContextDataBody>> getCollectedResources() {
		return collectedResources;
	}

	public void setCollectedResources(
			List<ContextData<ContextDataBody>> collectedResources) {
		this.collectedResources = collectedResources;
	}

	@Embedded
	public static class CollectionAdmin extends WithResource.WithAdmin {

		//public enum CollectionType {SimpleCollection, Exhibition};

		protected int entryCount = 0;
		//protected CollectionType collectionType = CollectionType.SimpleCollection;

		public int getEntryCount() {
			return entryCount;
		}

		public void setEntryCount(int entryCount) {
			this.entryCount = entryCount;
		}

		public void incEntryCount() {
			this.entryCount++;
		}

		/*public CollectionType getCollectionType() {
			return collectionType;
		}

		public void setCollectionType(CollectionType collectionType) {
			this.collectionType = collectionType;
		}*/

	}

	@Embedded
	public static class CollectionDescriptiveData extends DescriptiveData {
		//TODO: change these to camelCase!
		// start day or possible start days
		private MultiLiteralOrResource dccreator;
		// for whom the resource is intended or useful
		private MultiLiteralOrResource dctermsaudience;
		// additional views of the timespan?
		private MultiLiteralOrResource dclanguage;

		// TODO: add link to external collection
		public MultiLiteralOrResource getDccreator() {
			return dccreator;
		}

		public void setDccreator(MultiLiteralOrResource dccreator) {
			this.dccreator = dccreator;
		}

		public MultiLiteralOrResource getDctermsaudience() {
			return dctermsaudience;
		}

		public void setDctermsaudience(
				MultiLiteralOrResource dctermsaudience) {
			this.dctermsaudience = dctermsaudience;
		}

		public MultiLiteralOrResource getDclanguage() {
			return dclanguage;
		}

		public void setDclanguage(MultiLiteralOrResource dclanguage) {
			this.dclanguage = dclanguage;
		}

	}

	/*
	 * Elastic transformations
	 */

	/*
	 * Currently we are indexing only Resources that represent
	 * collected records
	 */
	public Map<String, Object> transform() {
		Map<String, Object> idx_map =  this.transformWR();
		//idx_map.put("collectionType", this.getAdministrative().getCollectionType());

		idx_map.put("dccreator", this.getDescriptiveData().getDccreator());
		idx_map.put("dctermsaudience", this.getDescriptiveData().getDctermsaudience());
		idx_map.put("dclanguage", this.getDescriptiveData().getDclanguage());

		ArrayNode cd = (ArrayNode) Json.toJson(this.getCollectedResources());
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		List<Object> cd_map = mapper.convertValue(cd, List.class);
		idx_map.put("collectedResources", cd_map);

		return idx_map;
	}

}
