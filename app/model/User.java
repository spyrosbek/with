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


package model;

import java.util.List;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

@Entity
@Indexes( @Index("name, -searchHistory.searchDate"))
public class User {

	@Id
	private String dbID;
	@Indexed(name="name", unique=true)
	private String name;
	private String email;
	private String firstName;
	private String lastName;
	private String md5Password;

	@Embedded
	private FacebookAccount fbAccount;
	@Embedded
	private List<Search> searchHistory;


	public String getDbID() {
		return dbID;
	}

	public void setDbID(String dbID) {
		this.dbID = dbID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMd5Password() {
		return md5Password;
	}

	public void setMd5Password(String md5Password) {
		this.md5Password = md5Password;
	}

	public FacebookAccount getFbAccount() {
		return fbAccount;
	}

	public void setFbAccount(FacebookAccount fbAccount) {
		this.fbAccount = fbAccount;
	}

	public List<Search> getSearcHistory() {
		return searchHistory;
	}

	public void setSearcHistory(List<Search> searcHistory) {
		this.searchHistory = searcHistory;
	}

}
