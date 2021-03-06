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


package model.resources;

import java.util.ArrayList;
import java.util.Collection;

import org.mongodb.morphia.annotations.Entity;

import model.DescriptiveData;
import model.basicDataTypes.MultiLiteralOrResource;
import model.resources.RecordResource.RecordDescriptiveData;

@Entity("RecordResource")
public class PlaceObject extends RecordResource<PlaceObject.PlaceData> {

	public PlaceObject() {
		super();
		this.resourceType = WithResourceType.valueOf(this.getClass().getSimpleName());
	}

	public static class PlaceData extends RecordDescriptiveData {

		// city, archeological site, area, nature reserve, historical site
		MultiLiteralOrResource nation;
		MultiLiteralOrResource continent;
		MultiLiteralOrResource partOfPlace;
		
		Double wgsposlat, wgsposlong, wgsposalt;
		
		// in meters how in accurate the position is
		// also describes the extend of the position
		Double accuracy;

		public MultiLiteralOrResource getNation() {
			return nation;
		}
		
		public MultiLiteralOrResource getContinent() {
			return continent;
		}
		
		public MultiLiteralOrResource getPartOfPlace() {
			return partOfPlace;
		}
		
		public void setNation(MultiLiteralOrResource nation) {
			this.nation = nation;
		}

		public void setContinent(MultiLiteralOrResource continent) {
			this.continent = continent;
		}
		
		public void setPartOfPlace(MultiLiteralOrResource partOfPlace) {
			this.partOfPlace = partOfPlace;
		}

		public Collection<String> collectURIs() {
			Collection<String> res = super.collectURIs();

			if (nation != null && nation.getURI() != null) {
				res.addAll(nation.getURI());
			}
			
			if (continent != null && continent.getURI() != null) {
				res.addAll(continent.getURI());
			}
			
			if (partOfPlace != null && partOfPlace.getURI() != null) {
				res.addAll(partOfPlace.getURI());
			}
			
			return res;
		} 

	}
}