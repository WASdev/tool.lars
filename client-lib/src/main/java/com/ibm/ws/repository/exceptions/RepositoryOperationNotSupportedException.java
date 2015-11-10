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

/**
 * This exception is thrown if an operation is attempted on a client that does not support that operation.
 * For example some clients can only perform read operations and trying to perform write operations will
 * cause this exception to be thrown
 */
public class RepositoryOperationNotSupportedException extends RepositoryBackendException {

    private static final long serialVersionUID = -8787752138728688643L;

    public RepositoryOperationNotSupportedException() {
        super();
    }

    public RepositoryOperationNotSupportedException(String message, RepositoryConnection connection) {
        super(message, connection);
    }

    public RepositoryOperationNotSupportedException(Throwable cause, RepositoryConnection connection) {
        super(cause, connection);
    }

    public RepositoryOperationNotSupportedException(String message, Throwable cause, RepositoryConnection connection) {
        super(message, cause, connection);
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }
}
