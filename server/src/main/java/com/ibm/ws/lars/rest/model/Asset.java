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

package com.ibm.ws.lars.rest.model;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.exceptions.RepositoryException;

/**
 * Enforces the constrains on a JSON document to turn it into a Asset as required by the asset
 * service.
 *
 */
public class Asset extends RepositoryObject {

    public static final String ATTACHMENTS = "attachments";

    /**  */
    public static final String LAST_UPDATED_ON = "lastUpdatedOn";
    /**  */
    public static final String CREATED_ON = "createdOn";

    public static final String STATE = "state";

    public static final String CREATED_BY = "createdBy";

    public static final String NAME = "name";

    public Asset() {
        super();
    }

    /**
     * Wrap an existing map of properties in an asset object.
     * <p>
     * Changes to the asset will be reflected in the original map.
     *
     * @param state the map of properties
     */
    public Asset(Map<String, Object> state) {
        super(state);
    }

    /**
     * Copy another attachment, creating a new copy of the internal set of properties.
     *
     * @param toCopy the attachment to clone
     */
    public Asset(Asset clone) {
        super(clone);
    }

    public String getCreatedOn() {
        return get(CREATED_ON);
    }

    public void setCreatedOn(String date) {
        put(CREATED_ON, date);
    }

    public String getLastUpdatedOn() {
        Object lastUpdatedOn = properties.get(LAST_UPDATED_ON);
        if (lastUpdatedOn != null) {
            return (String) lastUpdatedOn;
        }
        return null;
    }

    public void setLastUpdatedOn(String date) {
        put(LAST_UPDATED_ON, date);
    }

    public String getCreatedBy() {
        String createdByName = null;
    
        Map<?, ?> createdBy = (Map<?, ?>) properties.get(CREATED_BY);
        if (createdBy != null) {
            createdByName = (String) createdBy.get(NAME);
        }
    
        return createdByName;
    }

    public void setCreatedBy(String name) {
        Map<String, Object> createdBy = new HashMap<>();
        createdBy.put(NAME, name);
        properties.put(CREATED_BY, createdBy);
    }

    /**
     * Reads an asset from the supplied JSON. No fields will be modified. This method does not
     * sanitise the asset or check that it is in a fit state to be written straight to the database.
     *
     * TODO we really wanna get rid of this method
     */
    public static Asset deserializeAssetFromJson(String json) throws InvalidJsonAssetException {
        // TODO get rid

        return new Asset(readJsonState(json));
    }

    /**
     * Reads an asset from the supplied JSON. No fields will be modified. This method does not
     * sanitise the asset or check that it is in a fit state to be written straight to the database.
     *
     * @param json
     * @return
     * @throws InvalidJsonAssetException
     */
    public static Asset deserializeAssetFromJson(InputStream json) throws InvalidJsonAssetException {
        return new Asset(readJsonState(json));
    }

    public static Asset createAssetFromMap(Map<String, Object> state) {
        return new Asset(state);
    }

    public void setAttachments(AttachmentList attachments) {
        put(ATTACHMENTS, attachments.getState());
    }

    public AttachmentList getAttachments() {
        List<Map<String, Object>> attachmentsState = get(ATTACHMENTS);
        return AttachmentList.createAttachmentListFromMaps(attachmentsState);
    }

    /**
     * State should normally be updated using the <code>updateState</code> method which enforces
     * life-cycle state transition rules.
     *
     * @param newState
     */
    private void setState(State newState) {
        properties.put(STATE, newState.getValue());
    }

    public State getState() {
        Object state = properties.get(STATE);
        if (state != null) {
            State foundState = State.forValue((String) state);
            if (foundState != null) {
                return foundState;
            }
        }
        // Have failed to find a correct state value in the asset. This is an internal
        // error as the system should not allow an asset to be created in this manner.
        throw new RepositoryException("Asset doesn't contain valid state value."
                                      + "Actual value is: " + properties.get(STATE));
    }

    /**
     * This interface is implemented by the State enum. The methods change the state of the asset.
     * The actions equate to the following state transitions:
     *
     * PUBLISH: Move the state from DRAFT to AWAITING_APPROVAL<br>
     * APPROVE: Move the state from AWAITING_APPROVAL to PUBLISHED<br>
     * CANCEL: Move the state from AWAITING_APPROVAL to DRAFT<br>
     * NEED_MORE_INFO: Move the state from AWAITING_APPROVAL to NEED_MORE_INFO<br>
     * UNPUBLISH: Move the state from PUBLISHED to DRAFT
     *
     */
    private interface StateMachine {
        public void publish(Asset ass) throws RepositoryResourceLifecycleException;

