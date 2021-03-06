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
package org.conqat.engine.persistence.store.bdb;

import java.io.File;

import org.conqat.engine.persistence.store.IStorageSystem;
import org.conqat.engine.persistence.store.StorageException;
import org.conqat.engine.persistence.store.StorageSystemTestBase;

/**
 * Tests the {@link BDBStorageSystem}
 * 
 * @author hummelb
 * @author $Author: deissenb $
 * @version $Rev: 34252 $
 * @levd.rating GREEN Hash: 4D3FB287D3685EECDC650C0424F3F75E
 */
public class BDBStorageSystemTest extends StorageSystemTestBase {

	/** {@inheritDoc} */
	@Override
	protected IStorageSystem openStorage(File baseDir) throws StorageException {
		return new BDBStorageSystem(baseDir, 4);
	}
}