/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright 2005-2011 The ConQAT Project                                   |
|                                                                          |
| Licensed under the Apache License, Version 2.0 (the "License");          |
| you may not use this file except in compliance with the License.         |
| You may obtain a copy of the License at                                  |
|                                                                          |
|    http://www.apache.org/licenses/LICENSE-2.0                            |
|                                                                          |
| Unless required by applicable law or agreed to in writing, software      |
| distributed under the License is distributed on an "AS IS" BASIS,        |
| WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. |
| See the License for the specific language governing permissions and      |
| limitations under the License.                                           |
+-------------------------------------------------------------------------*/
package org.conqat.engine.core.conqatdoc.compare;

import java.util.Comparator;

import org.conqat.engine.core.driver.specification.ISpecification;

/**
 * Comparator for comparing specifications by their full name.
 * 
 * @author Benjamin Hummel
 * @author Florian Deissenboeck
 * @author $Author: juergens $
 * @version $Rev: 35194 $
 * @ConQAT.Rating GREEN Hash: FAFC415993EACE3D68646B1C1010AD13
 */
public class SpecificationNameComparator implements Comparator<ISpecification> {

	/** Instance of the comparator. */
	public final static SpecificationNameComparator INSTANCE = new SpecificationNameComparator();

	/** Compare specifications by name. */
	@Override
	public int compare(ISpecification specification1,
			ISpecification specification2) {
		return specification1.getName().compareTo(specification2.getName());
	}
}