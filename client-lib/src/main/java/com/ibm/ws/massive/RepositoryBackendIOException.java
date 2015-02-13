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

package com.ibm.ws.massive;

public class RepositoryBackendIOException extends RepositoryBackendException {

    private static final long serialVersionUID = -5918785594985895160L;

    public RepositoryBackendIOException() {
        super();
    }

    public RepositoryBackendIOException(String message) {
        super(message);
    }

    public RepositoryBackendIOException(Throwable cause) {
        super(cause);
    }

    public RepositoryBackendIOException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }

}
