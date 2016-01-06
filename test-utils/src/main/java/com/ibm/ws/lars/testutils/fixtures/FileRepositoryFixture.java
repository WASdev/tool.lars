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

import com.ibm.ws.lars.testutils.clients.DirectoryWriteableClient;
import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;

/**
 *
 */
public class FileRepositoryFixture extends RepositoryFixture {

    private final File root;

    public static FileRepositoryFixture createFixture(File root) {
        DirectoryRepositoryConnection connection = new DirectoryRepositoryConnection(root);
        RepositoryConnection adminConnection = connection;
        RepositoryConnection userConnection = connection;
        RepositoryReadableClient adminClient = connection.createClient();
        RepositoryReadableClient userClient = adminClient;
        RepositoryWriteableClient writableClient = new DirectoryWriteableClient(root);

        return new FileRepositoryFixture(adminConnection, userConnection, adminClient, writableClient, userClient, root);
    }

    private FileRepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                  RepositoryWriteableClient writableClient, RepositoryReadableClient userClient, File root) {
        super(adminConnection, userConnection, adminClient, writableClient, userClient);
        this.root = root;
    }

    @Override
    protected void createCleanRepository() throws Exception {
        recursiveDelete(root);
        root.mkdir();
    }

    @Override
    protected void cleanupRepository() throws Exception {
        recursiveDelete(root);
    }

    private void recursiveDelete(File file) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                recursiveDelete(child);
            }
        }

        file.delete();
    }

    @Override
    public String toString() {
        return "Directory repo";
    }

    public DirectoryRepositoryConnection getWritableConnection() {
        return new DirectoryRepositoryConnection(root) {
            @Override
            public RepositoryReadableClient createClient() {
                return new DirectoryWriteableClient(root);
            }
        };
    }

}
