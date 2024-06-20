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

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.lars.upload.cli.ClientException.HelpDisplay;
import com.ibm.ws.massive.esa.MassiveEsa;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBackendRequestFailureException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceDeletionException;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;
import com.ibm.ws.repository.strategies.writeable.AddThenDeleteStrategy;

public class Main {

    static final String CONNECTION_PROBLEM = "There was a problem connecting to the repository: ";
    static final String MISSING_URL = "The repository url must be provided, either as an argument or in a configuration file.";
    static final String INVALID_URL = "The supplied url is not valid: ";
    static final String NO_IDS_FOR_DELETE = "No asset IDs were supplied.";
    static final String ASSET_NOT_FOUND = "Asset not found in repository.";
    static final String SERVER_ERROR = "The repository server returned an error.";
    static final String NO_FILES = "No files to upload. The files to upload must be provided as arguments.";

    private static Pattern versionPattern = Pattern.compile("productVersion=\"?([0-9\\.+]+)");

    private Map<Option, String> options;
    private Action action;

    private final InputStream input;
    private final PrintStream output;

    /** Filter that only accepts .esa files */
    private static final FileFilter ESA_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file == null)
                return false;

            String name = file.getName();
            return !file.isDirectory() && name != null && name.endsWith(".esa");
        }
    };

    /**
     * All logic here should be delegated to run, to allow for easier testing
     */
    public static void main(String[] args) {
        Main main = new Main(System.in, System.out);

        try {
            main.run(args);
        } catch (ClientException e) {
            System.exit(e.getReturnCode());
        }

        System.exit(0);
    }

    public Main(InputStream in, PrintStream out) {
        this.input = in;
        this.output = out;
    }

    /**
     * Effectively a delegate of main, to allow for testing.
     *
     * @param args
     * @throws ClientException
     */
    public void run(String[] args) throws ClientException {

        try {
            List<String> remainingArgs = readActionAndOptions(args);

            switch (action) {
                case UPLOAD:
                    doUpload(remainingArgs);
                    break;
                case DELETE:
                    doDelete(remainingArgs);
                    break;
                case FIND:
                    doFind(remainingArgs);
                    break;
                case FIND_AND_DELETE:
                    // doDelete has conditional code checking for the FIND_AND_DELETE action
                    doDelete(remainingArgs);
                    break;
                case LISTALL:
                    doListAll(remainingArgs);
                    break;
                case HELP:
                    showHelp(remainingArgs);
                    break;
                default:
                    showHelp(remainingArgs);
                    break;
            }
        } catch (ClientException e) {
            if (e.getHelpDisplay() == HelpDisplay.SHOW_HELP) {
                new Help(output).printMessageAndGlobalUsage(e.getMessage());
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
    public static String getInvokedName() {
        return System.getenv("INVOKED");
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

        // Allows options to be passed before actions. Considers the action that comes first.
        String actionString = args[0];
        int skipAction = 0;
        for (int i = 0; i < args.length; i++) {
            String tempActionString = args[i].startsWith("--") ? args[i].substring(2) : args[i];
            if (Action.getByArgument(tempActionString) != null) {
                skipAction = i;
                actionString = args[i];
                this.action = Action.getByArgument(args[i]);
                break;
            }
        }

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
        for (int i = 0; i < args.length; i++) {
            if (skipAction == i) {
                continue;
            }
            String arg = args[i];

            if (keepProcessingOptions && arg.equals("--")) {
                keepProcessingOptions = false;
            } else if (keepProcessingOptions && arg.startsWith("--")) {
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
            } else {
                nonOptionArgs.add(arg);
            }
        }

        return nonOptionArgs;
    }

    /**
     * Prints a help message to the output stream, either a global message (if remainingArgs is
     * empty) or a help message for a particular command (if specified in remainingArgs).
     */
    private void showHelp(List<String> remainingArgs) {

        Help help = new Help(output);

        if (remainingArgs.size() == 0 ||
            remainingArgs.size() > 1) {
            help.printGlobalUsage();
        } else {
            Action action = Action.getByArgument(remainingArgs.get(0));
            if (action != null) {
                switch (action) {
                    case HELP:
                    case UPLOAD:
                    case DELETE:
                    case LISTALL:
                    case FIND:
                    case FIND_AND_DELETE:
                        help.printCommandUsage(action.getUsage(), action.getHelpDetail());
                        help.printGlobalOptions();
                        break;
                    default:
                        help.printGlobalUsage();
                        break;
                }
            } else {
                help.printGlobalUsage();
            }
        }
    }

    /**
     * Uploads a list of ESAs.
     * <p>
     * This method will print error messages and exit the program if there is an error.
     *
     * @param remainingArgs a list of file paths to ESAs which should be uploaded.
     */
    private void doUpload(List<String> remainingArgs) throws ClientException {
        RepositoryConnection repoConnection = createRepoConnection();
        List<File> files = new ArrayList<File>();
        for (String arg : remainingArgs) {

            File argFile = new File(arg);
            // If we encounter a directory then add its contents
            if (argFile.isDirectory()) {
                File[] directoryContents = argFile.listFiles(ESA_FILTER);
                if (directoryContents != null) {
                    files.addAll(Arrays.asList(directoryContents));
                }
            } else {
                files.add(argFile);
            }
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
                // We don't expect we'd ever hit this case but, if we do (due to a logic error eg in ESA_FILTER)
                // then bomb out
                throw new ClientException("File " + file.toString() + " is a directory", 1, HelpDisplay.NO_HELP);
            }
        }

        MassiveEsa uploader;

        try {
            uploader = new MassiveEsa(repoConnection);
        } catch (RepositoryException ex) {
            throw new ClientException("An error occurred while connecting to the repository: " + ex.getMessage(), 1, HelpDisplay.NO_HELP, ex);
        }

        int size = files.size();
        for (int i = 0; i < size; i++) {
            File file = files.get(i);
            try {
                output.print((i + 1) + " of " + size + ": Uploading " + file.toString() + " ... ");
                List<RepositoryResource> deletedResources = new ArrayList<>();
                AddThenDeleteStrategy uploadStrategy = new AddThenDeleteStrategy(State.PUBLISHED, State.PUBLISHED, true, null, deletedResources);
                uploader.addEsasToMassive(Collections.singleton(file), uploadStrategy);

                // Did this upload operation cause us to delete one or more existing assets?
                if (deletedResources.size() > 1) {
                    // This is an unusual case: we replaced more than one existing (duplicate) assets
                    output.println("done, replacing multiple duplicate assets:");
                    for (RepositoryResource deletedResource : deletedResources) {
                        output.println(resourceToString(deletedResource));
                    }
                } else if (deletedResources.size() == 1) {
                    // More common case: we replaced one asset. Effectively, we are updating that asset.
                    output.println("done, replacing existing asset " + resourceToString(deletedResources.get(0)));
                } else {
                    // Most common case... we didn't replace anything and just
                    // uploaded this new asset
                    output.println("done");
                }
            } catch (RepositoryException ex) {
                if (!file.getPath().endsWith(".esa")) {
                    throw new ClientException("\nAn error occurred while uploading " + file.toString() + ": "
                                              + "file does not appear to be an esa file.", 1, HelpDisplay.NO_HELP, ex);
                } else
                    throw new ClientException("\nAn error occurred while uploading " + file.toString() + ": " + ex.getMessage(), 1, HelpDisplay.NO_HELP, ex);
            }
        }
    }

    private void doListAll(List<String> params) throws ClientException {

        RepositoryConnection repoConnection = createRepoConnection();
        Collection<? extends RepositoryResource> assets = null;
        try {
            assets = repoConnection.getAllResources();
        } catch (RepositoryBackendException e) {
            throw new ClientException("An error was recieved from the repository: " + e.getMessage(), 1, HelpDisplay.NO_HELP, e);
        }

        output.println("Listing all assets in the repository:");

        printAssets(assets);

    }

    /**
     * @param assets
     */
    private void printAssets(Collection<? extends RepositoryResource> assets) {
        if (assets.size() == 0) {
            output.println("No assets found in repository");
            return;
        }

        printTabbed("Asset ID", "Asset Type", "Liberty Version", "Asset Name");

        for (RepositoryResource resource : assets) {
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

            printTabbed(resource.getId(), type, appliesTo, name);
        }
    }

    void printTabbed(String id, String type, String appliesTo, String name) {
        output.format("%-30.30s | %-15.15s | %-15.15s | %s%n", id, type, appliesTo, name);
    }

    private List<String> doFind(List<String> remainingArgs) throws ClientException {

        RepositoryConnection repoConnection = createRepoConnection();
        Collection<? extends RepositoryResource> assets = null;
        try {
            if (remainingArgs.size() > 0) {
                String searchString = remainingArgs.get(0);
                assets = repoConnection.findResources(searchString, null, null, null);
            } else {
                assets = repoConnection.getAllResources();
            }
        } catch (RepositoryBackendException e) {
            throw new ClientException("An error was recieved from the repository: " + e.getMessage(), 1, HelpDisplay.NO_HELP, e);
        }

        if (options.containsKey(Option.NAME)) {
            String name = options.get(Option.NAME);
            Iterator<? extends RepositoryResource> i = assets.iterator();
            while (i.hasNext()) {
                String assetName = i.next().getName();
                if (assetName == null || !!!assetName.contains(name)) {
                    i.remove();
                }
            }
        }

        if (action == Action.FIND_AND_DELETE) {
            List<String> assetIds = new ArrayList<String>(assets.size());
            for (RepositoryResource resource : assets) {
                assetIds.add(resource.getId());
            }
            return assetIds;
        }

        printAssets(assets);
        return null;
    }

    private void doDelete(List<String> remainingArgs) throws ClientException {
        RepositoryConnection repoConnection = createRepoConnection();

        if (remainingArgs.size() == 0 && !!!options.containsKey(Option.FIND_DELETE)) {
            throw new ClientException(NO_IDS_FOR_DELETE, 1, HelpDisplay.SHOW_HELP);
        }

        if (action == Action.FIND_AND_DELETE) {
            remainingArgs = doFind(remainingArgs);
        }

        BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));

        for (String id : remainingArgs) {
            RepositoryResourceWritable toDelete = null;
            try {
                toDelete = (RepositoryResourceWritable) repoConnection.getResource(id);
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
                throw new ClientException("Asset " + id + " not deleted. " + SERVER_ERROR + e.getMessage(), 1, HelpDisplay.NO_HELP, e);

            } catch (RepositoryBackendException e) {
                // Anything else is probably some kind of connection problem, so ditch out
                throw new ClientException("Asset " + id + " not deleted. " + CONNECTION_PROBLEM + e.getMessage(), 1, HelpDisplay.NO_HELP, e);
            }

            if ((action == Action.FIND_AND_DELETE) && !!!options.containsKey(Option.NO_PROMPTS)) {
                output.println("Delete asset " + toDelete.getId() + " " + toDelete.getName() + " (y/N)?");
                try {
                    if (!!!("y".equalsIgnoreCase(inputReader.readLine()))) {
                        continue;
                    }
                } catch (IOException e) {
                    throw new ClientException(e.getMessage(), 1, HelpDisplay.NO_HELP, e);
                }
            }

            try {
                toDelete.delete();
            } catch (RepositoryResourceDeletionException | RepositoryBackendException e) {
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
     * Creates a RepositoryConnection object from the options on the command line
     *
     * @return
     */
    private RepositoryConnection createRepoConnection() throws ClientException {
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
            throw new ClientException("A username must be provided if a password is provided or will be prompted for", 1, HelpDisplay.SHOW_HELP);
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

        RestRepositoryConnection connection = new RestRepositoryConnection(username, password, "0", urlString);
        return connection;
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

    /**
     * Converts this resource into a string that can be displayed to the user and which should be
     * useful when presented in the context of "this resource replaced that one when uploaded."
     */
    private String resourceToString(RepositoryResource resource) {

        // At present, we only support features. This code will need to be
        // revisited in order to upload other types of resources
        if (!(resource instanceof EsaResource)) {
            throw new IllegalArgumentException("This method only supports resources of type ESAResource. Type was: " + resource.getClass().getName() + " was supplied.");
        }

        EsaResource esaResource = (EsaResource) resource;

        return String.format("%s type=%s, appliesTo=%s, version=%s, provideFeature=%s",
                             esaResource.getName(),
                             esaResource.getType(),
                             esaResource.getAppliesTo(),
                             esaResource.getVersion(),
                             esaResource.getProvideFeature());
    }
}
