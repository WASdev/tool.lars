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

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceUpdateException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.writeable.WebDisplayable;

/**
 *
 */
public class AddThenHideOldStrategy extends AddThenDeleteStrategy {

    private static Hashtable<String, RepositoryResourceImpl> allResources = new Hashtable<String, RepositoryResourceImpl>();

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

        synchronized (allResources) {
            if (allResources.size() == 0) {
                refreshCache(resource.getRepositoryConnection(), null);
            }
        }

        // Add the asset
        super.uploadAsset(resource, matchingResources);

        if (isVisible(resource) && State.PUBLISHED.equals(resource.getState())) {
            hideAsset(resource, matchingResources);
            synchronized (allResources) {
                allResources.put(resource.getVanityURL(), resource);
            }
        }
    }

    /**
     * Refresh the cache
     *
     * @param loginInfo
     * @param ignore A resource with this ID will not be added to the cache. Useful if refreshing the cache after adding a new resource with the same vanity URL as an existing
     *            attachment.
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     * @throws RepositoryResourceUpdateException if there are two assets with the same vanity URL
     */
    private void refreshCache(RepositoryConnection repoConnection, String ignore) throws RepositoryBackendException, RepositoryResourceException, RepositoryResourceUpdateException {
        allResources.clear();

        /*
         * If the population fails we throw an exception that could leave a half populated map and if we have wrapped this call in retry logic we'll be in trouble... Therefore
         * populate a separate map and copy to the member variable *only* when complete
         */
        Hashtable<String, RepositoryResourceImpl> vanityUrlToResources = new Hashtable<String, RepositoryResourceImpl>();
        @SuppressWarnings("unchecked")
        Collection<RepositoryResourceImpl> allRes = (Collection<RepositoryResourceImpl>) new RepositoryConnectionList(repoConnection).getAllResourcesWithDupes();
        for (RepositoryResourceImpl res : allRes) {
            // See if it is visible AND published, if it isn't published then it won't be visible on the website
            if (isVisible(res) && State.PUBLISHED.equals(res.getState()) && !res.getId().equals(ignore)) {

                // Read attachments in now - so we don't reread from massive
                res.refreshFromMassive();
                RepositoryResourceImpl previousRes = vanityUrlToResources.put(res.getVanityURL(), res);
                if (previousRes != null) {
                    throw new RepositoryResourceUpdateException("There is more than 1 resource with the vanity URL " + res.getVanityURL() +
                                                                " in state visible, " + res.getName() + "(" + res.getId() + ") and " +
                                                                previousRes.getName() + "(" + previousRes.getId() + ")", res.getId());
                }
            }
        }

        allResources.putAll(vanityUrlToResources);
    }

    /**
     * This method will update the visible resource that uses the supplied vanity URL and mark it hidden
     *
     * @param vanityURL
     * @param matchingResources The resources that were matched to this one for working out if it should be updated
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    public void hideAsset(RepositoryResourceImpl newResource, List<RepositoryResourceImpl> matchingResources) throws RepositoryBackendException, RepositoryResourceException {
        String vanityURL = newResource.getVanityURL();
        RepositoryResourceImpl resource = allResources.get(vanityURL);

        if (resource instanceof WebDisplayable) {
            // All the matching resources should have been deleted so we won't be able to hide them, make sure we aren't trying to update a hidden resource
            boolean oldResourceDeleted = false;
            for (RepositoryResourceImpl massiveResource : matchingResources) {
                if (resource.equivalent(massiveResource) || resource.getId().equals(massiveResource.getId())) {
                    oldResourceDeleted = true;
                    break;
                }
            }
            if (!oldResourceDeleted) {
                // Since resource object is the resource that is being replaced, we need to get a copy of the
                // resource before it gets updated and we can then delete that copy (the 4th parameter
                // passed to the AdddThenDeleteStrategy constructor). There is a chance that the resource will
                // have been deleted since we setup the cache so do this in a try catch.
                RepositoryResource resourceToDelete;
                try {
                    resourceToDelete = resource.getRepositoryConnection().getResource(resource.getId());
                } catch (RepositoryException e) {
                    // Refresh the cache
                    synchronized (allResources) {
                        refreshCache(resource.getRepositoryConnection(), newResource.getId());
                        resource = allResources.get(vanityURL);
                        if (!(resource instanceof WebDisplayable)) {
                            // Nothing matches any more so exit
                            return;
                        }
                        resourceToDelete = resource.getRepositoryConnection().getResource(resource.getId());
                    }
                }
                ((WebDisplayable) resource).setWebDisplayPolicy(DisplayPolicy.HIDDEN);
                resource.uploadToMassive(new AddThenDeleteStrategy(null, State.DRAFT, true, (RepositoryResourceImpl) resourceToDelete));
            }
        }
    }

    /**
     * Returns <code>true</code> if the resource will be visible to the website.
     *
     * @param resource
     * @return
     */
    private boolean isVisible(RepositoryResourceImpl resource) {
        DisplayPolicy displayPolicy = null;
        if (resource instanceof WebDisplayable) {
            displayPolicy = ((WebDisplayable) resource).getWebDisplayPolicy();
        }
        return displayPolicy == DisplayPolicy.VISIBLE || displayPolicy == null;
    }

    public static void clearCache() {
        allResources = new Hashtable<String, RepositoryResourceImpl>();
    }
}
