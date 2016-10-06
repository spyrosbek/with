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


package vocabularies;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import play.libs.Json;
import annotators.Annotator;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.ThesaurusController;
import db.DB;

public class Vocabulary {

	public static enum VocabularyType {
		THESAURUS,
		RESOURCE
	}
	
	static {
		loadVocabularies();
	}

	private String name;
	private String label;
	private VocabularyType type;
	private Class<? extends Annotator> annotator;
	private String version;
	
	private static List<Vocabulary> vocabularies;
	
	public static List<Vocabulary> getVocabularies() {
		return vocabularies;
	}
	
	public static void loadVocabularies() {
		vocabularies = new ArrayList<>();
		
		for (String p : DB.getConf().getString("vocabulary.names").split(",")) {
			String title = DB.getConf().getString("vocabulary." + p + ".title");
			String t = DB.getConf().getString("vocabulary." + p + ".type");
			VocabularyType type = null;
			if (t.equals("thesaurus")) {
				type = VocabularyType.THESAURUS;
			} else {
				type = VocabularyType.RESOURCE;
			}
			String ann = DB.getConf().getString("vocabulary." + p + ".annotator");
			Class<? extends Annotator> annotator = null;
			if (ann != null) {
				try {
					annotator = (Class<? extends Annotator>)Class.forName("annotators." + ann);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			String version = DB.getConf().getString("vocabulary." + p + ".version");
			if (version == null) {
				version = "";
			}
			
			vocabularies.add(new Vocabulary(p, title, type, annotator, version));
		}
	}
	

	
	public Vocabulary(String name, String label, VocabularyType type, Class<? extends Annotator> annotator, String version) {
		this.name = name;
		this.setType(type);
		this.setLabel(label);
		this.setAnnotator(annotator);
		this.setVersion(version);
	}
	
	public String getName() {
		return name;
	}
	
	public static Vocabulary getVocabulary(String name){
		for (Vocabulary voc : vocabularies) {
			if (voc.name.equals(name)) {
				return voc;
			} 
		}
		
		return null;
	}

	public Class<? extends Annotator> getAnnotator() {
		return annotator;
	}

	public void setAnnotator(Class<? extends Annotator> annotator) {
		this.annotator = annotator;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public VocabularyType getType() {
		return type;
	}

	public void setType(VocabularyType type) {
		this.type = type;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
