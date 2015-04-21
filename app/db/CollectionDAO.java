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


package db;

import java.util.List;

import model.Collection;
import model.CollectionMetadata;
import model.User;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import play.Logger;
import play.Logger.ALogger;

public class CollectionDAO extends DAO<Collection> {
	public static final ALogger log = Logger.of(CollectionDAO.class);

	public CollectionDAO() {
		super( Collection.class );
	}

	public List<Collection> getCollectionsByIds(List<ObjectId> ids) {
		Query<Collection> colQuery = this.createQuery()
				.field("_id").hasAnyOf(ids);
		return find(colQuery).asList();
	}

	public Collection getByTitle(String title) {
		return this.findOne("title", title);
	}

	public Collection getById(ObjectId id) {
		Query<Collection> q = this.createQuery()
				.field("_id").equal(id);
		return findOne(q);
	}

	public List<Collection> getByOwner(ObjectId id) {
		return getByOwner(id, 0, 0);
	}

	public List<Collection> getByOwner(ObjectId ownerId, int offset, int count) {
		Query<Collection> q = this.createQuery()
				.field("ownerId").equal(ownerId)
				.offset(offset)
				.limit(count);
		return this.find(q).asList();
	}


	public User getCollectionOwner(ObjectId id) {
		Query<Collection> q =  this.createQuery()
				.field("_id").equal(id)
				.retrievedFields(true, "ownerId");
		return findOne(q).retrieveOwner();
	}


	public int removeById(ObjectId id) {
		User owner = getCollectionOwner(id);
		for(CollectionMetadata colMeta: owner.getCollectionMetadata()) {
			if(colMeta.getCollectionId().equals(id))
				owner.getCollectionMetadata().remove(colMeta);
			DB.getUserDAO().makePermanent(owner);
		}

		Query<Collection> q = this.createQuery()
				.field("_id").equal(id);
		return deleteByQuery(q).getN();
	}
}
