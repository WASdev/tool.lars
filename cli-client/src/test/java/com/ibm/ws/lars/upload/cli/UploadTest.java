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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import org.junit.Test;

import com.ibm.ws.lars.upload.cli.ClientException.HelpDisplay;
import com.ibm.ws.massive.esa.MassiveEsa;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.strategies.writeable.AddThenDeleteStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

/**
 * Test the upload action
 */
public class UploadTest {

    /**
     * Mock uploader which captures the login info and the names of files which are uploaded.
     */
    public static class MockUploader extends MockUp<MassiveEsa> {
        private RepositoryConnection repoConnection = null;
        private final List<String> filesUploaded = new ArrayList<>();

        @Mock
        public void $init(RepositoryConnection repoConnection) {
            this.repoConnection = repoConnection;
        }

        @Mock
        public Collection<EsaResource> addEsasToMassive(Collection<File> esas, UploadStrategy strategy) {
            for (File file : esas) {
                filesUploaded.add(file.getName());
            }
            return null;
        }

        public RepositoryConnection getLoginInfoEntry() {
            return repoConnection;
        }

        public List<String> getFilesUploaded() {
            return filesUploaded;
        }

    }

    /**
     * Mock file that reports that any file exists, except for "InvalidFile.esa"
     */
    public static class MockFile extends MockUp<File> {
        @Mock
        public boolean canRead(Invocation inv) {
            System.out.println("File is " + inv.<File> getInvokedInstance().getName());
            if (inv.<File> getInvokedInstance().getName().equals("InvalidFile.esa")) {
                return false;
            } else {
                return true;
            }
        }
    }

    public static class MockEmptyDirectory extends MockUp<File> {
        @Mock
        public boolean canRead(Invocation inv) {
            return true;
        }

        @Mock
        public boolean isDirectory(Invocation inv) {
            return true;
        }
    }

    /**
     * Mocks up a directory structure that looks like this:
     *
     * <pre>
     * superDirectory
     *   cheese.esa
     *   ston.ese
     *   I_am_not_a_feature.txt
     *   subDirectory
     *     dontUploadThis.esa
     * </pre>
     */
    public static class MockDirectoryWithFiles extends MockUp<File> {
        @Mock
        public boolean canRead(Invocation inv) {
            return true;
        }

        @Mock
        public boolean isDirectory(Invocation inv) {
            String name = ((File) inv.getInvokedInstance()).getName();
            return "superDirectory".equals(name) ||
                   "subDirectory".equals(name);
        }

        @Mock
        public File[] listFiles(Invocation inv, FileFilter filter) {
            final List<File> files;
            File thiz = (File) inv.getInvokedInstance();
            String name = thiz.getName();

            if ("superDirectory".equals(name)) {
                files = Arrays.asList(new File(thiz, "cheese.esa"),
                                      new File(thiz, "ston.esa"),
                                      new File(thiz, "I_am_not_a_feature.txt"),
                                      new File(thiz, "subDirectory"));
            } else if ("subDirectory".equals(name)) {
                files = Arrays.asList(new File(thiz, "dontUploadThis.esa"));
            } else {
                return null;
            }

            List<File> filteredFiles = new ArrayList<File>();
            for (File f : files) {
                if (filter.accept(f)) {
                    filteredFiles.add(f);
                }
            }
            return filteredFiles.toArray(new File[filteredFiles.size()]);
        }
    }

    /**
     * Mocked up AddThenDeleteStrategy that claims to have deleted the specified number of
     * resources.
     */
    public static class MockAddThenDeleteStrategy extends MockUp<AddThenDeleteStrategy> {

        private final int numDeletedResources;

        public MockAddThenDeleteStrategy(int numDeletedResources) {
            this.numDeletedResources = numDeletedResources;
        }

