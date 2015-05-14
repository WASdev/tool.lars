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

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.lars.upload.cli.ClientException.HelpDisplay;
import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.RepositoryBackendRequestFailureException;
import com.ibm.ws.massive.RepositoryException;
import com.ibm.ws.massive.esa.MassiveEsa;
import com.ibm.ws.massive.resources.AddThenDeleteStrategy;
import com.ibm.ws.massive.resources.EsaResource;
import com.ibm.ws.massive.resources.MassiveResource;
import com.ibm.ws.massive.resources.MassiveResource.State;
import com.ibm.ws.massive.resources.RepositoryBadDataException;
import com.ibm.ws.massive.resources.RepositoryResourceDeletionException;
import com.ibm.ws.massive.resources.UploadStrategy;

public class Main {

    static final String CONNECTION_PROBLEM = "There was a problem connecting to the repository: ";
    static final String MISSING_URL = "The repository url must be provided, either as an argument or in a configuration file.";
    static final String INVALID_URL = "The supplied url is not valid: ";
    static final String NO_IDS_FOR_DELETE = "No asset IDs were supplied.";
    static final String ASSET_NOT_FOUND = "Asset not found in repository.";
    static final String SERVER_ERROR = "The repository server returned an error.";
    static final String NO_FILES = "No files to upload. The files to upload must be provided as arguments.";

    private static Pattern versionPattern = Pattern.compile("productVersion=([0-9\\.+]+)");

    private Map<Option, String> options;
    private Action action;
    private String invokedName;

    private final PrintStream output;

    /**
     * All logic here should be delegated to run, to allow for easier testing
     */
    public static void main(String[] args) {
        Main main = new Main(System.out);

        try {
            main.run(args);
        } catch (ClientException e) {
            System.exit(e.getReturnCode());
        }

        System.exit(0);
    }

    public Main(PrintStream output) {
        this.output = output;
    }

