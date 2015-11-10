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

package com.ibm.ws.repository.exceptions;

import com.ibm.ws.repository.connections.RepositoryConnection;

public abstract class RepositoryBackendException extends RepositoryException {

    private static final long serialVersionUID = -6160923624838346326L;
    private RepositoryConnection _failingBackend;

    public RepositoryBackendException() {
        super();
    }

    public RepositoryBackendException(String message, RepositoryConnection connection) {
        super(message);
        _failingBackend = connection;
    }

    public RepositoryBackendException(Throwable cause, RepositoryConnection connection) {
        super(cause);
        _failingBackend = connection;
    }

    public RepositoryBackendException(String message, Throwable cause, RepositoryConnection connection) {
        super(message, cause);
        _failingBackend = connection;
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }

    public RepositoryConnection getFailingConnection() {
        return _failingBackend;
    }

}