        @Mock
        public void $init(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace,
                          RepositoryResourceImpl matchingResource, List<RepositoryResource> deletedResources) {
            if (deletedResources != null) {
                MockEsaResource mockESA = new MockEsaResource();
                for (int i = 0; i < numDeletedResources; i++) {
                    deletedResources.add(mockESA);
                }
            }
        }

    }

    public static class MockEsaResource extends EsaResourceImpl {
        MockEsaResource() {
            super(null);
        }

        @Mock
        public String getName() {
            return "a-fake-name";
        }

        @Mock
        public ResourceType getType() {
            return ResourceType.FEATURE;
        }

        @Mock
        public String getAppliesTo() {
            return "fake-applies-to";
        }

        @Mock
        public String getVersion() {
            return "1.2.3.4";
        }

        @Mock
        public String getProvideFeature() {
            return "a-fake-feature";
        }
    }

    @Test
    public void testNoFiles() {
        new MockUploader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        try {
            main.run(new String[] { "--upload", "--url=http://example.org/" });
            fail("ClientException not thrown");
        } catch (ClientException ex) {
            assertEquals("Wrong return code", 1, ex.getReturnCode());
            assertEquals("Wrong error message", Main.NO_FILES, ex.getMessage());
            assertEquals("Wrong help display", HelpDisplay.SHOW_HELP, ex.getHelpDisplay());
        }
    }

    @Test
    public void testEmptyDirectoryShouldResultInNoFilesError() {
        new MockUploader();
        new MockEmptyDirectory();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        try {
            main.run(new String[] { "--upload", "--url=http://example.org", "TestFile.esa" });
            fail("ClientException not thrown");
        } catch (ClientException ex) {
            assertEquals("Wrong return code", 1, ex.getReturnCode());
            assertEquals("Wrong error message", Main.NO_FILES, ex.getMessage());
            assertEquals("Wrong help display", HelpDisplay.SHOW_HELP, ex.getHelpDisplay());
        }
    }

    @Test
    public void testNoUrl() {
        new MockUploader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        try {
            main.run(new String[] { "--upload", "TestFile.esa" });
            fail("ClientException not thrown");
        } catch (ClientException ex) {
            assertEquals("Wrong return code", 1, ex.getReturnCode());
            assertEquals("Wrong error message", "The repository url must be provided, either as an argument or in a configuration file.", ex.getMessage());
            assertEquals("Wrong help display", HelpDisplay.SHOW_HELP, ex.getHelpDisplay());
        }
    }

    @Test
    public void testInvalidFile() {
        new MockUploader();
        new MockFile();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        try {
            main.run(new String[] { "--upload", "--url=http://example.org", "TestFile.esa", "InvalidFile.esa" });
            fail("ClientException not thrown");
        } catch (ClientException ex) {
            assertEquals("Wrong return code", 1, ex.getReturnCode());
            assertEquals("Wrong error message", "File InvalidFile.esa can't be read", ex.getMessage());
            assertEquals("Wrong help display", HelpDisplay.NO_HELP, ex.getHelpDisplay());
        }
    }

    @Test
    public void testUpload() throws ClientException {
        new MockFile();
        MockUploader uploader = new MockUploader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        main.run(new String[] { "--upload", "--url=http://example.org", "TestFile.esa" });

        assertEquals("Wrong files uploaded", Arrays.asList("TestFile.esa"), uploader.getFilesUploaded());
        assertThat("Output incorrect", out.toString(), containsString("Uploading TestFile.esa ... done"));
    }

    @Test
    public void testMultipleUpload() throws ClientException {
        new MockFile();
        MockUploader uploader = new MockUploader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        main.run(new String[] { "--upload", "--url=http://example.org", "TestFile.esa", "TestFile2.esa", "TestFile3.esa" });

        assertEquals("Wrong files uploaded", Arrays.asList("TestFile.esa", "TestFile2.esa", "TestFile3.esa"), uploader.getFilesUploaded());
        assertThat("Output incorrect", out.toString(), containsString("Uploading TestFile.esa ... done"));
        assertThat("Output incorrect", out.toString(), containsString("Uploading TestFile2.esa ... done"));
        assertThat("Output incorrect", out.toString(), containsString("Uploading TestFile3.esa ... done"));
    }

