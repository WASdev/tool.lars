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

import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryResourceValidationException;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;

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
    protected State calculateTargetState(RepositoryResourceImpl matchingResource) {
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
     * Delegates to the resource.findMatchingResource, see {@link RepositoryResourceImpl#findMatchingResource()}
     */
    @Override
    public List<RepositoryResourceImpl> findMatchingResources(RepositoryResourceImpl resource) throws RepositoryResourceValidationException,
                    RepositoryBackendException, RepositoryBadDataException {
        return resource.findMatchingResource();
    }

    @Override
    public boolean performEditionChecking() {
        return true;
    }

}
