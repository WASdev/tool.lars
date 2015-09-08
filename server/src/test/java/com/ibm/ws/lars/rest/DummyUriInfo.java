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

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * A dummy UriInfo for unit testing
 * <p>
 * Only {@link #getBaseUri()} is implemented
 */
public class DummyUriInfo implements UriInfo {

    private final URI baseUri;
    private final URI appUri;
    private final URI fullUri;

    /**
     * Create a UriInfo with the given base URI
     */
    public DummyUriInfo(URI baseUri) {
        this.baseUri = baseUri;
        this.appUri = null;
        this.fullUri = baseUri;
    }

    /**
     * Create a UriInfo with the given base URI and app-relative URI
     *
     * @param baseUri the base URI of the application
     * @param appUri the URI requested relative to the base URI
     * @throws Exception
     */
    public DummyUriInfo(String baseUri, String appUri) throws Exception {
        this.baseUri = new URI(baseUri);
        this.appUri = new URI(appUri);

        if (!this.baseUri.isAbsolute()) {
            throw new Exception("Base URI must be absolute");
        }

        if (this.appUri.isAbsolute()) {
            throw new Exception("App URI must be relative");
        }

        this.fullUri = this.baseUri.resolve(this.appUri);
    }

    /** {@inheritDoc} */
    @Override
    public URI getBaseUri() {
        return baseUri;
    }

    /** {@inheritDoc} */
    @Override
    public URI getAbsolutePath() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public UriBuilder getAbsolutePathBuilder() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public UriBuilder getBaseUriBuilder() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public List<Object> getMatchedResources() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getMatchedURIs() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getMatchedURIs(boolean arg0) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public String getPath(boolean arg0) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean arg0) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public List<PathSegment> getPathSegments() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public List<PathSegment> getPathSegments(boolean arg0) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do a simplified parsing of query parameters, splitting strings on '&' and '='
     * <p>
     * Decoding of parameters is not supported.
     */
    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        if (decode == true) {
            throw new UnsupportedOperationException("Decoding of query parameters is not supported");
        }

        MultivaluedMap<String, String> result = new MultivaluedMapImpl<String, String>();

        String query = fullUri.getRawQuery();

        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", -1); //-1 => Do not trim trailing empty strings

            if (parts.length != 2) {
                throw new RuntimeException("Bad parameter: " + parameter);
            }

            result.add(parts[0], parts[1]);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public URI getRequestUri() {
        return fullUri;
    }

    /** {@inheritDoc} */
    @Override
    public UriBuilder getRequestUriBuilder() {
        throw new UnsupportedOperationException();
    }

}
