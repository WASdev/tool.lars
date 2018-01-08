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
package com.ibm.ws.repository.transport.client.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.transport.client.ClientLoginInfo;
import com.ibm.ws.repository.transport.client.RestClient;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

public class RestClientUnitTest {

    /**
     * The test verifies that empty filters are ignored, i.e. they are not included in the query
     * url passed to the repository.
     *
     */
    @Test
    public void testEmptyFiltersAreIgnored() throws IOException, RequestFailureException {

        ClientLoginInfo info = new ClientLoginInfo("noone", "letmein", "123", "http://broken");

        /*
         * This is voodoo.... Doing this without a repository involves mocking the connection, but
         * it turns out that mocking HttpURLConnection is really hard. Instead, partially mock the client
         * object. The method to be tested (i.e. getFilteredAssets) is not mocked, but the private method
         * which returns the HTTP connection is.
         */
        final RestClient client = new RestClient(info);
        new Expectations(client) {
            {
                Deencapsulation.invoke(client, "createHttpURLConnectionToMassive", "/assets?type=com.ibm.websphere.Feature");
                // now that the correct query string has been constructed, stop the test
                // Otherwise the test will try to connect to a duff url
                result = new NullPointerException("This might just work");
            }

        };

        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();
        filters.put(FilterableAttribute.TYPE, Collections.singleton(ResourceType.FEATURE.getValue()));
        filters.put(FilterableAttribute.LOWER_CASE_SHORT_NAME, Collections.<String> emptySet());

        try {
            client.getFilteredAssets(filters);
        } catch (NullPointerException e) {
            if (!e.getMessage().equals("This might just work")) {
                throw e;
            }
        }

    }

    /**
     * Check that special characters in filters get encoded correctly
     */
    @Test
    public void testFilterEscaping() throws IOException, RequestFailureException {

        ClientLoginInfo info = new ClientLoginInfo("noone", "letmein", "123", "http://broken");

        final RestClient client = new RestClient(info);
        new Expectations(client) {
            {
                // Ensure the @, %, +, | and space characters are escaped
                Deencapsulation.invoke(client, "createHttpURLConnectionToMassive", "/assets?type=Foo%40%25%7CBar+%2B+Baz");
                // now that the correct query string has been constructed, stop the test
                // Otherwise the test will try to connect to a duff url
                result = new RuntimeException("OK");
            }
        };

        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();
        filters.put(FilterableAttribute.TYPE, Arrays.asList("Foo@%", "Bar + Baz"));

        try {
            client.getFilteredAssets(filters);
        } catch (RuntimeException e) {
            if (!e.getMessage().equals("OK")) {
                throw e;
            }
        }
    }

    @Test
    public void testGetStatusNoHeader(final @Mocked HttpURLConnection connection) throws IOException, RequestFailureException {
        try {
            executeGetStatus(connection, null);
            fail("An exception should have been thrown if there was no header");
        } catch (RequestFailureException e) {
            if (!e.getMessage().contains("No header returned")) {
                throw e;
            }
        }
    }

    @Test
    public void testGetStatusNoCount(final @Mocked HttpURLConnection connection) throws IOException, RequestFailureException {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        List<String> wibbleValues = new ArrayList<String>();
        wibbleValues.add("value1");
        wibbleValues.add("value2");
        headers.put("Wibble", wibbleValues);

        try {
            executeGetStatus(connection, headers);
            fail("An exception should have been thrown if there was no count in the header");
        } catch (RequestFailureException e) {
            if (!e.getMessage().contains("No count returned")) {
                throw e;
            }
        }
    }

    @Test
    public void testGetStatusWithCount(final @Mocked HttpURLConnection connection) throws IOException, RequestFailureException {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        List<String> wibbleValues = new ArrayList<String>();
        wibbleValues.add("value1");
        wibbleValues.add("value2");
        headers.put("Wibble", wibbleValues);

        List<String> countValues = new ArrayList<String>();
        countValues.add("1234");
        headers.put("count", countValues);

        executeGetStatus(connection, headers);
    }

    private void executeGetStatus(final HttpURLConnection connection, final Map<String, List<String>> headerFields) throws IOException, RequestFailureException {
        ClientLoginInfo info = new ClientLoginInfo("noone", "letmein", "123", "http://broken");

        final RestClient client = new RestClient(info);

        new Expectations(client) {
            {
                Deencapsulation.invoke(client, "createHeadConnection", "/assets");
                result = connection;

                Deencapsulation.invoke(client, "testResponseCode", connection);
                result = null;

                connection.getHeaderFields();
                result = headerFields;
            }
        };

        client.checkRepositoryStatus();
    }

}
