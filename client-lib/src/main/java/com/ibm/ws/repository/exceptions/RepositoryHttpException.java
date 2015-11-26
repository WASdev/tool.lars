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

package com.ibm.ws.repository.exceptions;

import com.ibm.ws.repository.connections.RepositoryConnection;

public class RepositoryHttpException extends RepositoryBackendIOException {

    private static final long serialVersionUID = 7084745358560323741L;
    private final int _httpRespCode;

    public RepositoryHttpException(String message, int httpRespCode, RepositoryConnection connection) {
        super(message, connection);
        _httpRespCode = httpRespCode;
    }

    /**
     * @return the _httpRespCode
     */
    public int get_httpRespCode() {
        return _httpRespCode;
    }

}
