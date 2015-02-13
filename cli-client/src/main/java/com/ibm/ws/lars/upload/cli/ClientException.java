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
 * An exception that occurred in the client that should cause the client to exit.
 * <p>
 * The exception encapsulates the message that should be shown to the client, the return code that
 * should be returned from the program and whether usage help should be shown.
 */
@SuppressWarnings("serial")
public class ClientException extends Exception {

    private final int returnCode;
    private final HelpDisplay helpDisplay;

    public enum HelpDisplay {
        NO_HELP,
        SHOW_HELP
    }

    /**
     * Creates an exception with the given message and return code which does not show help.
     *
     * @param message the message to show to the user
     * @param returnCode the return code
     */
    public ClientException(String message, int returnCode) {
        this(message, returnCode, HelpDisplay.NO_HELP);
    }

    /**
     * Creates an exception with the given message and return code.
     * <p>
     * Help will be shown if {@link HelpDisplay#SHOW_HELP} is passed
     *
     * @param message the message to show to the user
     * @param returnCode the return code
     * @param option whether to show the usage help to the user
     */
    public ClientException(String message, int returnCode, HelpDisplay help) {
        this(message, returnCode, help, null);
    }

    /**
     * Creates an exception with the given message, return code and cause.
     * <p>
     * Help will be shown if {@link HelpDisplay#SHOW_HELP} is passed
     *
     * @param message the message to show to the user
     * @param returnCode the return code
     * @param option whether to show the usage help to the user
     * @param cause the cause of the exception
     */
    public ClientException(String message, int returnCode, HelpDisplay help, Throwable cause) {
        super(message, cause);
        this.returnCode = returnCode;
        this.helpDisplay = help;
    }

    /**
     * Returns the return code which should be returned when the program exits
     *
     * @return the return code
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * Returns what help should be displayed
     *
     * @return what help should be displayed
     */
    public HelpDisplay getHelpDisplay() {
        return helpDisplay;
    }
}