        public void approve(Asset ass) throws RepositoryResourceLifecycleException;

        public void cancel(Asset ass) throws RepositoryResourceLifecycleException;

        public void need_more_info(Asset ass) throws RepositoryResourceLifecycleException;

        public void unpublish(Asset ass) throws RepositoryResourceLifecycleException;

    }

    public enum State implements StateMachine {

        /**
         * The asset is in draft state, valid transitions are to publish.
         */
        DRAFT("draft") {
            @Override
            public void publish(Asset ass) throws RepositoryResourceLifecycleException {
                ass.setState(State.AWAITING_APPROVAL);
            }

        },

        /**
         * The asset is in awaiting_approval valid transitions are to approve, cancel, or
         * need_more_info
         */
        AWAITING_APPROVAL("awaiting_approval") {

            @Override
            public void approve(Asset ass) throws RepositoryResourceLifecycleException {
                ass.setState(State.PUBLISHED);
            }

            @Override
            public void cancel(Asset ass) throws RepositoryResourceLifecycleException {
                ass.setState(State.DRAFT);
            }

            @Override
            public void need_more_info(Asset ass) throws RepositoryResourceLifecycleException {
                ass.setState(State.NEED_MORE_INFO);
            }

        },

        /**
         * The asset is in need_more_info state, valid transitions are to publish
         */
        NEED_MORE_INFO("need_more_info") {
            @Override
            public void publish(Asset ass) throws RepositoryResourceLifecycleException {
                ass.setState(State.AWAITING_APPROVAL);
            }

        },

        /**
         * The asset is in published state, there are no valid transitions from this state.
         */
        PUBLISHED("published") {
            @Override
            public void unpublish(Asset ass) throws RepositoryResourceLifecycleException {
                ass.setState(State.DRAFT);
            }
        };

        // Default implementation of the StateMachine methods, which all throw exceptions
        @Override
        public void publish(Asset ass) throws RepositoryResourceLifecycleException {
            throw new RepositoryResourceLifecycleException(this, StateAction.PUBLISH);
        }

        @Override
        public void approve(Asset ass) throws RepositoryResourceLifecycleException {
            throw new RepositoryResourceLifecycleException(this, StateAction.APPROVE);
        }

        @Override
        public void cancel(Asset ass) throws RepositoryResourceLifecycleException {
            throw new RepositoryResourceLifecycleException(this, StateAction.CANCEL);
        }

        @Override
        public void need_more_info(Asset ass) throws RepositoryResourceLifecycleException {
            throw new RepositoryResourceLifecycleException(this, StateAction.NEED_MORE_INFO);
        }

        @Override
        public void unpublish(Asset ass) throws RepositoryResourceLifecycleException {
            throw new RepositoryResourceLifecycleException(this, StateAction.UNPUBLISH);
        }

        private final String state;

        private State(String state) {
            this.state = state;
        }

        public String getValue() {
            return state;
        }

        public static State forValue(String value) {
            for (State state : State.values()) {
                if (state.getValue().equals(value)) {
                    return state;
                }
            }
            return null;
        }
    }

    private interface PerformAction {
        public void performAction(Asset asset) throws RepositoryResourceLifecycleException;
    }

    /**
     * The possible actions to change the state of an asset.
     */
    public enum StateAction implements PerformAction {

        PUBLISH("publish") {
            @Override
            public void performAction(Asset asset) throws RepositoryResourceLifecycleException {
                asset.getState().publish(asset);
            }
        }
        ,
        APPROVE("approve") {
            @Override
            public void performAction(Asset asset) throws RepositoryResourceLifecycleException {
                asset.getState().approve(asset);
            }
        },
        CANCEL("cancel") {
            @Override
            public void performAction(Asset asset) throws RepositoryResourceLifecycleException {
                asset.getState().cancel(asset);
            }
        },
        NEED_MORE_INFO("need_more_info") {
            @Override
            public void performAction(Asset asset) throws RepositoryResourceLifecycleException {
                asset.getState().need_more_info(asset);
            }
        },
        UNPUBLISH("unpublish") {
            @Override
            public void performAction(Asset asset) throws RepositoryResourceLifecycleException {
                asset.getState().unpublish(asset);
            }
        };

        String action;

        private StateAction(String action) {
            this.action = action;
        }

        public String getValue() {
            return action;
        }

        public static StateAction forValue(String value) {
            for (StateAction stateAction : StateAction.values()) {
                if (stateAction.getValue().equals(value)) {
                    return stateAction;
                }
            }
            return null;
        }

    }

}
