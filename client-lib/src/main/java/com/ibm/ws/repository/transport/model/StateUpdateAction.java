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

package com.ibm.ws.repository.transport.model;

import com.ibm.ws.repository.common.enums.StateAction;

/**
 * This is a wrapper object to store an action to take on the state of an asset.
 */
public class StateUpdateAction extends AbstractJSON {

    private StateAction action;

    /**
     * Default constructor that does not set the action property
     */
    public StateUpdateAction() {

    }

    /**
     * Construct an instance of this class with the action set
     * 
     * @param action
     */
    public StateUpdateAction(StateAction action) {
        this.action = action;
    }

    public StateAction getAction() {
        return action;
    }

    public void setAction(StateAction action) {
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
        StateUpdateAction other = (StateUpdateAction) obj;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equals(other.action))
            return false;
        return true;
    }

}
