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

package com.ibm.ws.repository.common.enums;

public enum StateAction {
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

    private StateAction(String action) {
        this.action = action;
    }

    public String getValue() {
        return action;
    }

    public static StateAction forValue(String value) {
        for (StateAction action : StateAction.values()) {
            if (action.getValue().equals(value)) {
                return action;
            }
        }
        return null;
    }

}