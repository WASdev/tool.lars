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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;

/**
 * Test the https redirect in the bluemix web.xml
 */
public class BluemixTest {

    @Rule
    public final RepositoryContext repository;

    public BluemixTest() {
        this.repository = new RepositoryContext(FatUtils.BLUEMIX_HTTP_URL,
                null, null, false, false);
    }

    /**
     * Test that an http request gets a 302 response with a redirect to the equivalent https
     * address. hence needs to examine the guts of the response object
     *
     * @throws IOException
     * @throws ClientProtocolException
     */
    @Test
    public void testRedirect() throws ClientProtocolException, IOException {
        HttpResponse resp = repository.doRawGet("");
        StatusLine statusLine = resp.getStatusLine();
        int actualStatusCode = statusLine.getStatusCode();
        assertEquals("The http response code was incorrect", 302, actualStatusCode);
        Header[] headers = resp.getHeaders("Location");
        assertEquals("The number of location headers was wrong.", 1, headers.length);
        assertEquals("The redirect in the location header is pointing to the wrong place",
                     FatUtils.BLUEMIX_HTTPS_URL, headers[0].getValue());
    }

}