    @Test
    public void testCredentials() throws ClientException {
        new MockFile();
        MockUploader uploader = new MockUploader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        main.run(new String[] { "--upload", "--url=http://example.org", "--username=jbloggs", "--password=foobar", "TestFile.esa" });

        RestRepositoryConnection repoConnection = (RestRepositoryConnection) uploader.getLoginInfoEntry();

        assertEquals("Wrong files uploaded", Arrays.asList("TestFile.esa"), uploader.getFilesUploaded());
        assertThat("Output incorrect", out.toString(), containsString("Uploading TestFile.esa ... done"));
        assertEquals("Wrong username", "jbloggs", repoConnection.getUserId());
        assertEquals("Wrong password", "foobar", repoConnection.getPassword());
        assertNull("Softlayer username incorrectly set", repoConnection.getSoftlayerUserId());
        assertNull("Softlayer password incorrectly set", repoConnection.getSoftlayerPassword());
        assertNull("Attachment username incorrectly set", repoConnection.getAttachmentBasicAuthUserId());
        assertNull("Attachment password incorrectly set", repoConnection.getAttachmentBasicAuthPassword());
        assertNotNull("ApiKey is null", repoConnection.getApiKey());
    }

    /**
     * Test that we prompt on the command line for a password when requested.
     
     * This does not work.  The problem is that mockConsole.getMockInstance() no longer exists,
     * so we can't get Main.getConsole() to return the MOCKED console.  Creating the MockUp does 
     * not work, because the System.console instance already exists, so never gets constructed,
     * so the faked class is never applied.
     * Making MockConsole a subclass of Concole would work, except that Console is final.
     * Also, Console has only private constructors, so there is no way we can make a fake one either.
     * Commented out most of this, until we can figure a work around...
     
    @Test
    public void testPasswordPrompt() throws ClientException {
        new MockFile();
        MockUploader uploader = new MockUploader();

        // Mock console that returns a fixed password
        class MockConsole extends MockUp<Console> {
            public int count = 0;

            @Mock
            public char[] readPassword(String fmt, Object... args) {
                count++;
                return "thePassw0rd".toCharArray();
            }
        }
        final MockConsole mockConsole = new MockConsole();

        new MockUp<Main>() {
            @Mock
            private Console getConsole() {
                return mockConsole.getMockInstance();
            }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // test with a password prompt
        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        main.run(new String[] { "--upload", "--url=http://example.org", "--username=jbloggs", "--password", "TestFile.esa" });

        RestRepositoryConnection repoConnection = (RestRepositoryConnection) uploader.getLoginInfoEntry();

        assertEquals("Wrong files uploaded", Arrays.asList("TestFile.esa"), uploader.getFilesUploaded());
        assertThat("Output incorrect", out.toString(), containsString("Uploading TestFile.esa ... done"));
        assertEquals("Wrong username", "jbloggs", repoConnection.getUserId());
        assertEquals("Wrong password", "thePassw0rd", repoConnection.getPassword());
        assertEquals("Wrong number of calls to readPassword", 1, mockConsole.count);
        assertNull("Softlayer username incorrectly set", repoConnection.getSoftlayerUserId());
        assertNull("Softlayer password incorrectly set", repoConnection.getSoftlayerPassword());
        assertNull("Attachment username incorrectly set", repoConnection.getAttachmentBasicAuthUserId());
        assertNull("Attachment password incorrectly set", repoConnection.getAttachmentBasicAuthPassword());
        assertNotNull("ApiKey is null", repoConnection.getApiKey());

        // test that we don't prompt when the password is provided
        main.run(new String[] { "--upload", "--url=http://example.org", "--username=jbloggs", "--password=foobar", "TestFile.esa" });
        // call count should not have increased here
        assertEquals("Wrong number of calls to readPassword", 1, mockConsole.count);

        // test that we don't prompt when the password is not mentioned
        main.run(new String[] { "--upload", "--url=http://example.org", "TestFile.esa" });
        // call count should not have increased here
        assertEquals("Wrong number of calls to readPassword", 1, mockConsole.count);
    }
    */

