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
package com.ibm.ws.lars.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.ws.lars.rest.exceptions.RepositoryClientException;

/**
 * Thrown to indicate that the user has provided an invalid value for a parameter
 */
public class InvalidParameterException extends RepositoryClientException {

    /**  */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new invalid parameter exception
     * <p>
     * The message will be shown to the user and should explain which parameter is incorrect and
     * what the problem is.
     * <p>
     * E.g. <code>Parameter foo must be a number</code>
     *
     * @param message message explaining the problem
     */
    public InvalidParameterException(String message) {
        super(message);
    }

    /** {@inheritDoc} */
    @Override
    public Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }

}
