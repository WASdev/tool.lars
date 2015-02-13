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

import java.util.List;

import com.ibm.ws.massive.RepositoryBackendException;

/**
 *
 */
public interface UploadStrategy {

    public static final UploadStrategy DEFAULT_STRATEGY = new AddThenDeleteStrategy();

    /**
     * Upload the resource using an implementation of this interface
     *
     * @param resource
     * @param matchingResource
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    public void uploadAsset(MassiveResource resource, List<MassiveResource> matchingResources)
                    throws RepositoryBackendException, RepositoryResourceException;

    public List<MassiveResource> findMatchingResources(MassiveResource resource) throws RepositoryResourceValidationException,
                    RepositoryBackendException, RepositoryBadDataException;
}
