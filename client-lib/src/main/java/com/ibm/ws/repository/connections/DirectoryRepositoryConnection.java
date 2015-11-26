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

import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.transport.client.DirectoryClient;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;

/**
 *
 */
public class DirectoryRepositoryConnection extends AbstractRepositoryConnection implements RepositoryConnection {

    private final File _root;

    /**
     * @param type
     */
    public DirectoryRepositoryConnection(File root) {
        _root = root;
    }

    public File getRoot() {
        return _root;
    }

    /** {@inheritDoc} */
    @Override
    public String getRepositoryLocation() {
        return _root.getAbsolutePath();
    }

    @Override
    public RepositoryReadableClient createClient() {
        return new DirectoryClient(getRoot());
    }

}
