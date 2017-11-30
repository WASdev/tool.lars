/*******************************************************************************
 * Copyright (c) 2017 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ibm.ws.repository.connections;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.SingleFileClient;

/**
 * A repository connection which reads the metadata for all of the assets in the repository from a single JSON file.
 */
public class SingleFileRepositoryConnection extends AbstractRepositoryConnection implements RepositoryConnection {

    private final File jsonFile;

    public SingleFileRepositoryConnection(File jsonFile) {
        this.jsonFile = jsonFile;
    }

    @Override
    public String getRepositoryLocation() {
        return jsonFile.getAbsolutePath();
    }

    @Override
    public RepositoryReadableClient createClient() {
        return new SingleFileClient(jsonFile);
    }

    /**
     * Create an empty single file Repository connection.
     * <p>
     * Repository data will be stored in {@code jsonFile} which must not exist and will be created by this method.
     *
     * @param jsonFile the location for the repository file
     * @return the repository connection
     * @throws IOException if {@code jsonFile} already exists or there is a problem creating the file
     */
    public static SingleFileRepositoryConnection createEmptyRepository(File jsonFile) throws IOException {
        if (jsonFile.exists()) {
            throw new IOException("Cannot create empty repository as the file already exists: " + jsonFile.getAbsolutePath());
        }

        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(jsonFile), "UTF-8");
            writer.write("[]");
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        return new SingleFileRepositoryConnection(jsonFile);
    }

}
