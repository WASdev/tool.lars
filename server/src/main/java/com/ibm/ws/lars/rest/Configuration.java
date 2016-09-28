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
import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

/**
 * Holds configuration options for the application
 */
@ApplicationScoped
public class Configuration {

    private final static String HTTP_SCHEME = "http";
    private final static int HTTP_PORT = 80;
    private final static String HTTPS_SCHEME = "https";
    private final static int HTTPS_PORT = 443;

    private final String urlBase;

    public Configuration() {
        String urlBase = null;
        try {
            String configString = (String) new InitialContext().lookup("lars/URLBase");
            urlBase = computeRestBaseUri(configString);
        } catch (NamingException e) {
            // lars/URLBase setting is optional
        }

        this.urlBase = urlBase;
    }

    /**
     * Returns the base URL of the REST application
     * <p>
     * This method should be used to compute any absolute URLs to resources within the REST
     * application.
     * <p>
     * This will be the same as uriInfo.getBaseUri, unless the user has configured a different URL.
     *
     * @param uriInfo the UriInfo to use to compute the base URL if the user has not overridden it
     * @return the base URL of the REST application
     */
    public String getRestBaseUri(UriInfo uriInfo) {
        if (urlBase != null) {
            return urlBase;
        } else {
            return stripDefaultPort(uriInfo.getBaseUri()).toString();
        }
    }

    /**
     * Given a URLBase that the user has provided, compute the corresponding BaseUri for the JAX-RS
     * application.
     * <p>
     * This is needed to maintain the same configuration behaviour after we moved the root of the
     * rest application.
     * <p>
     * If the user configures http://example.org/wibble, this method should return
     * http://example.org/wibble/ma/v1/
     *
     * @param configString the user-provided URLBase string
     * @return the base URL of the REST application
     */
    private static String computeRestBaseUri(String configString) {
        StringBuilder b = new StringBuilder(configString);
        if (!configString.endsWith("/")) {
            b.append("/");
        }
        b.append("ma/v1/");
        return b.toString();
    }

    /**
     * Removes the port from a URI, if it is an HTTP or HTTPS URI and explicitly specifies the
     * default port for that protocol.
     *
     * @param uri the URI
     * @return either {@code uri} with the port removed, or {@code uri} unchanged.
     */
    private static URI stripDefaultPort(URI uri) {
        if ((uri.getPort() == HTTP_PORT && HTTP_SCHEME.equalsIgnoreCase(uri.getScheme()))
            || (uri.getPort() == HTTPS_PORT && HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme()))) {
            try {
                // Copy uri but strip port
                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), -1, uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                // certainly should not happen
                throw new WebApplicationException(e);
            }
        }
        return uri;
    }
}
