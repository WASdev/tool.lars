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
import java.util.ArrayList;
import java.util.Collection;

import com.ibm.ws.lars.testutils.clients.LooseFileWriteableClient;
import com.ibm.ws.repository.connections.LooseFileRepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;

/**
 * This fixture class creates the collection which will back both the readable and writable clients.
 * Bit yucky sharing the collection like that but we need some way to update the backing collection
 * on the readable client for the tests to work.
 */
public class LooseFileRepositoryFixture extends RepositoryFixture {

    private final Collection<File> _assets;
    private final File _root;

    public static LooseFileRepositoryFixture createFixture(File root) {
        Collection<File> assets = new ArrayList<File>();
        LooseFileRepositoryConnection connection = new LooseFileRepositoryConnection(assets);
        RepositoryConnection adminConnection = connection;
        RepositoryConnection userConnection = connection;
        RepositoryReadableClient adminClient = connection.createClient();
        RepositoryReadableClient userClient = adminClient;
        RepositoryWriteableClient writableClient = new LooseFileWriteableClient(assets, root);

        return new LooseFileRepositoryFixture(adminConnection, userConnection, adminClient, writableClient, userClient, assets, root);
    }

    private LooseFileRepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                       RepositoryWriteableClient writableClient, RepositoryReadableClient userClient, Collection<File> assets, File root) {
        super(adminConnection, userConnection, adminClient, writableClient, userClient);
        _root = root;
        _assets = assets;
    }

    @Override
    protected void createCleanRepository() throws Exception {
        cleanupRepository();
    }

    @Override
    protected void cleanupRepository() throws Exception {

        // Delete all the json files
        for (File f : _assets) {
            if (f.exists()) {
                f.delete();
            }
        }

        // Clear out the file from list from the repository
        _assets.clear();
    }

    @Override
    public String toString() {
        return "Loose file repo";
    }

    /**
     * This repository does not support attachments
     */
    @Override
    public boolean isAttachmentSupported() {
        return false;
    }

    public LooseFileRepositoryConnection getWritableConnection() {
        return new LooseFileRepositoryConnection(_assets) {
            @Override
            public RepositoryReadableClient createClient() {
                return new LooseFileWriteableClient(_assets, _root);
            }
        };
    }

}
