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


package espace.core.sources.formatreaders;

import org.apache.commons.codec.digest.DigestUtils;

import espace.core.JsonContextRecordFormatReader;
import espace.core.sources.DPLASpaceSource;
import espace.core.utils.JsonContextRecord;
import model.ExternalBasicRecord;
import model.Provider;

public class DPLAExternalBasicRecordFormatter extends JsonContextRecordFormatReader<ExternalBasicRecord> {

	@Override
	public ExternalBasicRecord buildObjectFrom() {
		return new ExternalBasicRecord();
	}

	@Override
	public ExternalBasicRecord fillObjectFrom(JsonContextRecord rec, ExternalBasicRecord record) {
		record.addProvider(new Provider(rec.getStringValue("dataProvider")));
		record.addProvider(new Provider(rec.getStringValue("provider.name"),rec.getStringValue("provider.@id"),rec.getStringValue("provider.@id")));
		//TODO: add type
		//TODO: add null checks
		record.setThumbnailUrl(rec.getStringValue("objtect"));
		record.setIsShownBy(null);
		record.setTitle(rec.getStringValue("sourceResource.title"));
		record.setDescription(rec.getStringValue("sourceResource.description"));
		record.setCreator(rec.getStringValue("sourceResource.creator"));
		record.setContributors(rec.getStringArrayValue("sourceResource.contributor"));
		// TODO: add years
//		record.setYear(ListUtils.transform(rec.getStringArrayValue("year"), (String y)->{return Year.parse(y);}));
		record.setIsShownAt(rec.getStringValue("isShownAt"));
		// TODO: add rights
//		record.setItemRights(rec.getStringValue("sourceResource.rights"));
		record.setExternalId(record.getIsShownAt());
		if (record.getExternalId() == null || record.getExternalId() == "")
			record.setExternalId(record.getIsShownBy());
		record.setExternalId(DigestUtils.md5Hex(record.getExternalId()));
		String id = rec.getStringValue("id");
		Provider recordProvider = new Provider(DPLASpaceSource.LABEL, id, 
				"http://dp.la/item/"+id);
		record.addProvider(recordProvider);
		
		return record;
	}

}
