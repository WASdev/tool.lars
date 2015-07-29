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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.ibm.ws.lars.rest.exceptions.RepositoryException;

@Provider
public class RepositoryExceptionMapper implements ExceptionMapper<RepositoryException> {

    private static final Logger logger = Logger.getLogger(RepositoryExceptionMapper.class.getCanonicalName());

    @Override
    public Response toResponse(RepositoryException e) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe("Internal server error\n" + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.severe(sw.toString());
        }

        return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(RepositoryClientExceptionMapper.getErrorJson(Status.INTERNAL_SERVER_ERROR, "Internal server error, please contact the server administrator"))
                .build();
    }

}
