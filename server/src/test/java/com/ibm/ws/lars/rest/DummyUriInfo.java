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

    /**
     * Create a UriInfo with the given base URI
     */
    public DummyUriInfo(URI baseUri) {
        this.baseUri = baseUri;
    }

    /** {@inheritDoc} */
    @Override
    public URI getBaseUri() {
        return baseUri;
    }

    /** {@inheritDoc} */
    @Override
    public URI getAbsolutePath() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public UriBuilder getBaseUriBuilder() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<Object> getMatchedResources() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getMatchedURIs() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getMatchedURIs(boolean arg0) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getPath(boolean arg0) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean arg0) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<PathSegment> getPathSegments() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<PathSegment> getPathSegments(boolean arg0) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean arg0) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public URI getRequestUri() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public UriBuilder getRequestUriBuilder() {
        return null;
    }

    public URI relativize(URI uri) {
        return null;
    }

    public URI resolve(URI uri) {
        return null;
    }

}
