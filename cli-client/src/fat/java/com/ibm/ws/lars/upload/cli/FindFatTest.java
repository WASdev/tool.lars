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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;

/**
 * Test the find actions of the command line client
 */
public class FindFatTest {

    @Rule
    public RepositoryFixture repoServer = FatUtils.FAT_REPO;

    private RestRepositoryConnection repoConnection;

    @Before
    public void setUp() {
        repoConnection = (RestRepositoryConnection) repoServer.getAdminConnection();
    }

    @Test
    public void testFind() throws IOException, RepositoryBackendException {
        RepositoryConnectionList connectionList = new RepositoryConnectionList(repoConnection);

        String esaPath = "resources/com.ibm.websphere.appserver.adminCenter-1.0.esa";
        File esaFile = new File(esaPath);
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

        EsaResource resource = connectionList.getAllFeatures().iterator().next();
        assertEquals("Incorrect state", State.PUBLISHED, ((EsaResourceWritable) resource).getState());
        assertEquals("Incorrect license type", LicenseType.UNSPECIFIED, resource.getLicenseType());
        assertEquals("Incorrect size", esaFile.length(), resource.getMainAttachmentSize());

        // Find assets, should show just the uploaded above
        TestProcess findProcess = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                "find",
                                                                "",
                                                                "--url=" + FatUtils.SERVER_URL,
                                                                "--username=" + repoConnection.getUserId(),
                                                                "--password=" + repoConnection.getPassword()));
        findProcess.run();
        String output = findProcess.getOutput();

        assertFalse(output, output.contains("No assets found in repository"));
        assertTrue("More than one feature found: " + output, (output.indexOf("Feature") == output.lastIndexOf("Feature") && output.indexOf("Feature") != -1));
        int lineCount = FatUtils.countLines(output);
        assertEquals("Output had the wrong number of lines\n" + output, 2, lineCount);
        findFeatureId(output, "Admin Center");
        findProcess.assertReturnCode(0);

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
        TestProcess find2Process = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                 "find",
                                                                 "",
                                                                 "--url=" + FatUtils.SERVER_URL,
                                                                 "--username=" + repoConnection.getUserId(),
                                                                 "--password=" + repoConnection.getPassword()));
        find2Process.run();
        String list2output = find2Process.getOutput();
        int lineCount2 = FatUtils.countLines(list2output);
        assertEquals("Output had the wrong number of lines\n" + list2output, 3, lineCount2);
        findFeatureId(list2output, "Admin Center");
        find2Process.assertReturnCode(0);
    }

    @Test
    public void testFindWithName() throws IOException, RepositoryBackendException {
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
        upload2.assertOutputContains("done");

        // should now be two assets
        assertEquals("Incorrect resource count", 2, connectionList.getAllResources().size());
        assertEquals("Incorrect feature count", 2, connectionList.getAllFeatures().size());

        // Find assets, should show the two uploaded above
        TestProcess findProcess = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                "find",
                                                                "",
                                                                "--url=" + FatUtils.SERVER_URL,
                                                                "--username=" + repoConnection.getUserId(),
                                                                "--password=" + repoConnection.getPassword()));
        findProcess.run();
        String output = findProcess.getOutput();

        assertFalse(output, output.contains("No assets found in repository"));
        int lineCount = FatUtils.countLines(output);
        assertEquals("Output had the wrong number of lines\n" + output, 3, lineCount);
        findProcess.assertReturnCode(0);

        // Fine with name, should now show just the one asset
        TestProcess find2Process = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                 "find",
                                                                 "",
                                                                 "--name=Admin Center",
                                                                 "--url=" + FatUtils.SERVER_URL,
                                                                 "--username=" + repoConnection.getUserId(),
                                                                 "--password=" + repoConnection.getPassword()));
        find2Process.run();
        String list2output = find2Process.getOutput();
        int lineCount2 = FatUtils.countLines(list2output);

        assertEquals("Output had the wrong number of lines\n" + list2output, 2, lineCount2);
        assertTrue("More than one feature found: " + list2output, (list2output.indexOf("Feature") == list2output.lastIndexOf("Feature") && list2output.indexOf("Feature") != -1));
        findFeatureId(list2output, "Admin Center");
        find2Process.assertReturnCode(0);
    }

    @Test
    public void testFindSearchtSring() throws IOException, RepositoryBackendException {
        RepositoryConnectionList connectionList = new RepositoryConnectionList(repoConnection);

        String esaPath = "resources/com.ibm.websphere.appserver.adminCenter-1.0.esa";
        File esaFile = new File(esaPath);
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

        EsaResource resource = connectionList.getAllFeatures().iterator().next();
        assertEquals("Incorrect state", State.PUBLISHED, ((EsaResourceWritable) resource).getState());
        assertEquals("Incorrect license type", LicenseType.UNSPECIFIED, resource.getLicenseType());
        assertEquals("Incorrect size", esaFile.length(), resource.getMainAttachmentSize());

        // Find assets, should show just the uploaded above
        TestProcess findProcess = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                "find",
                                                                "admin",
                                                                "--url=" + FatUtils.SERVER_URL,
                                                                "--username=" + repoConnection.getUserId(),
                                                                "--password=" + repoConnection.getPassword()));
        findProcess.run();
        String output = findProcess.getOutput();

        assertFalse(output, output.contains("No assets found in repository"));
        assertTrue("More than one feature found: " + output, (output.indexOf("Feature") == output.lastIndexOf("Feature") && output.indexOf("Feature") != -1));
        int lineCount = FatUtils.countLines(output);
        assertEquals("Output had the wrong number of lines\n" + output, 2, lineCount);
        findFeatureId(output, "Admin Center");
        findProcess.assertReturnCode(0);

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
        TestProcess find2Process = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                                 "find",
                                                                 "admin",
                                                                 "--url=" + FatUtils.SERVER_URL,
                                                                 "--username=" + repoConnection.getUserId(),
                                                                 "--password=" + repoConnection.getPassword()));
        find2Process.run();
        String list2output = find2Process.getOutput();
        int lineCount2 = FatUtils.countLines(list2output);

        assertEquals("Output had the wrong number of lines\n" + list2output, 2, lineCount2);
        assertTrue("More than one feature found: " + output, (output.indexOf("Feature") == output.lastIndexOf("Feature") && output.indexOf("Feature") != -1));
        lineCount = FatUtils.countLines(output);
        assertEquals("Output had the wrong number of lines\n" + output, 2, lineCount);
        findFeatureId(output, "Admin Center");
        find2Process.assertReturnCode(0);
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
