/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
package com.ibm.ws.lars.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.ws.lars.rest.exceptions.RepositoryClientException;
import com.ibm.ws.lars.rest.exceptions.RepositoryException;

@Path("/provoke-error")
public class ErrorResource {

    @GET
    @Path("runtime")
    public Response causeRuntimeException() {
        throw new RuntimeException("Test exception");
    }

    @GET
    @Path("repository")
    public Response causeRepositoryException() throws RepositoryException {
        throw new RepositoryException("Test exception");
    }

    @SuppressWarnings("serial")
    @GET
    @Path("client")
    public Response causeClientException() throws RepositoryClientException {
        throw new RepositoryClientException("Test exception") {
            @Override
            public Status getResponseStatus() {
                return Status.INTERNAL_SERVER_ERROR;
            }
        };
    }
}
