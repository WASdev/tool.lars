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

/**
 * The purpose of this class is to handle printing help messages to the specified PrintStream. This
 * class doesn't contain the messages themselves but it knows how to print them.
 */
public class Help {

    private final PrintStream outputStream;
    private final String programName;

    public Help(PrintStream outputStream) {
        this.outputStream = outputStream;

        // Work out the name of the executable (which we will need for printing
        // out usage instructions).
        String invokedName = Main.getInvokedName();
        if (invokedName != null) {
            this.programName = invokedName;
        } else {
            this.programName = "java -jar larsClient.jar";
        }
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
     * Prints a usage message for the specified command.
     *
     * @param usageSummary the one line Usage: statement
     * @param detail a detailed message that will be wrapped if necessary.
     */
    public void printCommandUsage(String usageSummary, String detail) {
        outputStream.println("Usage: " + this.programName + " " + usageSummary);
        if (detail != null) {
            outputStream.println();
            printWrappedText(detail);
        }
    }

    /**
     * Prints an error message to the output stream, followed by global help.
     */
    public void printMessageAndGlobalUsage(String errorMessage) {
        outputStream.println(errorMessage);
        outputStream.println();
        printGlobalUsage();
    }

    /**
     * Prints a global usage message for larsClient.
     */
    public void printGlobalUsage() {
        // Print usage
        outputStream.print("Usage: ");
        outputStream.print(programName);
        outputStream.print(" action [options] [arguments] ...");
        outputStream.println();
        outputStream.println();

        // Print actions
        outputStream.println("Actions:");
        outputStream.println();
        for (Action action : Action.values()) {
            printAction(action.getArgument(), action.getHelpSummary(), programName);
        }

        // Print options
        printGlobalOptions();
    }

    /**
     * Prints the global usage message for larsClient. This message should be appropriate either in
     * a global context or in the context of a particular command.
     */
    public void printGlobalOptions() {
        // Print options
        outputStream.println("Options:");
        outputStream.println();
        printArgument("--url=\"repository URL\"", "Specify the URL of the repository to use.");
        printArgument("--username=\"user name\"",
                      "Specify the user name to use when connecting to the repository. If "
                              + "this and --password are not set then the client will connect without "
                              + "authentication.");
        printArgument("--password=\"password\"",
                      "Specify the password to use when connecting to the repository. If "
                              + "this and --username are not set then the client will connect without "
                              + "authentication. Using this option passes your password on the command "
                              + "line which exposes it to other users on the system who can view the "
                              + "list of running processes. Consider using --password without an "
                              + "argument to specify that the password should be prompted for on "
                              + "standard input.");
        printArgument("--password",
                      "Specify that the password should be prompted for on standard input. "
                              + "Using this option prevents your password from being seen by other "
                              + "users.");
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
