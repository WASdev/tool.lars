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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import com.ibm.ws.lars.rest.Condition.Operation;
import com.ibm.ws.lars.rest.exceptions.RepositoryException;

/**
 * Parses query parameters for REST calls which query for assets.
 */
public class AssetQueryParameters {

    private final Map<String, String> params;

    private static final String LIMIT_PARAM = "limit";
    private static final String OFFSET_PARAM = "offset";
    private static final String FIELDS_PARAM = "fields";
    private static final String APIKEY_PARAM = "apiKey";
    private static final String SEARCH_PARAM = "q";

    private static final Set<String> NON_QUERY_PARAMS = new HashSet<>(
            Arrays.asList(LIMIT_PARAM, OFFSET_PARAM, FIELDS_PARAM, APIKEY_PARAM, SEARCH_PARAM));

    private AssetQueryParameters(Map<String, String> params) {
        this.params = params;
    }

    /**
     * Parse the asset query parameters from a UriInfo
     *
     * @param uriInfo the UriInfo
     * @return the asset query parameters
     */
    public static AssetQueryParameters parse(UriInfo uriInfo) {

        MultivaluedMap<String, String> params = uriInfo.getQueryParameters(false);

        // NOTE: we're putting a multi-valued map into a regular map, only the last value for each key will be preserved
        // If the query parameters includes foo=bar&foo=baz, the decodedParams map will only contain foo=baz
        // This is the desired behaviour for parsing the parameters for an asset query
        Map<String, String> decodedParams = new HashMap<String, String>();

        try {
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                String decodedKey = URLDecoder.decode(entry.getKey(), StandardCharsets.UTF_8.name());
                for (String value : entry.getValue()) {
                    decodedParams.put(decodedKey, URLDecoder.decode(value, StandardCharsets.UTF_8.name()));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("UTF-8 is unexpectedly missing.", e);
        }

        return new AssetQueryParameters(decodedParams);
    }

    /**
     * Returns the filters parsed from the request in a map (parameter name -> conditions)
     * <p>
     * If there were no filter parameters passed with the request then the returned map will be
     * empty
     *
     * @return a map from parameter name to condition
     */
    public Map<String, List<Condition>> getFilterMap() {
        // process parameters as filters
        // Filters have the following syntax
        // field=value[|value]...

        Map<String, List<Condition>> filterMap = new HashMap<>();
        for (Entry<String, String> entry : params.entrySet()) {

            // Skip any parameters which have a special meaning
            if (NON_QUERY_PARAMS.contains(entry.getKey())) {
                continue;
            }

            String value = entry.getValue();

            // Pipe (|) separates values which should be ORed together
            // split(<regex>, -1) is used to retain any trailing empty strings, ensuring that we always have a non-empty list
            List<String> orParts = new ArrayList<String>(Arrays.asList(value.split("\\|", -1)));

            List<Condition> conditions = new ArrayList<>();

            // The first value can begin with ! to indicate that a filter for NOT that value
            if (orParts.get(0).startsWith("!")) {
                conditions.add(new Condition(Operation.NOT_EQUALS, orParts.get(0).substring(1)));
                orParts.remove(0);
            }

            // Any later values beginning with ! are ignored
            for (Iterator<String> iterator = orParts.iterator(); iterator.hasNext();) {
                if (iterator.next().startsWith("!")) {
                    iterator.remove();
                }
            }

            // Finally all remaining values represent an equals condition
            if (!orParts.isEmpty()) {
                for (String part : orParts) {
                    conditions.add(new Condition(Operation.EQUALS, part));
                }
            }

            filterMap.put(entry.getKey(), conditions);
        }

        return filterMap;
    }

    /**
     * @return the search term parameter, or null if it was not set or is blank
     */
    public String getSearchTerm() {
        String searchTerm = params.get(SEARCH_PARAM);

        if (searchTerm == null || searchTerm.isEmpty()) {
            return null;
        } else {
            return searchTerm;
        }
    }

    /**
     * @return the fields param
     */
    public String getFields() {
        return params.get(FIELDS_PARAM);
    }

}
