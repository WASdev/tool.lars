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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This front page exists as a simple IVT and to ensure there is a valid page at the server root to
 * help monitoring tools.
 */
@SuppressWarnings("serial")
@WebServlet("")
public class FrontPage extends HttpServlet {

    @Inject
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "CDI normal scoped injected field")
    private AssetServiceLayer serviceLayer;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType(MediaType.TEXT_HTML);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        int count = serviceLayer.countAllAssets(Collections.<AssetFilter> emptyList(), null);

        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>LARS is running</title></head>");
        out.println("<body>");
        out.println("<h1>LARS is running</h1>");
        out.println("<p>The repository is running with " + count + " assets</p>");
        out.println("</body>");
        out.println("</html>");
    }

}
