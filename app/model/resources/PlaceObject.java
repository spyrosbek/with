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

import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.DescriptiveData;

public class PlaceObject extends RecordResource<PlaceObject.PlaceData> {

	public static class PlaceData extends DescriptiveData {
		public PlaceData(Literal label) {
			super(label);
		}

		// city, archeological site, area, nature reserve, historical site
		ArrayList<LiteralOrResource> nation;
		ArrayList<LiteralOrResource> continent;
		ArrayList<LiteralOrResource> partOfPlace;
		
		Double wgsposlat, wgsposlong, wgsposalt;
		
		// in meters how in accurate the position is
		// also describes the extend of the position
		Double accuracy;
	}
}