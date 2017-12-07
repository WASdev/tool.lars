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

import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.SingleFileRepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;

/**
 * Repository fixture for a single file json repository
 */
public class SingleFileRepositoryFixture extends RepositoryFixture {

    private final File jsonFile;

    public static SingleFileRepositoryFixture createFixture(File jsonFile) {

        SingleFileRepositoryConnection connection = new SingleFileRepositoryConnection(jsonFile);
        RepositoryConnection adminConnection = connection;
        RepositoryConnection userConnection = connection;
        RepositoryReadableClient adminClient = connection.createClient();
        RepositoryReadableClient userClient = adminClient;
        RepositoryWriteableClient writableClient = (RepositoryWriteableClient) adminClient;

        return new SingleFileRepositoryFixture(adminConnection, userConnection, adminClient, writableClient, userClient, jsonFile);
    }

    private SingleFileRepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                        RepositoryWriteableClient writableClient, RepositoryReadableClient userClient, File jsonFile) {
        super(adminConnection, userConnection, adminClient, writableClient, userClient);
        this.jsonFile = jsonFile;
    }

    @Override
    protected void createCleanRepository() throws Exception {
        cleanupRepository();
        SingleFileRepositoryConnection.createEmptyRepository(jsonFile);
    }

    @Override
    protected void cleanupRepository() throws Exception {
        if (jsonFile.exists()) {
            jsonFile.delete();
        }
    }

    @Override
    public String toString() {
        return "Single file repo";
    }

    @Override
    public boolean isUpdateSupported() {
        return false;
    }

    /**
     * This repository does not support attachments
     */
    @Override
    public boolean isAttachmentSupported() {
        return false;
    }

}
