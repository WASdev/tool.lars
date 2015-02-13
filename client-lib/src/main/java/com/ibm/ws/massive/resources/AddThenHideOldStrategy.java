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
import java.util.Hashtable;
import java.util.List;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.RepositoryException;
import com.ibm.ws.massive.resources.MassiveResource.DisplayPolicy;
import com.ibm.ws.massive.resources.MassiveResource.State;

/**
 *
 */
public class AddThenHideOldStrategy extends AddThenDeleteStrategy {

    private static Hashtable<String, MassiveResource> allResources = new Hashtable<String, MassiveResource>();

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
    public void uploadAsset(MassiveResource resource, List<MassiveResource> matchingResources) throws RepositoryBackendException, RepositoryResourceException {

        synchronized (allResources) {
            if (allResources.size() == 0) {
                refreshCache(resource.getLoginInfo(), null);
            }
        }

        // Add the asset
        super.uploadAsset(resource, matchingResources);

        if (getWebDisplayPolicy(resource) == DisplayPolicy.VISIBLE && State.PUBLISHED.equals(resource.getState())) {
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
    private void refreshCache(LoginInfoEntry loginInfoResource, String ignore) throws RepositoryBackendException, RepositoryResourceException, RepositoryResourceUpdateException {
        allResources.clear();

        /*
         * If the population fails we throw an exception that could leave a half populated map and if we have wrapped this call in retry logic we'll be in trouble... Therefore
         * populate a separate map and copy to the member variable *only* when complete
         */
        Hashtable<String, MassiveResource> vanityUrlToResources = new Hashtable<String, MassiveResource>();
        Collection<MassiveResource> allRes = MassiveResource.getAllResourcesWithDupes(new LoginInfo(loginInfoResource));
        for (MassiveResource res : allRes) {
            // See if it is visible AND published, if it isn't published then it won't be visible on the website
            if (getWebDisplayPolicy(res) == DisplayPolicy.VISIBLE && State.PUBLISHED.equals(res.getState()) && !res.get_id().equals(ignore)) {

                // Read attachments in now - so we don't reread from massive
                res.refreshFromMassive();
                MassiveResource previousRes = vanityUrlToResources.put(res.getVanityURL(), res);
                if (previousRes != null) {
                    throw new RepositoryResourceUpdateException("There is more than 1 resource with the vanity URL " + res.getVanityURL() +
                                                                " in state visible, " + res.getName() + "(" + res.get_id() + ") and " +
                                                                previousRes.getName() + "(" + previousRes.get_id() + ")", res.get_id());
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
    public void hideAsset(MassiveResource newResource, List<MassiveResource> matchingResources) throws RepositoryBackendException, RepositoryResourceException {
        String vanityURL = newResource.getVanityURL();
        MassiveResource resource = allResources.get(vanityURL);

        if (resource instanceof WebDisplayable) {
            // All the matching resources should have been deleted so we won't be able to hide them, make sure we aren't trying to update a hidden resource
            boolean oldResourceDeleted = false;
            for (MassiveResource massiveResource : matchingResources) {
                if (resource.equivalent(massiveResource) || resource.get_id().equals(massiveResource.get_id())) {
                    oldResourceDeleted = true;
                    break;
                }
            }
            if (!oldResourceDeleted) {
                // Since resource object is the resource that is being replaced, we need to get a copy of the
                // resource before it gets updated and we can then delete that copy (the 4th parameter
                // passed to the AdddThenDeleteStrategy constructor). There is a chance that the resource will
                // have been deleted since we setup the cache so do this in a try catch.
                MassiveResource resourceToDelete;
                try {
                    resourceToDelete = MassiveResource.getResource(resource.getLoginInfo(), resource.get_id());
                } catch (RepositoryException e) {
                    // Refresh the cache
                    synchronized (allResources) {
                        refreshCache(resource.getLoginInfo(), newResource.get_id());
                        resource = allResources.get(vanityURL);
                        if (!(resource instanceof WebDisplayable)) {
                            // Nothing matches any more so exit
                            return;
                        }
                        resourceToDelete = MassiveResource.getResource(resource.getLoginInfo(), resource.get_id());
                    }
                }
                ((WebDisplayable) resource).setWebDisplayPolicy(DisplayPolicy.HIDDEN);
                resource.uploadToMassive(new AddThenDeleteStrategy(null, State.DRAFT, true, resourceToDelete));
            }
        }
    }

    public static void clearCache() {
        allResources = new Hashtable<String, MassiveResource>();
    }
}
