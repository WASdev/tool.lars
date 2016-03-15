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
import java.util.Collection;
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
import com.ibm.ws.lars.rest.SortOptions.SortOrder;
import com.ibm.ws.lars.rest.exceptions.InvalidParameterException;
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
    private static final String SORT_ORDER_PARAM = "sortOrder";
    private static final String SORT_BY_PARAM = "sortBy";

    // Permitted values for the SORT_BY parameter
    private static final String SORT_BY_ASC = "ASC";
    private static final String SORT_BY_DESC = "DESC";

    private static final Set<String> NON_QUERY_PARAMS = new HashSet<>(
            Arrays.asList(LIMIT_PARAM, OFFSET_PARAM, FIELDS_PARAM, APIKEY_PARAM, SEARCH_PARAM, SORT_ORDER_PARAM, SORT_BY_PARAM));

    private AssetQueryParameters(Map<String, String> params) {
        this.params = params;
    }

    /**
     * Parse the asset query parameters from a UriInfo
     *
     * @param uriInfo the UriInfo
     * @return the asset query parameters
     */
    public static AssetQueryParameters create(UriInfo uriInfo) {

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
     * Returns the filters parsed from the request as a list of AssetFilter.
     * <p>
     * If there were no filter parameters passed with the request then the returned list will be
     * empty
     * 
     * If a single field name appeared twice or more in the query string, the returned list will
     * only contain one filter for that field, in a single AssetFilter instance. The filter in the
     * returned list will represent the last filter from the query string.
     *
     * @return a list of AssetFilter
     */
    public Collection<AssetFilter> getFilters() {
        // process parameters as filters
        // Filters have the following syntax
        // field=value[|value]...

        // To ensure there is only one filter per field, add
        // them to a keyed map. Convert to a list later
        Map<String, AssetFilter> filterMap = new HashMap<>();
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

            filterMap.put(entry.getKey(), new AssetFilter(entry.getKey(), conditions));
        }

        List<AssetFilter> assetFilters = new ArrayList<>();
        assetFilters.addAll(filterMap.values());
        return assetFilters;
    }

    /**
     * Parses the limit and offset parameters to create and return a PaginationOptions.
     * <p>
     * If both parameters are present and are integers, a PaginationOptions object will be returned.
     * <p>
     * If only one or neither parameters are present, null will be returned. Pagination is only
     * enabled if both options are provided.
     * <p>
     * If both parameters are present but are not both integers, an InvalidParameterException is
     * thrown
     *
     * @return a PaginationOptions if both limit and offset parameters are provided, otherwise null
     * @throws InvalidParameterException if limit and offset parameters are provided but are not
     *             integers
     */
    public PaginationOptions getPagination() throws InvalidParameterException {
        String limitString = params.get(LIMIT_PARAM);
        String offsetString = params.get(OFFSET_PARAM);

        if (limitString == null && offsetString == null) {
            return null;
        }

        if (limitString == null || offsetString == null) {
            throw new InvalidParameterException("If either " + LIMIT_PARAM + " or " + OFFSET_PARAM + " is provided then both must be provided");
        }

        int limit;
        int offset;

        try {
            limit = Integer.parseInt(limitString);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(LIMIT_PARAM + " must be an integer");
        }

        try {
            offset = Integer.parseInt(offsetString);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(OFFSET_PARAM + " must be an integer");
        }

        return new PaginationOptions(offset, limit);
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

    /**
     * @return SortOptions describing how the results should be sorted or null if the results should
     *         not be sorted
     */
    public SortOptions getSortOptions() throws InvalidParameterException {
        String sortByField = params.get(SORT_BY_PARAM);
        String sortOrderParam = params.get(SORT_ORDER_PARAM);
        SortOrder sortOrder;

        if (sortByField != null && sortByField.isEmpty()) {
            throw new InvalidParameterException(SORT_BY_PARAM + " must not be blank");
        }

        if (sortOrderParam != null && sortByField == null) {
            throw new InvalidParameterException(SORT_ORDER_PARAM + " may only be provided if " + SORT_BY_PARAM + " is also provided");
        }

        if (sortOrderParam == null) {
            sortOrder = SortOrder.ASCENDING;
        } else if (sortOrderParam.equalsIgnoreCase(SORT_BY_ASC)) {
            sortOrder = SortOrder.ASCENDING;
        } else if (sortOrderParam.equalsIgnoreCase(SORT_BY_DESC)) {
            sortOrder = SortOrder.DESCENDING;
        } else {
            throw new InvalidParameterException(SORT_ORDER_PARAM + " must be either \"" + SORT_BY_ASC + "\" or \"" + SORT_BY_DESC + "\"");
        }

        if (sortByField != null) {
            return new SortOptions(sortByField, sortOrder);
        } else {
            return null;
        }
    }

}
