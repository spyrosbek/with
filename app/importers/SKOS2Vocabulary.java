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


package importers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import net.minidev.json.JSONObject;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import db.DB;


public class SKOS2Vocabulary {

	private String inPath = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.srcpath");
	private String outPath = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.path");
	
	private String newLine = System.getProperty("line.separator");
	
	public static void main(String[] args) {
//		new SKOS2Vocabulary("fashion", 
//				           "Fashion Thesaurus", 
//				           null, 
//				           "fashion", 
//				           "2014-12-10", 
//				           "thesaurus.europeanafashion.eu",
//				           "http://thesaurus.europeanafashion.eu/thesaurus", 
//				           new String[] { "http://thesaurus.europeanafashion.eu/thesaurus/Techniques", 
//				                          "http://thesaurus.europeanafashion.eu/thesaurus/Colours",
//										  "http://thesaurus.europeanafashion.eu/thesaurus/Type",
//				        		          "http://thesaurus.europeanafashion.eu/thesaurus/Materials" }
//		);
		new SKOS2Vocabulary("gemet", 
	        "GEMET Thesaurus", 
	        null, 
	        "gemet", 
	        "3.1", 
	        "www.eionet.europa.eu",
            "http://www.eionet.europa.eu/gemet/gemetThesaurus", 
	        null );

	}
	
	public SKOS2Vocabulary(String fn, String title, String labelProperty, String name, String version, String filter, String scheme, String[] keepSchemes) {

		try {
			readOntology(fn, title, labelProperty, name, version, filter, scheme, keepSchemes);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void readOntology(String fn, String title, String labelProperty, String name, String version, String filter, String scheme, String[] keepSchemes) throws OWLOntologyCreationException, IOException {

		Set<String> ks = null;
		if (keepSchemes != null) {
			ks = new HashSet<>();
			for (String s : keepSchemes) {
				ks.add(s);
			}
		}
		
		Model model = ModelFactory.createDefaultModel();
		File dir = new File(inPath + File.separator + fn);
		for (File f : dir.listFiles()) {
			System.out.println("Reading " + f);
			model.read(f.getAbsolutePath());
		}
		
		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", name);
		vocabulary.put("version", version);
		
		try (FileWriter fr = new FileWriter(new File(outPath + File.separator + fn + ".txt"));
				BufferedWriter br = new BufferedWriter(fr)) {
	
			String queryString;
			Query query;
			
			queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?subject  WHERE {?subject a <http://www.w3.org/2004/02/skos/core#ConceptScheme>}" ;
			query = QueryFactory.create(queryString) ; 
			
	
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
					QuerySolution qs = results.next();
					
					String uri = qs.get("subject").asResource().getURI();
					if (keepSchemes != null && !ks.contains(uri)) {
						continue;
					}
					
					ObjectNode main = makeMainStructure(uri, model);
					
					uri = "<" + uri + ">";
					
					Set<String> withBroader = new HashSet<>();
					
					queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?z WHERE {?z skos:inScheme " + uri + " . ?z skos:broader ?q . ?q skos:inScheme " + uri + " .}" ;
					query = QueryFactory.create(queryString) ; 
					try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
						for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
							QuerySolution q = res.next();
							withBroader.add(q.get("z").asResource().getURI());
						}
					}
					
					Set<String> keep = new HashSet<>();
					
					queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:inScheme " + uri + "}" ;
					query = QueryFactory.create(queryString) ; 
					try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
						for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
							QuerySolution q = res.next();
							
							String curi = q.get("uri").asResource().getURI();
							if (!withBroader.contains(curi)) {
								keep.add(curi);
							}
						}
					}

					queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:topConceptOf " + uri + "}" ;
					query = QueryFactory.create(queryString) ; 
					try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
						for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
							QuerySolution q = res.next();
							
							keep.add(q.get("uri").asResource().getURI());
						}
					}
					
					ArrayNode array = Json.newObject().arrayNode();
					
					for (String muri : keep) {
						array.add(makeMainStructure(muri, model));
					}
					
					if (array.size() > 0) {
						main.put("topConcepts", array);
					}

					
					main.put("vocabulary", vocabulary);

					ObjectNode json = Json.newObject();
					json.put("semantic", main);

					br.write(json.toString());
					br.write(newLine);
				}
			}
	
			queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?subject  WHERE {?subject a <http://www.w3.org/2004/02/skos/core#Concept>}" ;
			query = QueryFactory.create(queryString) ; 
	
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
					QuerySolution qs = results.next();
					
					String uri = qs.get("subject").asResource().getURI();
					
					ObjectNode main = makeMainStructure(uri, model);
					
					uri = "<" + uri + ">";
							
					JsonNode scopeNote = makeLiteralNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?literal WHERE {" + uri + " skos:scopeNote ?literal}", model, "literal");
					if (scopeNote != null) {
						main.put("scopeNote", scopeNote);
					}
					
					ArrayNode broader = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {" + uri + " skos:broader ?q}} UNION {SELECT ?q WHERE {?q skos:narrower " + uri + "}}}", model, "q");
					if (broader != null) {
						main.put("broader", broader);
					}
					
					ArrayNode narrower = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {" + uri + " skos:narrower ?q}} UNION {SELECT ?q WHERE {?q skos:broader " + uri + "}}}", model, "q");
					if (narrower != null) {
						main.put("narrower", narrower);
					}
					
					ArrayNode broaderTransitive = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {" + uri + " skos:broader+ ?q}} UNION {SELECT ?q WHERE {?q skos:narrower+ " + uri + "}}}", model, "q");
					if (broaderTransitive != null) {
						main.put("broaderTransitive", broaderTransitive);
					}
					
					ArrayNode inCollections = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:member " + uri + "}", model, "uri");
					if (inCollections != null) {
						main.put("inCollections", inCollections);
					}
	
					ArrayNode related = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {" + uri + " skos:related ?q}} UNION {SELECT ?q WHERE {?q skos:related " + uri + "}}}", model, "q");
					if (related != null) {
						main.put("related", related);
					}
					
					ArrayNode exactMatch = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:exactMatch ?uri}", model, "uri");
					if (exactMatch != null) {
						main.put("exactMatch", exactMatch);
					}
					
					ArrayNode closeMatch = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:closeMatch ?uri}", model, "uri");
					if (closeMatch != null) {
						main.put("closeMatch", closeMatch);
					}
					
					ArrayNode inSchemes = makeFilteredURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:inScheme ?uri}", model, "uri", ks);
					if (inSchemes != null) {
						main.put("inSchemes", inSchemes);
					} else {
						ArrayNode schemes = Json.newObject().arrayNode();
						schemes.add(scheme);
						main.put("inSchemes", schemes);
					}
					
					ArrayNode topConceptOf = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:topConceptOf ?uri}", model, "uri");
					if (topConceptOf != null) {
						main.put("topConceptOf", topConceptOf);
					}
	
					main.put("vocabulary", vocabulary);

					ObjectNode json = Json.newObject();
					json.put("semantic", main);

					br.write(json.toString());
					br.write(newLine);

				}
			}
			
			queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?subject  WHERE {?subject a <http://www.w3.org/2004/02/skos/core#Collection>}" ;
			query = QueryFactory.create(queryString) ; 
			
	
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
					QuerySolution qs = results.next();
					
					String uri = qs.get("subject").asResource().getURI();
					
					ObjectNode main = makeMainStructure(uri, model);
					
					uri = "<" + uri + ">";
							
					ArrayNode members = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:member ?uri}", model, "uri");
					if (members != null) {
						main.put("members", members);
					}
	
					main.put("vocabulary", vocabulary);
					
					ObjectNode json = Json.newObject();
					json.put("semantic", main);

					br.write(json.toString());
					br.write(newLine);
				}
			}
		}
