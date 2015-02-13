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

package com.ibm.ws.lars.upload.cli;

/**
 * The set of actions that a user can request the client to perform
 */
public enum Action implements HelpMessage {

    HELP("help") {
        @Override
        public String getHelpMessage() {
            return "Show this help.";
        }
    },
    UPLOAD("upload") {
        @Override
        public String getHelpMessage() {
            return "Upload ESAs to the repository. The ESA files to upload should be passed on the command line.";
        }
    },
    DELETE("delete") {
        @Override
        public String getHelpMessage() {
            return "Delete one or more assets from the repository, specified by asset ID";
        }
    },
    LISTALL("listAll") {
        @Override
        public String getHelpMessage() {
            return "List all the assets currently in the repository";
        }
    };

    private String argument;

    private Action(String argument) {
        this.argument = argument;
    }

    /**
     * Returns the text of the argument passed on the command line
     * <p>
     * If this method returns 'foo', the argument passed on the command line would be '--foo'
     *
     * @return the argument text
     */
    public String getArgument() {
        return argument;
    }

    /**
     * Retrieves the action for the given command line argument.
     * <p>
     * The argument should be passed without the leading '--'
     *
     * @param argument the argument text
     * @return the corresponding action or null if the argument is not recognised as an action
     */
    public static Action getByArgument(String argument) {
        for (Action action : Action.values()) {
            if (action.argument.equals(argument)) {
                return action;
            }
        }
        return null;
    }

}
