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


package sources.formatreaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import sources.BritishLibrarySpaceSource;
import sources.DPLASpaceSource;
import sources.core.JsonContextRecordFormatReader;
import sources.utils.JsonContextRecord;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.MediaObject;
import model.Provider.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;

public class BritishLibraryRecordFormatter extends JsonContextRecordFormatReader<CulturalObject> {
	
	public BritishLibraryRecordFormatter() {
		object = new CulturalObject();
	}
	
	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = new CulturalObjectData();
		object.setDescriptiveData(model);
		model.setLabel(rec.getLiteralValue("title"));
		model.setDescription(rec.getLiteralValue("description._content"));
//		model.setIsShownBy(rec.getStringValue("edmIsShownBy"));
//		model.setIsShownAt(rec.getStringValue("edmIsShownAt"));
		model.setMetadataRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType("http://www.europeana.eu/schemas/edm/ProvidedCHO");
//		model.setYear(Integer.parseInt(rec.getStringValue("year")));
		model.setDccreator(Arrays.asList(new LiteralOrResource(rec.getStringValue("principalOrFirstMaker"))));
		
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider")));
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("provider.name"),null,rec.getStringValue("provider.@id")));
		String id = rec.getStringValue("id");
		object.addToProvenance(new ProvenanceInfo(Sources.BritishLibrary.toString(),
				  "https://www.flickr.com/photos/britishlibrary/" + id + "/", id));
		EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
		//TODO: add both thumbnail embMedia and full size embedded media!
		medThumb.setUrl(rec.getStringValue("url_s"));
		object.addMedia(MediaVersion.Thumbnail, medThumb);
		EmbeddedMediaObject med = new EmbeddedMediaObject();
		//TODO: add rights
		med.setHeight(Integer.parseInt(rec.getStringValue("height_s")));
		med.setWidth(Integer.parseInt(rec.getStringValue("width_s")));
		object.addMedia(MediaVersion.Original, med);
//		med.setUrl(rec.getStringValue("edmIsShownBy"));
		return object;
		
//		object.setContributors(rec.getStringArrayValue("sourceResource.contributor"));
//		object.setYears(StringUtils.getYears(rec.getStringArrayValue("datetaken")));
//		// TODO: add rights
//		// object.setItemRights(rec.getStringValue("rights"));
//		object.setExternalId(object.getIsShownAt());
	}

}