    /**
     * Effectively a delegate of main, to allow for testing.
     *
     * @param args
     * @throws ClientException
     */
    public void run(String[] args) throws ClientException {

        invokedName = System.getenv("INVOKED");
        try {
            List<String> remainingArgs = readActionAndOptions(args);

            switch (action) {
                case UPLOAD:
                    doUpload(remainingArgs);
                    break;
                case DELETE:
                    doDelete(remainingArgs);
                    break;
                case LISTALL:
                    doListAll(remainingArgs);
                    break;
                case HELP:
                    showHelp();
                    break;
                default:
                    showHelp();
                    break;
            }
        } catch (ClientException e) {
            if (e.getHelpDisplay() == HelpDisplay.SHOW_HELP) {
                showHelp(e.getMessage());
            } else {
                output.println(e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Returns the name which was used to invoke this utility.
     * <p>
     * This is found by reading the INVOKED environment variable which should be set by any script
     * which launches this utility.
     *
     * @return the name by which this utility was invoked, or null if the jar was invoked directly
     */
    public String getInvokedName() {
        return invokedName;
    }

    /**
     * Reads the action and options from the command line, storing them in <code>action</code> and
     * <code>options</code>.
     * <p>
     * Reports errors and exits the program if there is a problem parsing the arguments.
     * <p>
     * The argument '--' is interpreted as an indication that all following arguments should not
     * parsed as options.
     *
     * @param args the array of args from main
     * @return the list of arguments which were not parsed as options
     */
    private List<String> readActionAndOptions(String[] args) throws ClientException {
        List<String> nonOptionArgs = new ArrayList<String>();

        if (args.length == 0) {
            throw new ClientException("No options were given", 1, HelpDisplay.SHOW_HELP);
        }

        String actionString = args[0];

        // If we're not invoked from a script, the action should start with "--"
        if (getInvokedName() == null) {
            if (!actionString.startsWith("--")) {
                throw new ClientException(actionString + " is not a valid action", 1, HelpDisplay.SHOW_HELP);
            }
            actionString = actionString.substring(2);
        }

        Action action = Action.getByArgument(actionString);
        if (action == null) {
            throw new ClientException(actionString + " is not a valid action", 1, HelpDisplay.SHOW_HELP);
        }
        this.action = action;
        this.options = new HashMap<Option, String>();

        boolean keepProcessingOptions = true;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];

            if (keepProcessingOptions && arg.equals("--")) {
                keepProcessingOptions = false;
            }
            else if (keepProcessingOptions && arg.startsWith("--")) {
                String[] argParts = arg.substring(2).split("=", 2);
                if (argParts.length == 0) {
                    throw new ClientException(arg + " is not a valid option", 1, HelpDisplay.SHOW_HELP);
                }
                String optionName = argParts[0];
                String value = argParts.length == 2 ? argParts[1] : null;

                Option option = Option.getByArgument(optionName);
                if (option == null) {
                    throw new ClientException(arg + " is not a valid option", 1, HelpDisplay.SHOW_HELP);
                }

                options.put(option, value);
            }
            else {
                nonOptionArgs.add(arg);
            }
        }

        return nonOptionArgs;
    }

    /**
     * Prints an error message to the output stream, followed by some help
     */
    private void showHelp(String errorMessage) {
        output.println(errorMessage);
        output.println();
        showHelp();
    }

    /**
     * Prints some help to stdout.
     */
    private void showHelp() {
        String programCommand = getInvokedName();
        if (programCommand == null) {
            programCommand = "java -jar larsClient.jar";
        }

        Help help = new Help(output);

        // Print usage
        output.print("Usage: ");
        output.print(programCommand);
        output.print(" action [options] esaFile ...");
        output.println();
        output.println();
        help.printArgument("esaFile", "A path to an OSGi enterprise subsystem archive which contains a liberty feature");

        // Print actions
        output.println("Actions:");
        output.println();
        for (Action action : Action.values()) {
            help.printAction(action.getArgument(), action.getHelpMessage(), this.invokedName);
        }

        // Print options
        output.println("Options:");
        output.println();
        help.printArgument("--url=\"repository URL\"", "Specify the URL of the repository to use.");
        help.printArgument("--username=\"user name\"",
                           "Specify the user name to use when connecting to the repository. If "
                                   + "this and --password are not set then the client will connect without "
                                   + "authentication.");
        help.printArgument("--password=\"password\"",
                           "Specify the password to use when connecting to the repository. If "
                                   + "this and --username are not set then the client will connect without "
                                   + "authentication. Using this option passes your password on the command "
                                   + "line which exposes it to other users on the system who can view the "
                                   + "list of running processes. Consider using --password without an "
                                   + "argument to specify that the password should be prompted for on "
                                   + "standard input.");
        help.printArgument("--password",
                           "Specify that the password should be prompted for on standard input. "
                                   + "Using this option prevents your password from being seen by other "
                                   + "users.");
    }

    private static final UploadStrategy UPLOAD_STRATEGY = new AddThenDeleteStrategy(State.PUBLISHED, State.PUBLISHED, true);

    /**
     * Uploads a list of ESAs.
     * <p>
     * This method will print error messages and exit the program if there is an error.
     *
     * @param remainingArgs a list of file paths to ESAs which should be uploaded.
     */
    private void doUpload(List<String> remainingArgs) throws ClientException {
        LoginInfoEntry loginInfoEntry = createLoginInfoEntry();
        List<File> files = new ArrayList<File>();
        for (String arg : remainingArgs) {
            files.add(new File(arg));
        }

        if (files.isEmpty()) {
            throw new ClientException(NO_FILES, 1, HelpDisplay.SHOW_HELP);
        }

        // Specifying the wrong file is a likely mistake, check them all before we upload anything
        for (File file : files) {
            if (!file.canRead()) {
                throw new ClientException("File " + file.toString() + " can't be read", 1, HelpDisplay.NO_HELP);
            }
            if (file.isDirectory()) {
                throw new ClientException("File " + file.toString() + " is a directory", 1, HelpDisplay.NO_HELP);
            }
        }

        MassiveEsa uploader;

        try {
            uploader = new MassiveEsa(loginInfoEntry);
        } catch (RepositoryException ex) {
            throw new ClientException("An error occurred while connecting to the repository: " + ex.getMessage(),
                    1, HelpDisplay.NO_HELP, ex);
        }

        for (File file : files) {
            try {
                output.print("Uploading " + file.toString() + " ... ");
                uploader.addEsasToMassive(Collections.singleton(file), UPLOAD_STRATEGY);
                output.println("done");
            } catch (RepositoryException ex) {
                throw new ClientException("An error occurred while uploading " + file.toString() + ": " + ex.getMessage(),
                        1, HelpDisplay.NO_HELP, ex);
            }
        }
    }

    private void doListAll(List<String> params) throws ClientException {
        LoginInfoEntry loginInfoEntry = createLoginInfoEntry();
        Collection<MassiveResource> assets = null;
        try {
            assets = MassiveResource.getAllResources(new LoginInfo(loginInfoEntry));
        } catch (RepositoryBackendException e) {
            throw new ClientException("An error was recieved from the repository: " + e.getMessage(), 1, HelpDisplay.NO_HELP, e);
        }

        output.println("Listing all assets in the repository:");

        if (assets.size() == 0) {
            output.println("No assets found in repository");
            return;
        }

        printTabbed("Asset ID", "Asset Type", "Liberty Version", "Asset Name");

        for (MassiveResource resource : assets) {
            String type = resource.getType().getValue();
            if (type.startsWith("com.ibm.websphere")) {
                type = type.substring(18);
            }

            String name = resource.getName();
            String shortName = null;
            if (resource instanceof EsaResource) {
                shortName = ((EsaResource) resource).getShortName();
            }
            if (shortName != null) {
                name = name + " (" + shortName + ")";
            }

            String appliesTo = "";
            if (resource instanceof EsaResource) {
                EsaResource esa = (EsaResource) resource;
                String fullAppliesTo = esa.getAppliesTo();
                if (fullAppliesTo != null) {
                    Matcher versionMatcher = versionPattern.matcher(fullAppliesTo);
                    if (versionMatcher.find()) {
                        appliesTo = versionMatcher.group(1);
                    }
                }
            }

            printTabbed(resource.get_id(), type, appliesTo, name);
        }

    }

    void printTabbed(String id, String type, String appliesTo, String name) {
        output.format("%-30.30s | %-15.15s | %-15.15s | %s%n", id, type, appliesTo, name);
    }

    private void doDelete(List<String> remainingArgs) throws ClientException {
        LoginInfoEntry loginInfoEntry = createLoginInfoEntry();

        if (remainingArgs.size() == 0) {
            throw new ClientException(NO_IDS_FOR_DELETE, 1, HelpDisplay.SHOW_HELP);
        }

        for (String id : remainingArgs) {
            MassiveResource toDelete = null;
            try {
                toDelete = MassiveResource.getResource(loginInfoEntry, id);
            } catch (RepositoryBadDataException e) {
                // This shouldn't happen unless there is client lib bug
                throw new ClientException("Asset " + id + " not deleted. " + e.getMessage(), 1, HelpDisplay.NO_HELP, e);
            } catch (RepositoryBackendRequestFailureException e) {
                // Server said no
                RepositoryBackendRequestFailureException requestFailed = e;
                int response = requestFailed.getResponseCode();
                if (response == 404) {
                    // Not found, go on to the next one
                    output.println("Asset " + id + " not deleted. " + ASSET_NOT_FOUND);
                    continue;
                }
                // Anything else should be a server error
                throw new ClientException("Asset " + id + " not deleted. " + SERVER_ERROR + e.getMessage(),
                        1, HelpDisplay.NO_HELP, e);

            } catch (RepositoryBackendException e) {
                // Anything else is probably some kind of connection problem, so ditch out
                throw new ClientException("Asset " + id + " not deleted. " + CONNECTION_PROBLEM + e.getMessage(),
                        1, HelpDisplay.NO_HELP, e);
            }

            try {
                toDelete.delete();
            } catch (RepositoryResourceDeletionException e) {
                // This can be an IO issue, or a request fail. Request fail is either a server
                // problem, or a non-existent asset. We've just checked that the asset exists, so
                // non-existent asset seems unlikely, most likely a server problem. As the source exception
                // isn't public, we can't check. Either way, this is probably terminal
                String message = "Asset " + id + " not deleted. There was a problem with the repository";
                Throwable cause = e.getCause();
                if (cause != null) {
                    message += cause.getMessage();
                } else {
                    message += e.getMessage();
                }
                throw new ClientException(message, 1, HelpDisplay.NO_HELP, e);
            }

            output.println("Deleted asset " + id);
        }
    }

    /**
     * Creates a logininfo object from the options on the command line
     *
     * @return
     */
    private LoginInfoEntry createLoginInfoEntry() throws ClientException {
        String urlString = null;
        String username = null;
        String password = null;
        boolean promptForPassword = false;

        if (options.containsKey(Option.CONFIG_FILE)) {
            File configFile = new File(options.get(Option.CONFIG_FILE));
            try (InputStream in = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(in);
                urlString = props.getProperty(Option.URL.getArgument());
                username = props.getProperty(Option.USERNAME.getArgument());
                password = props.getProperty(Option.PASSWORD.getArgument());
                if (password.trim().length() == 0) {
                    password = null;
                    promptForPassword = true;
                }
            } catch (IOException e) {
                output.println("Error reading config file: " + e.getMessage());
            }
        }

        if (options.containsKey(Option.URL)) {
            urlString = options.get(Option.URL);
        }

        if (options.containsKey(Option.USERNAME)) {
            username = options.get(Option.USERNAME);
        }

        if (options.containsKey(Option.PASSWORD)) {
            password = options.get(Option.PASSWORD);
            if (password == null) {
                promptForPassword = true;
            } else {
                promptForPassword = false;
            }
        }

        if (username == null && (password != null || promptForPassword)) {
            throw new ClientException("A username must be provided if a password is provided or will be prompted for",
                    1, HelpDisplay.SHOW_HELP);
        }

        if (promptForPassword) {
            char[] passwordChars = getConsole().readPassword("Password:");
            if (passwordChars == null) {
                throw new ClientException("No password provided", 1, HelpDisplay.NO_HELP);
            }
            password = new String(passwordChars);
        }

        if (urlString == null) {
            throw new ClientException(MISSING_URL, 1, HelpDisplay.SHOW_HELP);
        }

        // Check that the URL is at least valid, otherwise it doesn't seem to get
        // checked until a request is made to the repository. Checking it here allows
        // a better/more helpful error message
        try {
            new URL(urlString);
        } catch (MalformedURLException e) {
            throw new ClientException(INVALID_URL + urlString, 1, HelpDisplay.NO_HELP, e);
        }

        LoginInfoEntry loginInfoEntry = new LoginInfoEntry(username, password, "0", urlString);
        return loginInfoEntry;
    }

    /**
     * Get the console, or throw a suitable exception if it cannot be opened.
     *
     * @throws ClientException if the console cannot be opened
     */
    private Console getConsole() throws ClientException {
        Console console = System.console();
        if (console == null) {
            throw new ClientException("Failed to open an interactive prompt", 1, HelpDisplay.NO_HELP);
        }
        return console;
    }

}
