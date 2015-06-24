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


package elastic;

import org.elasticsearch.ElasticsearchException;

import model.Collection;
import model.CollectionRecord;
import play.Logger;

public class ElasticEraser {
	static private final Logger.ALogger log = Logger.of(ElasticUpdater.class);

	private Collection collection;
	private CollectionRecord record;



	public ElasticEraser(Collection c) {
		this.collection = c;
	}

	public ElasticEraser(CollectionRecord r) {
		this.record = r;
	}


	public void deleteCollection() {
		try {
			Elastic.getTransportClient().prepareDelete(
					Elastic.index,
					Elastic.type_collection,
					collection.getDbId().toString())
				.setOperationThreaded(false)
				.execute()
				.actionGet();
		} catch(ElasticsearchException e) {
			log.error("Cannot delete the specified collection document", e);
		}
	}

	public void deleteRecord() {
		try {
			Elastic.getTransportClient().prepareDelete(
					Elastic.index,
					Elastic.type_within,
					record.getDbId().toString())
				.setOperationThreaded(false)
				.execute()
				.actionGet();
		} catch(ElasticsearchException e) {
			log.error("Cannot delete the specified record document", e);
		}
	}

	public void deleteMergedRecord() {
		try {
			Elastic.getTransportClient().prepareDelete(
					Elastic.index,
					Elastic.type_general,
					record.getExternalId())
				.setOperationThreaded(false)
				.execute()
				.actionGet();
		} catch(ElasticsearchException e) {
			log.error("Cannot delete the specified merged record document", e);
		}
	}
}
