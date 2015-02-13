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
import com.ibm.ws.massive.resources.MassiveResource.AttachmentResource;
import com.ibm.ws.massive.resources.MassiveResource.DisplayPolicy;
import com.ibm.ws.massive.resources.MassiveResource.State;

/**
 * This is the base class for upload strategies. It provides access to package level methods in MassiveResource
 * so that strategies that don't live in the resources package can access these methods. It also provides some
 * common methods for derived classes to use.
 */
public abstract class BaseStrategy implements UploadStrategy {

    /*
     * Desired states depending of whether a matching asset was found or not.
     */
    protected State _desiredStateIfMatchingFound;
    protected State _desiredStateIfNoMatchingFound;

    /**
     * Uses default state, which is to set the asset to draft if no matching asset is found, and to use
     * the state of the matching resource if a matching resource is found
     */
    protected BaseStrategy() {
        _desiredStateIfMatchingFound = null; // if a matching is found then use it
        _desiredStateIfNoMatchingFound = State.DRAFT;
    }

    /**
     * Specify the states to use if a matching resource is found or not.
     *
     * @param desiredStateIfMatchingFound If this is set to null then use the state of the matching resource
     * @param desiredStateIfNoMatchingFound
     */
    protected BaseStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound) {
        _desiredStateIfMatchingFound = desiredStateIfMatchingFound;
        _desiredStateIfNoMatchingFound = desiredStateIfNoMatchingFound;
    }

    /**
     * Works out the target state based on the values passed to the constructor and whether a matching resource was found
     * If there is no matching resource use desiredStateIfNoMatchingFound. If there is a matching resource and a
     * desiredStateIfMatchingFound state has been set then use it. If a desiredStateIfMatchingFound has not been set (its
     * null) then use the matching resource's state
     *
     * @param matchingResource
     * @return
     */
    protected State calculateTargetState(MassiveResource matchingResource) {
        State targetState = _desiredStateIfNoMatchingFound;
        if (matchingResource != null) {
            if (_desiredStateIfMatchingFound == null) {
                targetState = matchingResource.getState();
            } else {
                targetState = _desiredStateIfMatchingFound;
            }
        }

        return targetState;
    }

    /**
     * Delegates to the resource.findMatchingResource, see {@link MassiveResource#findMatchingResource()}
     */
    @Override
    public List<MassiveResource> findMatchingResources(MassiveResource resource) throws RepositoryResourceValidationException,
                    RepositoryBackendException, RepositoryBadDataException {
        return resource.findMatchingResource();
    }

    /*
     * -----------------------------------------------------------------------------------------------------------------
     * These methods provide access to package level methods in MassiveResource
     * -----------------------------------------------------------------------------------------------------------------
     */

    /**
     * This method copies the fields from "this" that we care about to the "fromResource". Then we
     * set our asset to point to the one in "fromResource". In effect this means we get all the details
     * from the "fromResource" and override fields we care about and store the merged result in our asset.
     *
     * @param fromResource
     * @throws RepositoryResourceValidationException
     */
    protected void overWriteAssetData(MassiveResource resource, MassiveResource fromResource, boolean includeAttachmentInfo) throws RepositoryResourceValidationException {
        resource.overWriteAssetData(fromResource, includeAttachmentInfo);
    }

    /**
     * Add the asset to the backend, this does NOT add any attachments, just the base asset itself
     *
     * @param resource The resource to update
     * @throws RepositoryResourceCreationException
     * @throws RepositoryBadDataException
     */
    protected void addAsset(MassiveResource resource) throws RepositoryResourceCreationException, RepositoryBadDataException {
        resource.addAsset();
    }

    /**
     * Updates the asset in the repo. This does not update any attachments
     *
     * @param resource The resource to update
     * @throws RepositoryResourceUpdateException
     * @throws RepositoryResourceValidationException
     * @throws RepositoryBadDataException
     */
    protected void updateAsset(MassiveResource resource) throws RepositoryResourceUpdateException,
                    RepositoryResourceValidationException, RepositoryBadDataException {
        resource.updateAsset();
    }

    /**
     * Copies the asset from the "from" resource to the "resource" resource
     *
     * @param resource
     * @param from
     */
    protected void copyAsset(MassiveResource resource, MassiveResource from) {
        resource.copyAsset(from);
    }

    /**
     * Adds the attachment to the repo.
     *
     * @param resource
     * @param at
     * @throws RepositoryResourceCreationException
     * @throws RepositoryBadDataException
     * @throws RepositoryResourceUpdateException
     */
    protected void addAttachment(MassiveResource resource, AttachmentResource at) throws RepositoryResourceCreationException,
                    RepositoryBadDataException, RepositoryResourceUpdateException {
        resource.addAttachment(at);
    }

    /**
     * Updates the attachment in massive
     *
     * @param resource
     * @param at
     * @throws RepositoryResourceUpdateException
     * @throws RepositoryBadDataException
     */
    protected void updateAttachment(MassiveResource resource, AttachmentResource at) throws RepositoryResourceUpdateException,
                    RepositoryBadDataException {
        resource.updateAttachment(at);
    }

    /**
     * Gets the web display policy
     *
     * @param resource
     * @return
     */
    protected DisplayPolicy getWebDisplayPolicy(MassiveResource resource) {
        switch (resource.getType()) {
            case FEATURE:
                return ((EsaResource) resource).getWebDisplayPolicy();
            case IFIX:
                return ((IfixResource) resource).getWebDisplayPolicy();
            case TOOL:
                // Fall through to install
            case ADDON:
                // Fall through to install
            case INSTALL:
                return ((ProductRelatedResource) resource).getWebDisplayPolicy();
            default:
                // Not applicable for other types
                return null;
        }
    }

    /*
     * -----------------------------------------------------------------------------------------------------------------
     * End of "expose package level methods on MassiveResource" section
     * -----------------------------------------------------------------------------------------------------------------
     */
}
