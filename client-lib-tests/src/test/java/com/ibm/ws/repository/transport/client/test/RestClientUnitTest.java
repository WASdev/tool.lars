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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mockit.Deencapsulation;
import mockit.Expectations;

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

}
