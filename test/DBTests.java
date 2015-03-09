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


import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import model.Collection;
import model.Media;
import model.Record;
import model.RecordLink;
import model.Search;
import model.User;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import play.twirl.api.Content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mongodb.MongoException;

import db.DB;

/**
 *
 * Simple (JUnit) tests that can call all parts of a play app. If you are
 * interested in mocking a whole application, see the wiki for more details.
 *
 */
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DBTests {


	@Test
	public void userStorage() {
		/* Add 1000 random users */
		//int i = 42;
		for (int i = 0; i < 1000; i++) {
			User testUser = new User();
			String email;
			if (i == 42) {
				// email
				email = "heres42_@mongo.gr";
				testUser.setEmail(email);
				testUser.setFirstName(randomString());
			} else {
				// email
				email = randomString() + "@mongo.gr";
				testUser.setEmail(email);
				testUser.setFirstName(randomString());
			}
			// set an MD5 password
			if (i == 42) {
				digest.update("helloworld".getBytes());
				testUser.setMd5Password(digest.digest().toString());
			} else {
				digest.update(randomString().getBytes());
				testUser.setMd5Password(digest.digest().toString());
			}
			if (testUser != null)
				try {
					DB.getUserDAO().makePermanent(testUser);
				} catch (MongoException e) {
					System.out.println("mongo exception");
				}
			// search history
			List<Search> searchHistory = new ArrayList<Search>();
			User userWithDates = DB.getUserDAO().getByEmail(email);
			for (int j = 0; j < 1000; j++) {
				Search s1 = new Search();
				s1.setSearchDate(generate_random_date_java());
				s1.setUser(userWithDates);
				searchHistory.add(s1);
				userWithDates.setSearchHistory(searchHistory);
			}
			if (userWithDates != null)
				try {
					DB.getUserDAO().makePermanent(userWithDates);
				} catch (MongoException e) {
					System.out.println("mongo exception");
				}
		}

		List<User> l = DB.getUserDAO().find().asList();
		assertThat(l.size()).isGreaterThanOrEqualTo(1);

		// int count = DB.getUserDAO().removeAll("obj.name='Tester'" );
		// assertThat( count )
		// .overridingErrorMessage("Not removed enough Testers")
		// .isGreaterThanOrEqualTo(1 );
	}

	@Test
	public void test_Record_and_Media_storage() throws IOException, URISyntaxException {

		//Create a Media Object
		/*for(int i = 0; i < 50; i++) {
			Media image = new Media();

			URL url = new URL("http://clips.vorwaerts-gmbh.de/VfE_html5.mp4");
			File file = new File("test_java.txt");
			FileUtils.copyURLToFile(url, file);
			FileInputStream fileStream = new FileInputStream(
					file);

			byte[] rawbytes = IOUtils.toByteArray(fileStream);

			image.setData(rawbytes);
			image.setType("video/mp4");
			image.setMimeType("mp4");
			image.setDuration(0.0f);
			image.setHeight(1024);
			image.setWidth(1080);

			DB.getMediaDAO().makePermanent(image);

			//Create Record Object
			Record record = new Record();
			DB.getRecordDAO().save(record);

			//Create a RecordLink Object
			//and references to Media and Record
*/
			//Get Media object
			Media imageRetrieved = DB.getMediaDAO().findById(new ObjectId("54ef0a09e4b0af9ca4dc8fbc"));
			//Media imageRetrieved = DB.getMediaDAO().
			//Get Record object
			Record recordRetrieved = DB.getRecordDAO().find().get();

			RecordLink rlink = new RecordLink();
			rlink.setThumbnail(imageRetrieved);
			rlink.setRecordReference(recordRetrieved);

			//embed recordlink in collection - 10th
			Collection col = DB.getCollectionDAO().getById(new ObjectId("54f6eb79e4b0aaf7d551abe1"));
			ArrayList<RecordLink> firstEntries = new ArrayList<RecordLink>();
			firstEntries.add(rlink);
			col.setFirstEntries(firstEntries);
			DB.getCollectionDAO().save(col);

			DB.getRecordLinkDAO().save(rlink);


		//}

	}

	@Test
	public void testDAOsFuntionality() {

		//Get a user by email
		User user1 = DB.getUserDAO().getByEmail("heres42_@mongo.gr");
		jsonPrettyPrint(user1.toString());

		//Get user Searches
		List<Search> userSearches = DB.getUserDAO().getSearchResults("heres42_@mongo.gr");
		for(Search s: userSearches) {
			jsonPrettyPrint(s.toString());
		}

		List<Collection> cols = DB.getUserDAO().getUserCollectionsByEmail("heres42_@mongo.gr");
	}

	@Test
	public void testUserDAO() {
		User user1 = DB.getUserDAO().getByEmail("heres42@mongo.gr");
		User user3 = DB.getUserDAO().getByEmailPassword("heres42@mongo.gr", "helloworld");
		// List<Search> searchList = DB.getUserDAO().getSearchResults("man42");
		System.out.println(user1.toString());
	}

	@Test
	public void renderTemplate() {
		Content html = views.html.index
				.render("Your new application is ready.");
		assertThat(contentType(html)).isEqualTo("text/html");
		assertThat(contentAsString(html)).contains(
				"Your new application is ready.");
	}

	/*
	 * test set up stuff... don't give a sh#t
	 */
	/* ************************************* */
	private long beginTime;
	private long endTime;
	// create an MD5 password
	private MessageDigest digest = null;

	@Before
	public void setUp() {
		DB.getDs().ensureIndexes(User.class);

		beginTime = Timestamp.valueOf("2013-01-01 00:00:00").getTime();
		endTime = Timestamp.valueOf("2013-12-31 00:58:00").getTime();

		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
	}

	/*
	 * Pretty print json
	 */
	private void jsonPrettyPrint(String json) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(json);
		String pretty = gson.toJson(je);
		System.out.println(pretty);
	}

	/**
	 * Method should generate random number that represents a time between two
	 * dates.
	 *
	 * @return
	 */
	private long getRandomTimeBetweenTwoDates() {
		long diff = (endTime - beginTime) + 1;
		return beginTime + (long) (Math.random() * diff);
	}

	public Date generate_random_date_java() {

		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd hh:mm:ss");

		return new Date(getRandomTimeBetweenTwoDates());
	}

	public String randomString() {
		char[] text = new char[50];
		for (int i = 0; i < 50; i++)
			text[i] = "abcdefghijklmnopqrstuvwxyz".charAt(new Random()
					.nextInt(25));
		return text.toString();
	}

	/* *********************************************** */


}
