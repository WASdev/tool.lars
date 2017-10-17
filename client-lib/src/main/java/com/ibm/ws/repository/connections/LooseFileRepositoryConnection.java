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
package com.ibm.ws.repository.connections;

import java.io.File;
import java.util.Collection;

import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.transport.client.LooseFileClient;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;

/**
 * Connection to a repository which consists of a loose collection of json files. No
 * attachments are supported for this type of repository and an UnsupportedOperationException
 * will be thrown if any attachment operations are invoked.
 */
public class LooseFileRepositoryConnection extends AbstractRepositoryConnection implements RepositoryConnection {

    // The collection of json files that this repository represents
    private final Collection<File> _assets;

    /**
     * @param assets The collection of json file names that this repository represents
     */
    public LooseFileRepositoryConnection(Collection<File> assets) {
        _assets = assets;
    }

    /**
     * This repository has no concept of a location since it contains a collection
     * of loose files
     */
    @Override
    public String getRepositoryLocation() {
        return null;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public RepositoryReadableClient createClient() {
        return new LooseFileClient(_assets);
    }

}
