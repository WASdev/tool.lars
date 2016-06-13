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

import java.util.Collections;
import java.util.List;

import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceUpdateException;
import com.ibm.ws.repository.exceptions.RepositoryResourceValidationException;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;

/**
 * Perform a direct update of an asset, ignoring any attachments.
 * <p>
 * Requires the server to support updates.
 */
public class AssetOnlyReplacementStrategy extends BaseStrategy {

    private RepositoryResourceImpl _matchingResource;
    private boolean _forceReplace = false;

    public AssetOnlyReplacementStrategy() {}

    /**
     * @param matching Use this resource to replace instead of trying to find one
     * @param forceReplace Force update regardless of whether the strategy thinks an update is needed
     */
    public AssetOnlyReplacementStrategy(RepositoryResourceImpl matching, boolean forceReplace) {
        _matchingResource = matching;
        _forceReplace = forceReplace;
    }

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    @Override
    public void uploadAsset(RepositoryResourceImpl resource, List<RepositoryResourceImpl> matchingResources)
                    throws RepositoryBackendException, RepositoryResourceException {
        RepositoryResourceImpl firstMatch = (matchingResources == null || matchingResources.isEmpty()) ? null : matchingResources.get(0);
        if (firstMatch == null) {
            throw new RepositoryResourceUpdateException("No matching resource found when one should have been for " + resource.getName(), null);
        }

        if (_forceReplace) {
            update(resource, firstMatch);
        } else {
            // Is this an update or an add?
            switch (resource.updateRequired(firstMatch)) {
                case UPDATE:
                    update(resource, firstMatch);
                    break;
                case NOTHING:
                    // We are updating an existing asset .. we can reach here if no update is required.
                    // We have already read the asset from massive so no copy is required.
                    break;
                default:
                    // do nothing .. findbugs wants this
                    break;
            }
        }
    }

    /**
     * Update the asset (but not attachments)
     *
     * @param resource The new resource to use
     * @param firstMatch The asset to replace
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    private void update(RepositoryResourceImpl resource, RepositoryResourceImpl firstMatch) throws RepositoryBackendException, RepositoryResourceException {
        State initialState = firstMatch.getState();
        // We need to unpublish in order to overwrite
        if (firstMatch.getState() == State.PUBLISHED) {
            firstMatch.unpublish();
        }

        // Partial updates don't work so copy our data into the one
        // we found in massive and then set our asset to point to that
        // merged asset
        resource.overWriteAssetData(firstMatch, true);
        resource.updateAsset();
        // Read back from massive to get the fields massive generated into our resource
        resource.refreshFromMassive();

        resource.moveToState(initialState);
    }

    @Override
    public List<RepositoryResourceImpl> findMatchingResources(RepositoryResourceImpl resource) throws RepositoryResourceValidationException,
                    RepositoryBackendException, RepositoryBadDataException {
        if (_matchingResource != null) {
            return Collections.singletonList(_matchingResource);
        } else {
            return resource.findMatchingResource();
        }
    }

}
