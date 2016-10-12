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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for deliberately causing errors to test error handling code
 */
@SuppressWarnings("serial")
@WebServlet("/ma/v1/provoke-error-servlet")
public class ErrorServlet extends HttpServlet {

    static final Logger LOGGER = Logger.getLogger(ErrorServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String errorType = req.getParameter("type");

        if (errorType == null) {
            throw new ServletException("Test Exception");
        } else if (errorType.equals("runtime")) {
            throw new RuntimeException("Test Exception");
        } else {
            try {
                int responseCode = Integer.parseInt(errorType);
                resp.sendError(responseCode, "Test Error");
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.SEVERE, "Invalid error type requested: " + errorType, ex);
            }
        }
    }
}
