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
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.japi.Option;
import controllers.CollectionObjectController;
import controllers.WithResourceController;
import db.DB;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.annotations.ContextData;
import model.annotations.ExhibitionData;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.Resource;
import model.basicDataTypes.WithDate;
import model.resources.CulturalObject;
import model.resources.WithResourceType;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.collection.Exhibition;
import model.resources.collection.Exhibition.ExhibitionDescriptiveData;
import play.Logger;
import play.core.j.JavaResultExtractor;
import play.libs.F;
import play.libs.Json;
import play.mvc.Result;
import search.Sources;
import sources.core.ApacheHttpConnector;
import sources.core.HttpConnector;
import sources.utils.JsonContextRecord;
import utils.ListUtils;

public class ExhibitionReader {

	static private final Logger.ALogger log = Logger.of(ExhibitionReader.class);

	public static HttpConnector getHttpConnector() {
		return ApacheHttpConnector.getApacheHttpConnector();
	}

	public void importOmeka(ObjectId creatorDbId, int ncollections) {
		JsonNode response;
		try {
			response = getHttpConnector().getURLContent("http://tellyourphotostory.be/espace_test/api/exhibits");
			if (ncollections < 0)
				ncollections = response.size();
			else
				ncollections = Math.min(ncollections, response.size());
			for (int i = 0; i < ncollections; i++) {
				fillExhibitionObjectFrom(new JsonContextRecord(response.get(i)), creatorDbId);
			}
		} catch (Exception e) {
			log.error("Exeption", e);
		}

	}

	protected Exhibition fillExhibitionObjectFrom(JsonContextRecord text, ObjectId creatorDbId) {

		Exhibition exhibition = new Exhibition();
		exhibition.getAdministrative().getAccess().setIsPublic(true);

		ExhibitionDescriptiveData model = new ExhibitionDescriptiveData();
		exhibition.setDescriptiveData(model);
		model.setMetadataRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType(new Resource("http://www.europeana.eu/schemas/edm/ProvidedCHO"));
		model.setLabel(text.getMultiLiteralValue("title"));
		model.setDescription(text.getMultiLiteralValue("description"));
		model.setCredits(text.getStringValue("credits"));
		ProvenanceInfo provInfo = new ProvenanceInfo("OMECA", text.getStringValue("id"), text.getStringValue("url"));
		exhibition.addToProvenance(provInfo);
		exhibition.getAdministrative().setExternalId(text.getStringValue("id"));
		ObjectNode resultInfo = Json.newObject();
		boolean success = CollectionObjectController.internalAddCollection(exhibition, WithResourceType.Exhibition,
				creatorDbId, resultInfo);
		if (success)
			importExhibitionPagesObjectFrom(text, exhibition.getDbId());
		return exhibition;
	}

	protected void importExhibitionPagesObjectFrom(JsonContextRecord text, ObjectId collectionId) {
		String url = text.getStringValue("pages.url");
		try {
			JsonNode response = getHttpConnector().getURLContent(url);
			JsonContextRecord rec = new JsonContextRecord(response);
			Function<JsonNode, JsonContextRecord> function = (x)-> new JsonContextRecord(x);
			List<Object> sequences = new ArrayList<>();
			for (JsonContextRecord page : ListUtils.transform(rec.getValues("[.*]"), function)) {
				List<Object> blocks = new ArrayList<>();
				
				for (JsonContextRecord pageBlock : ListUtils.transform(page.getValues("page_blocks[.*]"), function)) {
					JsonContextRecord texto = buildTextElement(pageBlock.getStringValue("text"));
					List<Object> recs = new ArrayList<>();
					recs.add(texto);
					for (JsonContextRecord item : ListUtils.transform(pageBlock.getValues("attachments[.*]"), function)) {
						CulturalObject parseTheItem = parseTheItem(collectionId, item);
						if (parseTheItem!=null)
						recs.add(buildCHOElement(parseTheItem));
						// TODO check the caption here
					}
					blocks.add(buildTogetherElement(recs));
					// TODO add a together
				}
			sequences.add(buildSequenceElement(blocks));
				// TODO add the group
			}
			System.out.println(Json.toJson(buildSequenceElement(sequences)));
		} catch (Exception e) {
			log.error("Exeption", e);
		}
	}

	private JsonContextRecord buildTextElement(String stringValue) {
		JsonContextRecord r = new JsonContextRecord("{}");
		r.setValue("type", "Text");
		r.setValue("id", "Text1");
		r.setValue("en", stringValue);
		return r;
	}
	
