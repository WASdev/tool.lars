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

package com.ibm.ws.lars.rest.model;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.ws.lars.rest.RepositoryClientException;

/**
 * Thrown when attempting to do an invalid state change on an asset
 */
public class RepositoryResourceLifecycleException extends RepositoryClientException {

    /**  */
    private static final long serialVersionUID = 1L;

    public RepositoryResourceLifecycleException(Asset.State state, Asset.StateAction action) {
        super("Invalid action " + action.getValue() + " performed on the asset with state " + state.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}