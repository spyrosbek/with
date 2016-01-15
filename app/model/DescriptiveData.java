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

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Embedded;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DescriptiveData {

	public DescriptiveData() {
	}

	public DescriptiveData(MultiLiteral label) {
		this.label = label;
	}

	// one line content description with identifiable characteristic
	@NotNull
	@NotBlank
	private MultiLiteral label;

	// arbitrary length content description
	private MultiLiteral description;

	// an indexers dream !! They can be literal concepts and enriched easily
	private MultiLiteralOrResource keywords;

	// This are reachable URLs
	private String isShownAt, isShownBy;

	// The whole legal bla, unedited, from the source, mostly cc0
	private LiteralOrResource metadataRights;

	// rdf .. Agent, Artist, Painter, Painting, Series
	private String rdfType;

	// URIs how this Resource is known elsewhere
	private List<String> sameAs;

	// in a timeline where would this resource appear
	private List<WithDate> dates;

	// alternative title or name or placename
	private MultiLiteral altLabels;

	public MultiLiteral getLabel() {
		return label;
	}

	public void setLabel(MultiLiteral label) {
		this.label = label;
	}

	public MultiLiteral getDescription() {
		return description;
	}

	public void setDescription(MultiLiteral description) {
		this.description = description;
	}

	public MultiLiteralOrResource getKeywords() {
		return keywords;
	}

	public void setKeywords(MultiLiteralOrResource keywords) {
		this.keywords = keywords;
	}

	public String getIsShownAt() {
		return isShownAt;
	}

	public void setIsShownAt(String isShownAt) {
		this.isShownAt = isShownAt;
	}

	public String getIsShownBy() {
		return isShownBy;
	}

	public void setIsShownBy(String isShownBy) {
		this.isShownBy = isShownBy;
	}

	public LiteralOrResource getMetadataRights() {
		return metadataRights;
	}

	public void setMetadataRights(LiteralOrResource metadataRights) {
		this.metadataRights = metadataRights;
	}

	public String getRdfType() {
		return rdfType;
	}

	public void setRdfType(String rdfType) {
		this.rdfType = rdfType;
	}

	public List<String> getSameAs() {
		return sameAs;
	}

	public void setSameAs(List<String> sameAs) {
		this.sameAs = sameAs;
	}

	public List<WithDate> getDates() {
		return dates;
	}

	public void setDates(List<WithDate> dates) {
		this.dates = dates;
	}

	public MultiLiteral getAltLabels() {
		return altLabels;
	}

	public void setAltLabels(MultiLiteral altLabels) {
		this.altLabels = altLabels;
	}
}
