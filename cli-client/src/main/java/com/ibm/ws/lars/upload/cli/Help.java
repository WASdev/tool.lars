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

import java.io.PrintStream;

public class Help {

    PrintStream outputStream;

    public Help(PrintStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Prints the help text for an action
     * <p>
     * This is the same as {@link #printArgument(String, String)} except that if the
     * <code>invokedName</code> is null (i.e. the program was not invoked from a script) the action
     * name will have "--" prepended.
     *
     * @param action the bare action name, as used if the program is invoked from a script
     * @param description the description of the action
     */
    public void printAction(String action, String description, String invokedName) {

        // Use --action style if the jar is invoked directly to match other liberty utilities
        if (invokedName == null) {
            action = "--" + action;
        }

        printArgument(action, description);
    }

    /**
     * Prints the help text for an argument
     * <p>
     * The argument is indented using four spaces.
     * <p>
     * The description is indented using a tab and wrapped at 80 columns (i.e. tab + 72 characters)
     * <p>
     * Example:
     *
     * <pre>
     *    --username="user name"
     *    Specify the user name to use when connecting to the repository. If this
     *    and --password are not set then the client will connect without
     *    authentication.
     * </pre>
     *
     * @param option the string showing how the argument is used, exactly as it should be displayed
     * @param description the description of the argument
     */
    public void printArgument(String option, String description) {
        outputStream.print("    ");
        outputStream.println(option);
        printWrappedText(description);
    }

    /**
     * Prints a block of text, indented by one tab and wrapped at 80 columns (tab + 72 characters)
     * <p>
     * Newlines within the text are treated as a hard line break.
     *
     * @param text the text to display
     */
    private void printWrappedText(String text) {
        String paragraphs[] = text.split("\n");

        for (String paragraph : paragraphs) {
            int startPosition = 0;
            while (startPosition < paragraph.length()) {
                int endPosition;
                int newStartPosition;

                // Find last space up to 72nd char
                if (startPosition + 72 <= paragraph.length()) {
                    endPosition = paragraph.lastIndexOf(' ', startPosition + 72);
                } else {
                    endPosition = paragraph.length();
                }

                // If there are no spaces, break at 72 characters
                if (endPosition < startPosition) {
                    endPosition = 72;
                    newStartPosition = endPosition;
                } else {
                    // If we found a space, next line starts with the character after the space
                    newStartPosition = endPosition + 1;
                }

                outputStream.print("\t");
                outputStream.println(paragraph.substring(startPosition, endPosition));

                startPosition = newStartPosition;
            }
        }

        outputStream.println();
    }
}
