/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
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
package com.ibm.ws.lars.testutils.fixtures;

import java.io.File;

import com.ibm.ws.lars.testutils.clients.ZipWriteableClient;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.ZipRepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;

public class ZipRepositoryFixture extends RepositoryFixture {

    public static ZipRepositoryFixture createFixture(File zip) {
        ZipRepositoryConnection connection = new ZipRepositoryConnection(zip);
        RepositoryConnection adminConnection = connection;
        RepositoryConnection userConnection = connection;
        RepositoryReadableClient adminClient = connection.createClient();
        RepositoryReadableClient userClient = adminClient;
        RepositoryWriteableClient writableClient = new ZipWriteableClient(zip);

        return new ZipRepositoryFixture(adminConnection, userConnection, adminClient, writableClient, userClient, zip);
    }

    private final File zip;

    private ZipRepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                 RepositoryWriteableClient writableClient, RepositoryReadableClient userClient, File zip) {
        super(adminConnection, userConnection, adminClient, writableClient, userClient);
        this.zip = zip;
    }

    /** {@inheritDoc} */
    @Override
    protected void createCleanRepository() throws Exception {
        if (zip.exists()) {
            zip.delete();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void cleanupRepository() throws Exception {
        if (zip.exists()) {
            zip.delete();
        }
    }

    @Override
    public String toString() {
        return "Zip repo";
    }

}
