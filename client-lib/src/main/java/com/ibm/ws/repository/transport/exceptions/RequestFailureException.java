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
package com.ibm.ws.repository.transport.exceptions;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This exception is thrown when there was an error in the HTTP request to the
 * Massive repository that lead to a response code that was not successful
 * (2xx). The response code is available on {@link #getResponseCode()} and the
 * message from the error stream on the HTTP URL connection is available via {@link #getErrorMessage()} with the full contents of the error stream being
 * available through {@link #getErrorStreamContents()}.
 */
public class RequestFailureException extends Exception {

    private static final long serialVersionUID = -2336710508495285178L;
    private final int responseCode;
    private final String errorMessage;
    private final String errorStreamContents;

    public RequestFailureException(int responseCode, String errorMessage, URL url, String errorStreamMessage) {
        super("Server returned HTTP response code: " + responseCode + " for URL: " + url.toString() + " error message: \"" + errorMessage + "\"");
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
        this.errorStreamContents = errorStreamMessage;
    }

    /**
     * Returns the response code from the HTTP URL connection when connecting to the repository backend.
     *
     * @return The response code
     * @see HttpURLConnection#getResponseCode()
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Returns the error message written to the error stream on the HTTP URL connection when connecting to the repository backend.
     *
     * @return The error message
     * @see HttpURLConnection#getErrorStream()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the full contents of the error stream.
     *
     * @return The contents of the error stream
     * @see HttpURLConnection#getErrorStream()
     */
    public String getErrorStreamContents() {
        return errorStreamContents;
    }

}
