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


package general.controllerTest;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;
import general.TestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import model.Collection;
import model.CollectionRecord;
import model.User;

import org.junit.Assert;
import org.junit.Test;

import play.mvc.Result;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import db.DB;

public class TestRightsController {



	@Test
	public void testSetRights() {
		User receiver = new User();
		receiver.setEmail("receive@controller.gr");
		receiver.setUsername(TestUtils.randomString());
		DB.getUserDAO().makePermanent(receiver);

		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUsername(TestUtils.randomString());
		DB.getUserDAO().makePermanent(user);

		Collection col = new Collection();
		col.setDescription("Collection from Controller");
		col.setTitle("Test_1 collection from Controller");
		col.setCategory("Dance");
		col.setCreated(new Date());
		col.setLastModified(new Date());
		col.setPublic(false);
		col.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col);

		CollectionRecord record = new CollectionRecord();
		record.setCollectionId(col.getDbId());
		record.setTitle("This is stored to test the controller");
		record.setDescription("Desc to test controller");
		record.setCreated(new Date());
		record.setSource("Youtube");
		record.setSourceId("123456");
		record.setSourceUrl("http://www.youtube.com/");
		record.setType("Music");
		record.setRights("all");

		try {
			record.getContent().put("XML-EDM",
					new String(Files.readAllBytes(Paths.get("sample-euscreenxml-core.xml"))));
			record.getContent().put("XML-ITEM/CLIP",
					new String(Files.readAllBytes(Paths.get("sample-euscreenxl-item_clip.xml"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		DB.getCollectionRecordDAO().makePermanent(record);

		//test set rights
		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("POST", "/rights"
						+ "/"+record.getDbId()
						+ "/READ"
						+ "?receiver="+receiver.getUsername()
						+ "&owner="+user.getUsername()
						+ "&collection="+col.getTitle()));

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));

			    if(status(result) == 200)
				    assertThat(status(result)).isEqualTo(OK);
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});

		//test list rights
		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("GET", "/rights"
						+ "/list"
						+ "?ownerId="+user.getDbId()));

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));

			    if(status(result) == 200)
				    assertThat(status(result)).isEqualTo(OK);
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});


	}
}
