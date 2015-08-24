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

package com.ibm.ws.lars.rest.exceptions;

import javax.ws.rs.core.Response;

/**
 * An exception which represents an error in input data from the client
 */
public abstract class RepositoryClientException extends Exception {

    /** @see java.lang.Exception#Exception(String) */
    public RepositoryClientException(String message) {
        super(message);
    }

    /** @see java.lang.Exception#Exception(String,Throwable) */
    public RepositoryClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /** @see java.lang.Exception#Exception() */
    public RepositoryClientException() {
        super();
    }

    /** @see java.lang.Exception#Exception(Throwable) */
    public RepositoryClientException(Throwable cause) {
        super(cause);
    }

    public abstract Response.Status getResponseStatus();

    /**  */
    private static final long serialVersionUID = 1L;

}
