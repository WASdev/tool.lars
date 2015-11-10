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

package com.ibm.ws.repository.exceptions;

import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.common.enums.StateAction;

/**
 * An exception to indicate that a state transition attempted on a Massive
 * Asset was not a valid transition for an asset in that state ie between
 * draft and published without going through awwaiting_approval.
 * 
 */
public class RepositoryResourceLifecycleException extends RepositoryResourceException {

    private static final long serialVersionUID = -7562518379718038340L;

    private final State oldState;
    private final StateAction action;

    public RepositoryResourceLifecycleException(String message, String resourceId, State oldState, StateAction action) {
        super(message, resourceId);
        this.oldState = oldState;
        this.action = action;
    }

    public RepositoryResourceLifecycleException(String message, String resourceId, State oldState, StateAction action, Throwable cause) {
        super(message, resourceId, cause);
        this.oldState = oldState;
        this.action = action;
    }

    public State getOldState() {
        return this.oldState;
    }

    public StateAction getAction() {
        return this.action;
    }
}
