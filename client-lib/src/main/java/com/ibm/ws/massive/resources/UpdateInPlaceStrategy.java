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
import java.util.List;

import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.resources.MassiveResource.AttachmentResource;
import com.ibm.ws.massive.resources.MassiveResource.State;

/**
 * This strategy will overwrite an existing resource if it finds a matching one. This strategy uses the default mechanism
 * for setting the state of the overwritten resource, which is: If there is a matching resource and a
 * desiredStateIfMatchingFound state has been set then use it. If a desiredStateIfMatchingFound has not been set (its
 * null) then use the matching resource's state
 */
public class UpdateInPlaceStrategy extends BaseStrategy {

    private boolean _forceReplace;
    private List<MassiveResource> _matchingResources = null;

    /**
     * Delegate so super class for states
     */
    public UpdateInPlaceStrategy() {}

    /**
     * Sets the desired state of the asset after uploading it
     *
     * @param desiredStateIfMatchingFound Set the resource to this state if a matching resource was found. If this
     *            is set to null then it will set the state to whatever state the matching resource is set to.
     * @param desiredStateIfNoMatchingFound If no matching resource is found then set the state to this value
     * @param forceReplace Set to true if you wish to perform a replace even if a matching resource was found.
     */
    public UpdateInPlaceStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace) {
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
    public UpdateInPlaceStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace,
                                 MassiveResource matchingResource) {
        super(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound);
        _forceReplace = forceReplace;
        if (matchingResource != null) {
            _matchingResources = new ArrayList<MassiveResource>();
            _matchingResources.add(matchingResource);
        }
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
     * {@inheritDoc}
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    @Override
    public void uploadAsset(MassiveResource resource, List<MassiveResource> matchingResources)
                    throws RepositoryBackendException, RepositoryResourceException {
        MassiveResource firstMatch = (matchingResources == null || matchingResources.isEmpty()) ? null : matchingResources.get(0);
        State targetState = calculateTargetState(firstMatch);

        // We need to unpublish in order to overwrite
        if (firstMatch != null && firstMatch.getState() == State.PUBLISHED) {
            firstMatch.unpublish();
        }

        // Is this an update or an add?
        switch (resource.updateRequired(firstMatch)) {
            case ADD:
                addAsset(resource);
                break;
            case NOTHING:
                if (!_forceReplace) {
                    // Use the asset from massive, although it's identical as far as we are concerned it
                    // also has the fields massive sets onto an asset when it gets uploaded, including
                    // the id itself.
                    copyAsset(resource, firstMatch);
                    break;
                }
                // If force replace is true then drop through to the update logic.
            case UPDATE:
                // Partial updates don't work so copy our data into the one
                // we found in massive and then set our asset to point to that
                // merged asset
                overWriteAssetData(resource, firstMatch, true);
                updateAsset(resource);
                break;
        }

        // Now iterate over the attachments
        for (AttachmentResource attachment : resource.getAttachments()) {
            uploadAttachment(resource, attachment, firstMatch);
        }

        // Read back from massive to get the fields massive generated into our resource
        resource.refreshFromMassive();

        resource.moveToState(targetState);
    }

    /**
     * Goes through each attachment and adds/updates the attachment as needed.
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws RepositoryResourceUpdateException
     * @throws RepositoryBadDataException
     * @throws RepositoryResourceCreationException
     */
    public void uploadAttachment(MassiveResource resource, AttachmentResource attachment, MassiveResource matchingResource)
                    throws RepositoryResourceCreationException, RepositoryBadDataException, RepositoryResourceUpdateException,
                    RepositoryBackendException, RepositoryResourceException {
        switch (attachment.updateRequired(matchingResource)) {
            case ADD:
                addAttachment(resource, attachment);
                break;
            case UPDATE:
                updateAttachment(resource, attachment);
                break;
            case NOTHING:
                // Nothing to do but have to include this to stop findbugs crying
                break;
        }
    }
}
