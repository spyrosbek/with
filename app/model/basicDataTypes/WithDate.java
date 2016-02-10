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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import play.Logger;

/**
 * Capture accurate and inaccurate dates in a visualisable way. Enable search
 * for year. This is a point in time. If you mean a timespan, use different
 * class.
 */
public class WithDate {
	Date isoDate;
	// facet
	// year should be filled in whenever possible
	// 100 bc is translated to -100
	int year;

	// controlled expression of an epoch "stone age", "renaissance", "16th
	// century"
	LiteralOrResource epoch;

	// if the year is not accurate, give the inaccuracy here( 0- accurate)
	int approximation;

	// ontology based time
	String uri;
	// ResourceType uriType;

	// mandatory, other fields are extracted from that
	String free;

	public WithDate() {
		super();
	}

	public Date getIsoDate() {
		return isoDate;
	}

	public void setIsoDate(Date d) {
		if (d != null) {
			this.isoDate = d;
			Calendar instance = Calendar.getInstance();
			instance.setTime(d);
			year = instance.get(Calendar.YEAR);
		}
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public LiteralOrResource getEpoch() {
		return epoch;
	}

	public void setEpoch(LiteralOrResource epoch) {
		this.epoch = epoch;
	}

	public int getApproximation() {
		return approximation;
	}

	public void setApproximation(int approximation) {
		this.approximation = approximation;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	//
	// public ResourceType getUriType() {
	// return uriType;
	// }
	//
	// public void setUriType(ResourceType uriType) {
	// this.uriType = uriType;
	// }

	public String getFree() {
		return free;
	}

	public void setFree(String free) {
		this.free = free;
	}

	public WithDate(String free) {
		super();
		setDate(free);
	}

	public void setDate(String free) {
		this.free = free;
		// code to init the other Date representations
		if (sources.core.Utils.isNumericInteger(free)) {
			this.year = Integer.parseInt(free);
		} else if (free.matches("\\d+\\s+(bc|BC)")) {
			this.year = Integer.parseInt(free.split("\\s")[0]);
		} else if (free.matches("\\d\\d\\d\\d-\\d\\d\\d\\d")) {
			this.year = Integer.parseInt(free.split("-")[0]);
		} else if (free.matches("\\d\\d-\\d\\d-\\d\\d\\d\\d")) {
			try {
				setIsoDate((new SimpleDateFormat("dd-mm-yyyy")).parse(free));
			} catch (ParseException e) {
				Logger.warn("unrecognized date: " + free);
			}
		} else if (free.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d")) {
			try {
				setIsoDate((new SimpleDateFormat("yyyy-mm-dd")).parse(free));
			} catch (ParseException e) {
				Logger.warn("unrecognized date: " + free);
			}
		} else if (sources.core.Utils.isValidURL(free)) {
			this.uri = free;
			// this.uriType = ResourceType.uri;
		} else {
			Logger.warn("unrecognized date: " + free);
		}
	}

}
