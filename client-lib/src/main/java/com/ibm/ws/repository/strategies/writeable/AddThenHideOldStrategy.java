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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.FilterPredicate;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceUpdateException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;
import com.ibm.ws.repository.resources.writeable.WebDisplayable;

public class AddThenHideOldStrategy extends AddThenDeleteStrategy {

    /**
     * Delegate to super class for states
     */
    public AddThenHideOldStrategy() {}

    /**
     * Sets the desired state of the asset after uploading it
     *
     * @param desiredStateIfMatchingFound This is not used by this strategy but can be used by derived strategies
     * @param desiredStateIfNoMatchingFound Set the resource to this state after uploading. This behaviour can
     *            be changed by derived classes
     */
    public AddThenHideOldStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound) {
        super(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound, false);
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAsset(RepositoryResourceImpl resource, List<RepositoryResourceImpl> matchingResources) throws RepositoryBackendException, RepositoryResourceException {

        // Add the asset
        super.uploadAsset(resource, matchingResources);

        if (isVisibleAndWebDisplayable(resource) && State.PUBLISHED.equals(resource.getState())) {
            hideMatchingResources(resource);
        }
    }

    /**
     * This method should hide any asset which:
     * - has the same vanity URL as newResource
     * - is visible
     * - is published
     * - is not newResource
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    private void hideMatchingResources(RepositoryResourceImpl newResource) throws RepositoryBackendException, RepositoryResourceException {
        String vanityURL = newResource.getVanityURL();
        RepositoryConnection repo = newResource.getRepositoryConnection();
        Collection<RepositoryResource> matchingResources =
                        repo.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.VANITY_URL, vanityURL));

        List<String> hiddenAssets = new ArrayList<String>();
        for (RepositoryResource resource : matchingResources) {
            // Only hide visible, published resources, and don't hide the one that's just been created!
            if (newResource.getId().equals(resource.getId())) {
                continue;
            }
            if (!isVisibleAndWebDisplayable(resource)) {
                continue;
            }
            State stateOfResourceToBeHidden = ((RepositoryResourceWritable) resource).getState();
            if (!stateOfResourceToBeHidden.equals(State.PUBLISHED)) {
                continue;
            }

            // This is safe, due to the isVisibleAndWebDisplayable check above
            ((WebDisplayable) resource).setWebDisplayPolicy(DisplayPolicy.HIDDEN);
            // The desired stated is passed explicitly here, just in case a previous, failed, upload attempt
            // has left a matching resource in the wrong state.
            // There should always be a matching resource, so the desiredStateIfNoMatchingFound (second parameter) should never be used.
            ((RepositoryResourceWritable) resource).uploadToMassive(new AddThenDeleteStrategy(stateOfResourceToBeHidden, State.DRAFT, true));
            hiddenAssets.add(resource.getId());
        }

        if (hiddenAssets.size() > 1) {
            // Multiple resources were hidden. This should not happen, so the repository was in a bad state
            StringBuilder ids = new StringBuilder();
            for (String id : hiddenAssets) {
                ids.append(id + ", ");
            }
            throw new RepositoryResourceUpdateException("There was more than 1 resource with the vanity URL " + newResource.getVanityURL() +
                                                        " in state visible. New resource is " + newResource.getName() + "(" + newResource.getId() +
                                                        ") and old asset ids were " + ids.toString(), newResource.getVanityURL());
        }

    }

    /**
     * Returns <code>true</code> if the resource will be visible to the website.
     *
     * @param resource
     * @return
     */
    private boolean isVisibleAndWebDisplayable(RepositoryResource resource) {
        if (resource instanceof WebDisplayable) {
            DisplayPolicy displayPolicy = ((WebDisplayable) resource).getWebDisplayPolicy();
            return displayPolicy == DisplayPolicy.VISIBLE || displayPolicy == null;
        } else {
            return false;
        }
    }

}
