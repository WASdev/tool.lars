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

import java.net.URISyntaxException;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;

/**
 * A fixture for a repository server.
 * <p>
 * This rule will empty the repository before each test.
 */
public abstract class RepositoryFixture implements TestRule {

    protected final RepositoryConnection adminConnection;
    protected final RepositoryConnection userConnection;
    protected final RepositoryReadableClient adminClient;
    protected final RepositoryWriteableClient writableClient;
    protected final RepositoryReadableClient userClient;

    /**
     * @param adminConnection a repository connection with admin privileges
     * @param userConnection a repository connection with user privileges
     * @param adminClient a read client with admin privileges
     * @param writableClient a write client with admin privileges
     * @param userClient a read client with user privileges
     */
    protected RepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                RepositoryWriteableClient writableClient, RepositoryReadableClient userClient) {
        this.adminConnection = adminConnection;
        this.userConnection = userConnection;
        this.adminClient = adminClient;
        this.writableClient = writableClient;
        this.userClient = userClient;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                createCleanRepository();
                base.evaluate();
                cleanupRepository();
            }
        };
    }

    protected abstract void createCleanRepository() throws Exception;

    protected abstract void cleanupRepository() throws Exception;

    public RepositoryConnection getAdminConnection() {
        return adminConnection;
    }

    public RepositoryConnection getUserConnection() {
        return userConnection;
    }

    public RepositoryReadableClient getAdminClient() {
        return adminClient;
    }

    public RepositoryWriteableClient getWriteableClient() {
        return writableClient;
    }

    public RepositoryReadableClient getUserClient() {
        return userClient;
    }

    public void refreshTextIndex(String assetId) {}

    /**
     * Returns true if this repository supports updating assets in place
     *
     * @return
     */
    public boolean isUpdateSupported() {
        return true;
    }

    /**
     * Returns the root URL where hosted testfiles can be found
     * <p>
     * Not applicable to all repository types
     *
     * @throws URISyntaxException
     */
    public String getHostedFileRoot() throws URISyntaxException {
        return null;
    }

    @Override
    public abstract String toString();

}
