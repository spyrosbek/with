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


package model.basicDataTypes;

import java.util.List;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import play.Logger;
import sources.core.Utils;
import sources.utils.StringUtils;
import utils.ListUtils;

public interface ILiteral {
	

	public static double THRESHOLD = 0.95;

	void addLiteral(Language lang, String value);

	default void addLiteral(String value) {
		addLiteral(Language.UNKNOWN, value);
	}
	
	default void addSmartLiteral(String value, Language... suggestedLanguages) {
		if (!Utils.isValidURL(value)){
			boolean shortText = value.length()<100;
			// create a text object factory
			TextObjectFactory textObjectFactory = shortText ?
						CommonTextObjectFactories.forDetectingShortCleanText()
					:
						CommonTextObjectFactories.forDetectingOnLargeText();
			// query:
			TextObject textObject = textObjectFactory.forText(value);
			List<DetectedLanguage> probabilities = StringUtils.getLanguageDetector().getProbabilities(textObject);
			
	        if (!probabilities.isEmpty()) {
	        	System.out.println(probabilities);
	        	boolean gotSome = false;
	            for (DetectedLanguage detectedLanguage : probabilities) {
					if (detectedLanguage.getProbability()>=THRESHOLD && 
							(suggestedLanguages==null || suggestedLanguages.length==0 ||
							ListUtils.anyof(suggestedLanguages, 
									(Language l)-> l.belongsTo(detectedLanguage.getLanguage())))){
						addLiteral(Language.getLanguage(detectedLanguage.getLanguage()), value);
						Logger.info("Detected ["+detectedLanguage.getLanguage()+"] for " + value);
						gotSome = true;
					}
				}
	            if (gotSome)
	            	return;
	        }
			Logger.warn("Unknown Language for text " + value);
		}
		addLiteral(Language.UNKNOWN, value);
	}

}