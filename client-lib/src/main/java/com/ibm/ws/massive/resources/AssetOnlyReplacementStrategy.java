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
import com.ibm.ws.massive.resources.MassiveResource.State;

/**
 *
 */
public class AssetOnlyReplacementStrategy extends BaseStrategy {

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    @Override
    public void uploadAsset(MassiveResource resource, List<MassiveResource> matchingResources)
                    throws RepositoryBackendException, RepositoryResourceException {
        MassiveResource firstMatch = (matchingResources == null || matchingResources.isEmpty()) ? null : matchingResources.get(0);
        if (firstMatch == null) {
            throw new RepositoryResourceUpdateException("No matching resource found when one should have been for " + resource.getName(), null);
        }

        State initialState = firstMatch.getState();

        // We need to unpublish in order to overwrite
        if (firstMatch.getState() == State.PUBLISHED) {
            firstMatch.unpublish();
        }

        // Is this an update or an add?
        switch (resource.updateRequired(firstMatch)) {
            case UPDATE:
                // Partial updates don't work so copy our data into the one
                // we found in massive and then set our asset to point to that
                // merged asset
                overWriteAssetData(resource, firstMatch, true);
                updateAsset(resource);
                // Read back from massive to get the fields massive generated into our resource
                resource.refreshFromMassive();
                break;
            case NOTHING:
                // We are updating an existing asset .. we can reach here if no update is required.
                // We have already read the asset from massive so no copy is required.
                break;
            default:
                // do nothing .. findbugs wants this
                break;
        }
        resource.moveToState(initialState);
    }
}
