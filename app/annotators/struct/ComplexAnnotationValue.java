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


package annotators.struct;

import annotators.struct.AnnotatedObject;
import annotators.struct.AnnotationValue;

public class ComplexAnnotationValue extends AnnotationValue {
	private Object[] value;
	
	public ComplexAnnotationValue(Object... vv) {
		this.value = vv;
	}
	
	public int hashCode() {
		return value.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof ComplexAnnotationValue)) {
			return false;
		}
		
		Object[] v2 = ((ComplexAnnotationValue)obj).value;
		
		if (v2.length != value.length) {
			return false;
		}
		
		for (int i = 0; i < value.length; i++) {
			if (!value[i].equals(v2[i])) {
				return false;
			}
		}
		
		return true;
	}
	
	public String toString() {
		String s = "[";
		for (int i = 0; i < value.length; i++) {
			if (i > 0) {
				s += ", ";
			}
			if (value[i] instanceof AnnotatedObject) {
				s += "@" + ((AnnotatedObject)value[i]).getID() + ":" + ((AnnotatedObject)value[i]).getText() + "@";
			} else {
				s += value[i].toString();
			}
		}
		
		return s + "]";
	}

	public Object getValue() {
		return value;
	}

}
