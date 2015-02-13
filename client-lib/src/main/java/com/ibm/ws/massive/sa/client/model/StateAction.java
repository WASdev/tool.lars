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

package com.ibm.ws.massive.sa.client.model;

import com.ibm.ws.massive.sa.client.model.Asset.State;

/**
 * This is a wrapper object to store an action to take on the state of an asset.
 */
public class StateAction extends AbstractJSON {

    private Action action;

    public enum Action {
        /**
         * Move the state from {@link State#draft} to {@link State#awaiting_approval}
         */
        PUBLISH("publish"),
        /**
         * Move the state from {@link State#awaiting_approval} to {@link State#published}
         */
        APPROVE("approve"),
        /**
         * Move the state from {@link State#awaiting_approval} to {@link State#draft}
         */
        CANCEL("cancel"),
        /**
         * Move the state from {@link State#awaiting_approval} to {@link State#need_more_info}
         */
        NEED_MORE_INFO("need_more_info"),
        /**
         * Move the state from {@link State#published} to {@link State#draft}
         */
        UNPUBLISH("unpublish");

        private final String action;

        private Action(String action) {
            this.action = action;
        }

        public String getValue() {
            return action;
        }

        public static Action forValue(String value) {
            for (Action action : Action.values()) {
                if (action.getValue().equals(value)) {
                    return action;
                }
            }
            return null;
        }
    }

    /**
     * Default constructor that does not set the action property
     */
    public StateAction() {

    }

    /**
     * Construct an instance of this class with the action set
     *
     * @param action
     */
    public StateAction(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StateAction other = (StateAction) obj;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equals(other.action))
            return false;
        return true;
    }

}