    @Test
    public void testShouldUploadContentsOfDirectory() throws Exception {
        MockUploader uploader = new MockUploader();
        new MockDirectoryWithFiles();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(out));

        main.run(new String[] { "--upload", "--url=http://example.org", "superDirectory" });

        assertEquals("Wrong files uploaded", Arrays.asList("cheese.esa", "ston.esa"), uploader.getFilesUploaded());
        assertThat("Output incorrect", out.toString(), containsString("Uploading superDirectory" + File.separator + "cheese.esa ... done"));
        assertThat("Output incorrect", out.toString(), containsString("Uploading superDirectory" + File.separator + "ston.esa ... done"));
    }

    @Test
    public void testShouldGiveProgressIndication() throws Exception {
        new MockUploader();
        new MockDirectoryWithFiles();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        Main main = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(outStream));

        main.run(new String[] { "--upload", "--url=http://example.org", "superDirectory", "i-am-a-feature.esa" });

        String output = outStream.toString();

        assertThat("Output incorrect", output, containsString("1 of 3: Uploading superDirectory" + File.separator + "cheese.esa"));
        assertThat("Output incorrect", output, containsString("2 of 3: Uploading superDirectory" + File.separator + "ston.esa"));
        assertThat("Output incorrect", output, containsString("3 of 3: Uploading i-am-a-feature.esa"));
    }

    @Test
    public void shouldGiveIndicationIfExistingResourceReplaced() throws Exception {
        new MockUploader();

        new MockDirectoryWithFiles();

        new MockAddThenDeleteStrategy(1);

        new MockEsaResource();

        // The mocks that we define above make the uploader think that an existing ESA
        // has been deleted when we upload our new resource.
        // We verify that we get a message telling us that the pre-existing file was deleted
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Main main2 = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(outStream));
        main2.run(new String[] { "--upload", "--url=http://example.org", "superDirectory/cheese.esa" });

        assertThat("Output incorrect", outStream.toString(),
                   containsString("1 of 1: Uploading superDirectory" + File.separator + "cheese.esa ... done, replacing existing asset " +
                                  "a-fake-name type=FEATURE, appliesTo=fake-applies-to, " +
                                  "version=1.2.3.4, provideFeature=a-fake-feature"));
    }

    @Test
    public void shouldGiveIndicationIfMultipleExistingResourceReplaced() throws Exception {
        new MockUploader();

        new MockDirectoryWithFiles();

        new MockAddThenDeleteStrategy(2);

        new MockEsaResource();

        // The mocks that we define above make the uploader think that two existing ESAs
        // have been deleted when we upload our new resource.
        // We verify that we get a message telling us that the pre-existing files were deleted
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Main main2 = new Main(new ByteArrayInputStream(new byte[0]), new PrintStream(outStream));
        main2.run(new String[] { "--upload", "--url=http://example.org", "superDirectory/cheese.esa" });

        assertEquals("Output incorrect",
                     "1 of 1: Uploading superDirectory" + File.separator + "cheese.esa ... done, replacing multiple duplicate assets:" +
                             System.getProperty("line.separator") +
                             "a-fake-name type=FEATURE, appliesTo=fake-applies-to, version=1.2.3.4, provideFeature=a-fake-feature" +
                             System.getProperty("line.separator") +
                             "a-fake-name type=FEATURE, appliesTo=fake-applies-to, version=1.2.3.4, provideFeature=a-fake-feature" +
                             System.getProperty("line.separator"),
                     outStream.toString());
    }
}
