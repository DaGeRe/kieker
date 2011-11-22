/***************************************************************************
 * Copyright 2011 by
 *  + Christian-Albrechts-University of Kiel
 *    + Department of Computer Science
 *      + Software Engineering Group 
 *  and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package kieker.monitoring.writer.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import kieker.common.record.HashRecord;

/**
 * @author Andre van Hoorn, Jan Waller
 */
public class MappingFileWriter {

	private final File mappingFile;

	public MappingFileWriter(final String path) throws IOException {
		final StringBuffer sbm = new StringBuffer(path.length() + 11); // NOCS (MagicNumber)
		sbm.append(path).append(File.separatorChar).append("kieker.map");
		final String mappingFileFn = sbm.toString();
		this.mappingFile = new File(mappingFileFn);
		if (!this.mappingFile.createNewFile()) {
			throw new IOException("Mapping File '" + mappingFileFn + "' already exists.");
		}
	}

	public final void write(final HashRecord hashRecord) throws IOException {
		synchronized (this.mappingFile) {
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.mappingFile, true)));
				pw.print('$');
				pw.print(hashRecord.getId());
				pw.print('=');
				pw.print(hashRecord.getObject());
				pw.println();
				if (pw.checkError()) {
					throw new IOException("Error writing to mappingFile " + this.mappingFile.toString());
				}
			} finally {
				if (pw != null) {
					pw.close();
				}
			}
		}
	}
}
