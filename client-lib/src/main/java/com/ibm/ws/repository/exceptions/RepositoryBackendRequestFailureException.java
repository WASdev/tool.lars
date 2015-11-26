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

import java.net.HttpURLConnection;

import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

/**
 * This exception is thrown when there was an error in the HTTP request to the repository that lead to a response code that was not successful (2xx). The response code is
 * available on {@link #getResponseCode()} and the message from the error stream on the HTTP URL connection is available via {@link #getErrorMessage()} with the full contents of
 * the error stream being available through {@link #getErrorStreamContents()}.
 */
public class RepositoryBackendRequestFailureException extends
                RepositoryBackendException {

    private static final long serialVersionUID = 5987530731417694030L;
    private final RequestFailureException requestFailureException;

    public RepositoryBackendRequestFailureException(RequestFailureException cause, RepositoryConnection connection) {
        super(cause, connection);
        this.requestFailureException = cause;
    }

    /**
     * Returns the response code from the HTTP URL connection when connecting to the repository backend.
     * 
     * @return The response code
     * @see HttpURLConnection#getResponseCode()
     */
    public int getResponseCode() {
        return requestFailureException.getResponseCode();
    }

    /**
     * Returns the error message written to the error stream on the HTTP URL connection when connecting to the repository backend.
     * 
     * @return The error message
     * @see HttpURLConnection#getErrorStream()
     */
    public String getErrorMessage() {
        return requestFailureException.getErrorMessage();
    }

    /**
     * Returns the full contents of the error stream.
     * 
     * @return The contents of the error stream
     * @see HttpURLConnection#getErrorStream()
     */
    public String getErrorStreamContents() {
        return requestFailureException.getErrorStreamContents();
    }

}
