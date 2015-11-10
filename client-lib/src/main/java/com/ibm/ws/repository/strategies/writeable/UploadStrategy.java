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
package com.ibm.ws.repository.strategies.writeable;

import java.util.List;

import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceValidationException;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;

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
    public void uploadAsset(RepositoryResourceImpl resource, List<RepositoryResourceImpl> matchingResources)
                    throws RepositoryBackendException, RepositoryResourceException;

    public List<RepositoryResourceImpl> findMatchingResources(RepositoryResourceImpl resource) throws RepositoryResourceValidationException,
                    RepositoryBackendException, RepositoryBadDataException;

    /**
     * Whether to check the editions on upload. The base strategy sets this to true which should be used, however in certain scenarios this
     * can be overridden.
     * 
     * @return
     */
    public boolean performEditionChecking();
}
