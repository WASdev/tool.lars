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
package com.ibm.ws.lars.rest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * Servlet to gracefully handle servlet and other 500 errors which are propagated to the web
 * container.
 *
 * Note: Exceptions which are propagated to JAX-RS will be handled by RepositoryExceptionMapper
 * before they reach the web container.
 */
@SuppressWarnings("serial")
@WebServlet("/error")
public class ErrorHandler extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // copy the status code from the request, default to 500 if not set
        Integer statusCode = (Integer)request.getAttribute("javax.servlet.error.status_code");
        int status = (statusCode == null)?500:statusCode.intValue();

        // copy the message from the request, but override it for error codes 500 and 404
        String message = (String)request.getAttribute("javax.servlet.error.message");        
        if(message == null || status == 500) {
            message = "Internal server error, please contact the server administrator";
        } else if(status == 404) {
            message = "Not found";
        }

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON);
        PrintWriter printWriter = response.getWriter();
        JsonGenerator frontPageJsonGenerator = new JsonFactory().createGenerator(printWriter);
        frontPageJsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());

        frontPageJsonGenerator.writeStartObject();
        frontPageJsonGenerator.writeStringField("message", message);
        frontPageJsonGenerator.writeNumberField("statusCode", status);
        frontPageJsonGenerator.writeEndObject();

        frontPageJsonGenerator.flush();
        frontPageJsonGenerator.close();
    }

}