	private JsonContextRecord buildSequenceElement(Collection<?> records) {
		JsonContextRecord r = new JsonContextRecord("{}");
		r.setValue("type", "Sequence");
		r.setValue("id", "Seq1");
		ObjectMapper mapper = new ObjectMapper(); 
		JsonNode node = mapper.convertValue(records, JsonNode.class);
		r.setValue("groups", node);
		return r;
	}
	
	private JsonContextRecord buildTogetherElement(Collection<?> records) {
		JsonContextRecord r = new JsonContextRecord("{}");
		r.setValue("type", "Together");
		r.setValue("id", "Together1");
		ObjectMapper mapper = new ObjectMapper(); 
		JsonNode node = mapper.convertValue(records, JsonNode.class);
		r.setValue("groups", node);
		return r;
	}
	
	private JsonContextRecord buildCHOElement(CulturalObject o) {
		JsonContextRecord r = new JsonContextRecord("{}");
		r.setValue("type", "Cho");
		r.setValue("id", "Cho1");
		r.setValue("url", "/record/"+o.getDbId().toString());
		return r;
	}

	private CulturalObject parseTheItem(ObjectId collectionId, JsonContextRecord itemJsonContextRecord) {
		JsonNode response1;
		try {
			response1 = getHttpConnector().getURLContent(itemJsonContextRecord.getStringValue("item.url"));
			JsonContextRecord rec1 = new JsonContextRecord(response1);
			String source = rec1.getStringValue("item_type.name");
			String id = rec1.getStringValue("element_texts[element.name=Identifier].text");
			System.out.println(source + " id=" + id);
			CulturalObject record = new CulturalObject();
			CulturalObjectData descData = new CulturalObjectData();
			record.setDescriptiveData(descData);
			if (source==null || id==null){
				source = Sources.UploadedByUser.getID();
				id = rec1.getStringValue("id");
				record.addToProvenance(new ProvenanceInfo(source, null, id));
				
				descData.setLabel(rec1.getMultiLiteralValue("element_texts[element.name=Title].text"));
				descData.setDescription(rec1.getMultiLiteralValue("element_texts[element.name=Description].text"));
				descData.setDccreator(rec1.getMultiLiteralOrResourceValue("element_texts[element.name=Creator].text"));
				descData.setDates(rec1.getWithDateArrayValue("element_texts[element.name=Date].text"));
				
				String rights = rec1.getStringValue("element_texts[element.name=Rights].text");
				String type = rec1.getStringValue("element_texts[element.name=Type].text");
				
				response1 = getHttpConnector().getURLContent(itemJsonContextRecord.getStringValue("file.url"));
				JsonContextRecord rec2 = new JsonContextRecord(response1);
				
				String original = rec2.getStringValue("file_urls.original");
				String thumbnail = rec2.getStringValue("file_urls.thumbnail");
				
				EmbeddedMediaObject media = new EmbeddedMediaObject();
				LiteralOrResource originalRights = rights!=null?new LiteralOrResource(rights):null;
				media.setOriginalRights(originalRights);
				media.setUrl(original);
//						media.setType(type);
				record.addMedia(MediaVersion.Original, media );
				
				media = new EmbeddedMediaObject();
				media.setOriginalRights(originalRights);
				media.setUrl(thumbnail);
//						media.setType(type);
				record.addMedia(MediaVersion.Thumbnail, media );
				
				
			} else {
				descData.setLabel(new MultiLiteral(id).fillDEF());
				record.addToProvenance(new ProvenanceInfo(source, null, id));
			}
			play.libs.F.Option<Integer> p = play.libs.F.Option.None();
			Result result = WithResourceController.addRecordToCollection(Json.toJson(record), collectionId, p, false);
//			record.setDbId(new ObjectId(r.));
			byte[] body = JavaResultExtractor.getBody(result, 30000L);
//			Having a byte array, you can extract charset from Content-Type header and create java.lang.String:
			String header = JavaResultExtractor.getHeaders(result).get("Content-Type");
			String charset = "utf-8";
			if(header != null && header.contains("; charset=")){
			    charset = header.substring(header.indexOf("; charset=") + 10, header.length()).trim();
			}
			String bodyStr = new String(body, charset);
			JsonContextRecord rr = new JsonContextRecord(bodyStr);
			record.setDbId(new ObjectId(rr.getStringValue("recordId")));
			return record;
		} catch (Exception e) {
			log.error("Exeption", e);
		}
		return null;
	}

}
