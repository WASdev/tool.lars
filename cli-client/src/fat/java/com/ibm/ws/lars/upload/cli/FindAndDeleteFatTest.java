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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.TestProcess;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;

/**
 * Test the find actions of the command line client
 */
public class FindAndDeleteFatTest {

    @Rule
    public RepositoryFixture repoServer = FatUtils.FAT_REPO;

    private RestRepositoryConnection repoConnection;

    @Before
    public void setUp() {
        repoConnection = (RestRepositoryConnection) repoServer.getAdminConnection();
    }

    @Test
    public void testDelete() throws IOException, RepositoryBackendException {
        RepositoryConnectionList connectionList = new RepositoryConnectionList(repoConnection);

        String esaPath = "resources/com.ibm.websphere.appserver.adminCenter-1.0.esa";
        TestProcess tp = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                       "upload",
                                                       "--url=" + FatUtils.SERVER_URL,
                                                       "--username=" + repoConnection.getUserId(),
                                                       "--password=" + repoConnection.getPassword(),
                                                       esaPath));
        tp.run();
        tp.assertReturnCode(0);
        assertEquals("Incorrect resource count", 1, connectionList.getAllResources().size());
        assertEquals("Incorrect feature count", 1, connectionList.getAllFeatures().size());
        tp.assertOutputContains("done");

        // Add another asset
        String esa2Path = "resources/userFeature.esa";
        TestProcess upload2 = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                            "upload",
                                                            "--url=" + FatUtils.SERVER_URL, esa2Path,
                                                            "--username=" + repoConnection.getUserId(),
                                                            "--password=" + repoConnection.getPassword()));
        upload2.run();
        upload2.assertReturnCode(0);
        assertEquals("Incorrect resource count", 2, connectionList.getAllResources().size());
        assertEquals("Incorrect feature count", 2, connectionList.getAllFeatures().size());
        upload2.assertOutputContains("done");

        // List assets, should now show 2 assets
        TestProcess list2Process = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                 "listAll",
                                                                 "--url=" + FatUtils.SERVER_URL,
                                                                 "--username=" + repoConnection.getUserId(),
                                                                 "--password=" + repoConnection.getPassword()));
        list2Process.run();
        String list2output = list2Process.getOutput();
        int lineCount2 = FatUtils.countLines(list2output);

        assertEquals("Output had the wrong number of lines\n" + list2output, 4, lineCount2);
        findFeatureId(list2output, "Admin Center");
        list2Process.assertReturnCode(0);

        // Delete asset
        TestProcess deleteProcess = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                  "findAndDelete",
                                                                  "--noPrompts",
                                                                  "admin",
                                                                  "--url=" + FatUtils.SERVER_URL,
                                                                  "--username=" + repoConnection.getUserId(),
                                                                  "--password=" + repoConnection.getPassword()));
        deleteProcess.run();
        String deleteOutput = deleteProcess.getOutput();
        int lineCount = FatUtils.countLines(deleteOutput);

        assertEquals("Output had the wrong number of lines\n" + deleteOutput, 1, lineCount);
        assertTrue("Asset not deleted\n" + deleteOutput + "\n" + list2output, deleteOutput.contains("Deleted asset "));
        deleteProcess.assertReturnCode(0);

        // List assets, should now show 1 assets
        TestProcess list3Process = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                 "listAll",
                                                                 "--url=" + FatUtils.SERVER_URL,
                                                                 "--username=" + repoConnection.getUserId(),
                                                                 "--password=" + repoConnection.getPassword()));
        list3Process.run();
        String list3output = list3Process.getOutput();
        int lineCount3 = FatUtils.countLines(list3output);

        assertEquals("Output had the wrong number of lines\n" + list3output, 3, lineCount3);
        findFeatureId(list3output, "com.ibm.ws.test.userFeature");
        list3Process.assertReturnCode(0);

    }

    @Test
    public void testDeleteWithPrompts() throws IOException, RepositoryBackendException {
        RepositoryConnectionList connectionList = new RepositoryConnectionList(repoConnection);

        String esaPath = "resources/com.ibm.websphere.appserver.adminCenter-1.0.esa";
        TestProcess tp = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                       "upload",
                                                       "--url=" + FatUtils.SERVER_URL,
                                                       "--username=" + repoConnection.getUserId(),
                                                       "--password=" + repoConnection.getPassword(),
                                                       esaPath));
        tp.run();
        tp.assertReturnCode(0);
        assertEquals("Incorrect resource count", 1, connectionList.getAllResources().size());
        assertEquals("Incorrect feature count", 1, connectionList.getAllFeatures().size());
        tp.assertOutputContains("done");

        // Add another asset
        String esa2Path = "resources/userFeature.esa";
        TestProcess upload2 = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                            "upload",
                                                            "--url=" + FatUtils.SERVER_URL, esa2Path,
                                                            "--username=" + repoConnection.getUserId(),
                                                            "--password=" + repoConnection.getPassword()));
        upload2.run();
        upload2.assertReturnCode(0);
        assertEquals("Incorrect resource count", 2, connectionList.getAllResources().size());
        assertEquals("Incorrect feature count", 2, connectionList.getAllFeatures().size());
        upload2.assertOutputContains("done");

        // List assets, should now show 2 assets
        TestProcess list2Process = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                 "listAll",
                                                                 "--url=" + FatUtils.SERVER_URL,
                                                                 "--username=" + repoConnection.getUserId(),
                                                                 "--password=" + repoConnection.getPassword()));
        list2Process.run();
        String list2output = list2Process.getOutput();
        int lineCount2 = FatUtils.countLines(list2output);

        assertEquals("Output had the wrong number of lines\n" + list2output, 4, lineCount2);
        findFeatureId(list2output, "Admin Center");
        list2Process.assertReturnCode(0);

        // Delete asset but answer "n" to the deletion prompt
        TestProcess deleteProcess = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                  "findAndDelete",
                                                                  "admin",
                                                                  "--url=" + FatUtils.SERVER_URL,
                                                                  "--username=" + repoConnection.getUserId(),
                                                                  "--password=" + repoConnection.getPassword()), "n" + System.lineSeparator());

        deleteProcess.run();

        String deleteOutput = deleteProcess.getOutput();
        int lineCount = FatUtils.countLines(deleteOutput);

        assertEquals("Output had the wrong number of lines\n" + deleteOutput, 1, lineCount);
        assertTrue("No delete prompt\n" + deleteOutput + "\n" + list2output, deleteOutput.contains("Delete asset "));
        deleteProcess.assertReturnCode(0);

        // List assets, should still show 2 assets
        TestProcess list3Process = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                 "listAll",
                                                                 "--url=" + FatUtils.SERVER_URL,
                                                                 "--username=" + repoConnection.getUserId(),
                                                                 "--password=" + repoConnection.getPassword()));
        list3Process.run();
        String list3output = list3Process.getOutput();
        int lineCount3 = FatUtils.countLines(list3output);

        assertEquals("Output had the wrong number of lines\n" + list3output, 4, lineCount3);
        findFeatureId(list3output, "com.ibm.ws.test.userFeature");
        findFeatureId(list2output, "Admin Center");
        list3Process.assertReturnCode(0);

        // Delete asset answering "y" to the deletion prompt
        TestProcess deleteProcess2 = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                   "findAndDelete",
                                                                   "admin",
                                                                   "--url=" + FatUtils.SERVER_URL,
                                                                   "--username=" + repoConnection.getUserId(),
                                                                   "--password=" + repoConnection.getPassword()), "y" + System.lineSeparator());

        deleteProcess2.run();

        String delete2Output = deleteProcess2.getOutput();
        int lineCount4 = FatUtils.countLines(delete2Output);

        assertEquals("Output had the wrong number of lines\n" + delete2Output, 2, lineCount4);
        assertTrue("No delete prompt\n" + delete2Output + "\n" + delete2Output, delete2Output.contains("Delete asset "));
        assertTrue("Asset not deleted\n" + delete2Output + "\n" + delete2Output, delete2Output.contains("Deleted asset "));
        deleteProcess.assertReturnCode(0);

        // List assets, should now show 1 asset
        TestProcess list4Process = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                 "listAll",
                                                                 "--url=" + FatUtils.SERVER_URL,
                                                                 "--username=" + repoConnection.getUserId(),
                                                                 "--password=" + repoConnection.getPassword()));
        list4Process.run();
        String list4output = list4Process.getOutput();
        int lineCount5 = FatUtils.countLines(list4output);

        assertEquals("Output had the wrong number of lines\n" + list4output, 3, lineCount5);
        findFeatureId(list3output, "com.ibm.ws.test.userFeature");
        list3Process.assertReturnCode(0);
    }

    /**
     * Finds the id of a feature from the output of the list command, or fails the test if it can't
     * be found. Output from the list command looks something like:
     *
     * Listing all assets in the repository:<br>
     * Asset ID | Asset Type | Liberty Version | Asset Name<br>
     * 54be3a90469ba2513fea88e7 | Feature | 8.5.5.5 | Admin Center (adminCenter-1.0)
     *
     * where the version may be blank
     *
     * @param output - output of the list command
     * @param name - name of the feature to find
     * @return
     */
    private static String findFeatureId(String output, String name) {

        String regex = "(\\w+)\\s+\\|\\s+Feature\\s+\\|.+\\|\\s+" + name;
        Pattern idPattern = Pattern.compile(regex);
        Matcher idMatcher = idPattern.matcher(output);
        boolean result = idMatcher.find();
        assertTrue("Can't find id for feature " + name + " in output:\n" + output, result);
        String id = idMatcher.group(1);
        return id;
    }

}
