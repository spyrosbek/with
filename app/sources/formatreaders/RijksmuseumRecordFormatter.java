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

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import play.Logger;
import sources.FilterValuesMap;
import sources.core.Utils;
import sources.utils.JsonContextRecord;

public class RijksmuseumRecordFormatter extends CulturalRecordFormatter {

	public RijksmuseumRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		Language[] language = null;
		language = getLanguagesFromText(rec.getStringValue("title"), 
				rec.getStringValue("longTitle"));
		rec.setLanguages(language);
		
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		model.setLabel(rec.getMultiLiteralValue("title"));
		model.setDescription(rec.getMultiLiteralValue("longTitle"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("edmIsShownBy"));
		model.setIsShownAt(rec.getLiteralOrResourceValue("edmIsShownAt"));
		// model.setYear(Integer.parseInt(rec.getStringValue("year")));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("principalOrFirstMaker"));

		String id = rec.getStringValue("objectNumber");
		object.addToProvenance(new ProvenanceInfo(Sources.Rijksmuseum.toString(), 
				"https://www.rijksmuseum.nl/en/search/objecten?q=dance&p=1&ps=12&ii=0#/" + id + ",0", id));
		
		
		
		String uri3 = rec.getStringValue("headerImage.url");
		String uri2 = rec.getStringValue("webImage.url");
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setWidth(rec.getIntValue("headerImage.width"));
			medThumb.setHeight(rec.getIntValue("headerImage.height"));
//			medThumb.setType(type);
//			if (Utils.hasInfo(rights))
//			medThumb.setOriginalRights(new LiteralOrResource(rights.get(0)));
//			medThumb.setWithRights(withRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		
		if (Utils.hasInfo(uri2)){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setUrl(uri2);
			med.setWidth(rec.getIntValue("webImage.width"));
			med.setHeight(rec.getIntValue("webImage.height"));
//			if (Utils.hasInfo(rights))
//			med.setOriginalRights(new LiteralOrResource(rights.get(0)));
//			med.setWithRights(withRights);
//			med.setType(type);
			object.addMedia(MediaVersion.Original, med);
		}
		
		return object;
	}

}
