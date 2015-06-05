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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.resources.MassiveResource.AttachmentResource;
import com.ibm.ws.massive.resources.MassiveResource.State;

/**
 * This strategy will check if there is a matching resource.<br>
 * If the resource is equivalent, it will not upload anything.<br>
 * If the resource is different then a new resource is added to the repo and the original resource is deleted<br>
 * If there is no matching resource then the resource is added.<br>
 * <br>
 * The resource is then moved to the desired state based on the values passed to the constructor and whether a matching
 * resource was found. If there is no matching resource use desiredStateIfNoMatchingFound. If there is a matching
 * resource and a desiredStateIfMatchingFound state has been set then use it. If a desiredStateIfMatchingFound has not
 * been set (it's null) then set the new resource's state to match the state of the matching resource found in the
 * repo.
 * 
 * Note: this class is not threadsafe and should not be shared between multiple threads.
 */
public class AddThenDeleteStrategy extends AddNewStrategy {

    private boolean _forceReplace;
    private List<MassiveResource> _matchingResources = null;

    private List<MassiveResource> deletedResources = Collections.emptyList();

    /**
     * Delegate to super class for states
     */
    public AddThenDeleteStrategy() {}

    /**
     * Sets the desired state of the asset after uploading it
     *
     * @param desiredStateIfMatchingFound Set the resource to this state if a matching resource was found. If this
     *            is set to null then it will set the state to whatever state the matching resource is set to.
     * @param desiredStateIfNoMatchingFound If no matching resource is found then set the state to this value
     * @param forceReplace Set to true if you wish to perform a replace even if a matching resource was found.
     */
    public AddThenDeleteStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace) {
        super(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound);
        _forceReplace = forceReplace;
    }

    /**
     * Sets the desired state of the asset after uploading it
     *
     * @param desiredStateIfMatchingFound Set the resource to this state if a matching resource was found. If this
     *            is set to null then it will set the state to whatever state the matching resource is set to.
     * @param desiredStateIfNoMatchingFound If no matching resource is found then set the state to this value
     * @param forceReplace Set to true if you wish to perform a replace even if a matching resource was found.
     * @param matchingResource Set this if you wish to specify the resource to be replaced rather than letting
     *            the resource try and find a matching. This is of use in scenarios where there may be more than one matching
     *            resource and the caller can decide which one to use, as the resource logic will select the first matching one
     *            it finds
     */
    public AddThenDeleteStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace,
                                 MassiveResource matchingResource) {
        super(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound);
        _forceReplace = forceReplace;
        if (matchingResource != null) {
            _matchingResources = new ArrayList<MassiveResource>();
            _matchingResources.add(matchingResource);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    @Override
    public void uploadAsset(MassiveResource resource, List<MassiveResource> matchingResources) throws RepositoryBackendException, RepositoryResourceException {
        boolean doUpdate = false;
        boolean deleteOriginal = false;
        MassiveResource firstMatch = (matchingResources == null || matchingResources.isEmpty()) ? null : matchingResources.get(0);

        // The list of deleted resources is initialised to an empty list and
        // remains so until we actually delete something.
        // If we hit some logic in this method that causes resources to be deleted
        // then we will set it appropriately.
        deletedResources = Collections.emptyList();
        
        // Check if we need to do an update
        if (_forceReplace) {
            doUpdate = true;
            deleteOriginal = firstMatch == null ? false : true;
        } else {
            // First check assert itself for changes
            switch (resource.updateRequired(firstMatch)) {
            // This strategy will add a new asset instead of overwriting an existing one
            // Update should fall through to add
                case UPDATE:
                    deleteOriginal = true;
                case ADD:
                    doUpdate = true;
                    break;
                default:
                    // Nothing to do but have to include this to stop findbugs crying
            }

            // If the asset is the same, check attachments just in case they have changed
            if (!doUpdate) {
                // Then check each attachment
                loop: for (AttachmentResource attachment : resource.getAttachments()) {
                    switch (attachment.updateRequired(firstMatch)) {
                        case UPDATE:
                            deleteOriginal = true;
                        case ADD:
                            doUpdate = true;
                            break loop;
                        default:
                            // Nothing to do but have to include this to stop findbugs crying
                    }
                }
            }
        }

        // Only do an update if needed
        if (doUpdate) {

            super.uploadAsset(resource, matchingResources);

            // If the action was an update to an existing resource then delete the original now
            if (deleteOriginal) {
                deletedResources = matchingResources;

                for (MassiveResource massiveResource : matchingResources) {
                    massiveResource.delete();
                }
            }

        } else {
            // Use the asset from massive, although it's identical as far as we are concerned it
            // also has the fields massive sets onto an asset when it gets uploaded, including
            // the id itself.
            copyAsset(resource, firstMatch);

            // Also remove duplicates so there is only one asset left.  NOTE starting at 1, don't delete the first one as this is what we are using
            List<MassiveResource> resourcesToDelete = new ArrayList<>();
            for (int i = 1; i < matchingResources.size(); i++) {
                MassiveResource resourceToDelete = matchingResources.get(i);
                resourcesToDelete.add(resourceToDelete);
                resourceToDelete.delete();
            }
            deletedResources = resourcesToDelete;
        }
        resource.refreshFromMassive();
    }

    @Override
    public List<MassiveResource> findMatchingResources(MassiveResource resource) throws RepositoryResourceValidationException,
                    RepositoryBackendException, RepositoryBadDataException {
        if (_matchingResources != null) {
            return _matchingResources;
        } else {
            return resource.findMatchingResource();
        }
    }

    /**
     * The target state is dependent on if a matching resource is found. See {@link AddThenDeleteStrategy} class doc
     */
    @Override
    protected State getTargetState(MassiveResource matchingResource) {
        return calculateTargetState(matchingResource);
    }

    /**
     * Returns the list of resources (if any) that were deleted as a result of
     * the upload operation that this AddThenDeleteStrategy object is responsible
     * for.
     */
    public List<MassiveResource> getDeletedResources() {
        return deletedResources;
    }
}
