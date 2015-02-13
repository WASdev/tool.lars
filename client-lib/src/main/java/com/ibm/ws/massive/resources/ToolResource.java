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

package com.ibm.ws.massive.resources;

import java.util.Collection;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.RepositoryException;

public class ToolResource extends ProductRelatedResource {

    /*
     * ----------------------------------------------------------------------------------------------------
     * STATIC HELPER METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    /**
     *
     * @param userId
     * @param password
     * @param apiKey
     * @throws RepositoryBackendException
     */
    public static Collection<ToolResource> getAllTools(LoginInfo loginInfo) throws RepositoryBackendException {
        return MassiveResource.getAllResources(Type.TOOL, loginInfo);
    }

    /**
     * Get all products with a given license type
     *
     * @param License type
     * @param userId
     * @param password
     * @param apiKey
     * @throws RepositoryException
     */
    public static Collection<ToolResource> getAllTools(LicenseType licenseType, LoginInfo loginInfo) throws RepositoryBackendException {
        return MassiveResource.getAllResources(licenseType, Type.TOOL, loginInfo);
    }

    /**
     *
     * @param userId
     * @param password
     * @param apiKey
     * @param id
     * @return
     * @throws RepositoryBackendException
     */
    public static ToolResource getTool(LoginInfoEntry loginInfoResource, String id) throws RepositoryBackendException, RepositoryBadDataException {
        return getResource(loginInfoResource, id);
    }

    /*
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */
    public ToolResource(LoginInfoEntry loginInfoResource) {
        super(loginInfoResource);
        setType(Type.TOOL);
    }

}
