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
        public String getHelpSummary() {
            return "Show help for larsClient.";
        }

        @Override
        public String getUsage() {
            return "help [COMMAND]";
        }

        @Override
        public String getHelpDetail() {
            return "Show help for larsClient.";
        }
    },
    UPLOAD("upload") {
        @Override
        public String getHelpSummary() {
            return "Upload ESAs to the repository.";
        }

        @Override
        public String getUsage() {
            return "upload [FILE]...";
        }

        @Override
        public String getHelpDetail() {
            return "Uploads one or more features to a LARS server. "
                   + "For each argument, if the argument is a file with the extension .esa then this is treated as a Liberty feature and uploaded. "
                   + "If the argument is a directory then any .esa files within that directory are treated as Liberty features and uploaded. "
                   + "Note that subdirectories are not recursively searched.";
        }
    },
    DELETE("delete") {
        @Override
        public String getHelpSummary() {
            return "Delete one or more assets from the repository, specified by id.";
        }

        @Override
        public String getUsage() {
            return "delete [ASSET-ID]...";
        }

        @Override
        public String getHelpDetail() {
            return "Delete one or more assets from the repository, specified by id.";
        }
    },
    LISTALL("listAll") {
        @Override
        public String getHelpSummary() {
            return "List all the assets currently in the repository.";
        }

        @Override
        public String getUsage() {
            return "listAll";
        }

        @Override
        public String getHelpDetail() {
            return "List all the assets currently in the repository.";
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
