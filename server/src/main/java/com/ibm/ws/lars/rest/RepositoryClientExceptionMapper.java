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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.ws.lars.rest.exceptions.RepositoryClientException;

/**
 *
 */
@Provider
public class RepositoryClientExceptionMapper implements ExceptionMapper<RepositoryClientException> {
    @Override
    public Response toResponse(RepositoryClientException e) {
        return Response.status(e.getResponseStatus())
                .entity(getErrorJson(e.getResponseStatus(), e.getMessage()))
                .build();
    }

    // Used for the error json below
    private static ObjectMapper errorMapper = new ObjectMapper();

    /**
     * Produce a JSON string with an error message, hopefully matching the same standard as what
     * comes out of Massive. Except without the stack trace for the moment.
     */
    static String getErrorJson(Response.Status status, String message) {
        Map<String, Object> errorMap = new HashMap<String, Object>();
        errorMap.put("statusCode", status.getStatusCode());
        errorMap.put("message", message);
        String error;
        try {
            error = errorMapper.writeValueAsString(errorMap);
        } catch (JsonProcessingException e) {
            // Ooer missus, this really shouldn't happen
            throw new WebApplicationException(e);
        }
        return error;
    }
}
