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
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.ZipClient;

/**
 *
 */
public class ZipRepositoryConnection extends AbstractRepositoryConnection implements RepositoryConnection {

    private final File _zip;

    /**
     * @param type
     */
    public ZipRepositoryConnection(File zip) {
        _zip = zip;
    }

    public File getZip() {
        return _zip;
    }

    /** {@inheritDoc} */
    @Override
    public String getRepositoryLocation() {
        return _zip.getAbsolutePath();
    }

    @Override
    public RepositoryReadableClient createClient() {
        return new ZipClient(getZip());
    }

}