//
//		
//		int cutout = Math.max(fn.lastIndexOf("/"), fn.lastIndexOf("\\"));
//		if (cutout > 0) {
//			fn = fn.substring(cutout);
//		}
//		try (FileWriter fr = new FileWriter(new File(outPath + File.separator + fn + ".txt"));
//            BufferedWriter br = new BufferedWriter(fr)) {
//			
//			for (ObjectNode on : jsons) {
//				ObjectNode sem = (ObjectNode)on.get("semantic");
//				if (sem.get("prefLabel").size() == 0) {
//					sem.remove("prefLabel");
//				}
//				
//				ArrayNode bro = (ArrayNode)sem.get("broader");
//				if (bro != null) {
//					for (Iterator<JsonNode> iter = bro.elements(); iter.hasNext();) {
//						ObjectNode el = ((ObjectNode)iter.next());
//						if (el.get("prefLabel").size() == 0) {
//							el.remove("prefLabel");
//						}
//					}
//				}
//
//				ArrayNode brt = (ArrayNode)sem.get("broaderTransitive");
//				if (brt != null) {
//					for (Iterator<JsonNode> iter = brt.elements(); iter.hasNext();) {
//						ObjectNode el = ((ObjectNode)iter.next());
//						if (el.get("prefLabel").size() == 0) {
//							el.remove("prefLabel");
//						}
//					}
//				}
//				
//				ArrayNode nar = (ArrayNode)sem.get("narrower");
//				if (nar != null) {
//					for (Iterator<JsonNode> iter = nar.elements(); iter.hasNext();) {
//						ObjectNode el = ((ObjectNode)iter.next());
//						if (el.get("prefLabel").size() == 0) {
//							el.remove("prefLabel");
//						}
//					}
//				}
//				
//				br.write(on.toString());
//				br.write(newLine);
//			}
//			
//			ObjectNode jtop = Json.newObject();
//			ObjectNode topc = Json.newObject();
//			topc.put("uri", scheme);
//			topc.put("type", "http://www.w3.org/2004/02/skos/core#ConceptScheme");
//			
//			ObjectNode prefLabel = Json.newObject();
//			prefLabel.put("en", title);
//			topc.put("prefLabel", prefLabel);
//			
//			ArrayNode arr = Json.newObject().arrayNode();
//			for (OWLClass sc : topConcepts) {
//				ObjectNode term = Json.newObject();
//				term.put("uri", sc.getIRI().toString());
//				term.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
//				
//				ObjectNode pLabel = labelMap.get(sc);
//				if (pLabel == null) {
//					pLabel = Json.newObject();
//					labelMap.put(sc, pLabel);
//				}
//				term.put("prefLabel", pLabel);
//				
//				arr.add(term);
//			}
//			
//			topc.put("topConcepts", arr);
//			topc.put("vocabulary", vocabulary);
//
//			jtop.put("semantic", topc);
//			
//			br.write(jtop.toString());
//
//        } catch (IOException e) {
//			e.printStackTrace();
//		}
	
	}

	private static JsonNode makeLiteralNode(String queryString, Model model, String var) {
		Literal literal = new Literal();
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get(var).asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguage(lang);
					if (ll == null) {
						ll = Language.UNKNOWN;
					}
				} else {
					ll = Language.UNKNOWN;
				}
				
				literal.addLiteral(ll, lit.getString());
			}
		}
		
		if (literal.size() > 0) {
			return Json.toJson(literal);
		} else {
			return null;
		}
	}
	
	private static ArrayNode makeURIArrayNode(String queryString, Model model, String var) {
		ArrayNode array = Json.newObject().arrayNode();
		
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				String uri = s.get(var).asResource().getURI();
				array.add(uri);
			}
		}
		
		if (array.size() > 0) {
			return array;
		} else {
			return null;
		}
	}
	
	private static ArrayNode makeFilteredURIArrayNode(String queryString, Model model, String var, Set<String> keepOnly) {
		ArrayNode array = Json.newObject().arrayNode();
		
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				String uri = s.get(var).asResource().getURI();
				if (keepOnly != null && keepOnly.contains(uri)) {
					array.add(uri);
				}
			}
		}
		
		if (array.size() > 0) {
			return array;
		} else {
			return null;
		}
	}
	
	private static ArrayNode makeNodesArray(String queryString, Model model, String var) {
		ArrayNode array = Json.newObject().arrayNode();
		
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				array.add(makeMainStructure(s.get(var).asResource().getURI(), model));
			}
		}
		
		if (array.size() > 0) {
			return array;
		} else {
			return null;
		}
	}

	
	private static ObjectNode makeMainStructure(String urit, Model model) {
		Literal prefLabel = new Literal();
		MultiLiteral altLabel = new MultiLiteral();
		
		String uri = "<" + urit + ">";
		
//		System.out.println(">> " + urit);
		
		String queryString;
		Query query;
		
		queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?literal WHERE {" + uri + " skos:prefLabel ?literal}";
		query = QueryFactory.create(queryString);
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get("literal").asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguage(lang);
					if (ll == null) {
						ll = Language.UNKNOWN;
					}
				} else {
					ll = Language.UNKNOWN;
				}
				
				prefLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
			}
		}
		
		queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?literal WHERE {" + uri + " skos:altLabel ?literal}";
		query = QueryFactory.create(queryString) ;
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get("literal").asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguage(lang);
					if (ll == null) {
						ll = Language.UNKNOWN;
					}
				} else {
					ll = Language.UNKNOWN;
				}
				
				if (prefLabel.getLiteral(ll) == null) {
					prefLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
				} else {
					altLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
				}
				
			}
		}
		
		queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?literal WHERE {" + uri + " rdfs:label ?literal}";
		query = QueryFactory.create(queryString) ;
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get("literal").asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguage(lang);
					if (ll == null) {
						ll = Language.UNKNOWN;
					}
				} else {
					ll = Language.UNKNOWN;
				}
				
				if (prefLabel.getLiteral(ll) == null) {
					prefLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
				} else {
					altLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
				}
				
			}
		}

		String type = null;

		queryString = "SELECT ?type WHERE {" + uri + " a ?type}";
		query = QueryFactory.create(queryString) ;
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				String t = s.get("type").asResource().getURI();
				if (t.contains("skos/core#")) {
					type = t;
					break;
				}
			}
		}
		
		ObjectNode json = Json.newObject();
		json.put("uri", urit);
		
		if (type != null) {
			json.put("type", type);
		}
		
		if (prefLabel.size() > 0) {
			json.put("prefLabel", Json.toJson(prefLabel));
			
		}
		if (altLabel.size() > 0) {
			json.put("altLabel", Json.toJson(altLabel));
		}

		return json;
	}
}
