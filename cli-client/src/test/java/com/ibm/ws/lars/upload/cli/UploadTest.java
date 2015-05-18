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

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import org.junit.Test;

import com.ibm.ws.lars.upload.cli.ClientException.HelpDisplay;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.esa.MassiveEsa;
import com.ibm.ws.massive.resources.EsaResource;
import com.ibm.ws.massive.resources.UploadStrategy;

/**
 * Test the upload action
 */
public class UploadTest {

    /**
     * Mock uploader which captures the login info and the names of files which are uploaded.
     */
    public static class MockUploader extends MockUp<MassiveEsa> {
        private LoginInfoEntry loginInfoEntry = null;
        private final List<String> filesUploaded = new ArrayList<>();

        @Mock
        public void $init(LoginInfoEntry loginInfo) {
            this.loginInfoEntry = loginInfo;
        }

        @Mock
        public Collection<EsaResource> addEsasToMassive(Collection<File> esas, UploadStrategy strategy) {
            for (File file : esas) {
                filesUploaded.add(file.getName());
            }
            return null;
        }

        public LoginInfoEntry getLoginInfoEntry() {
            return loginInfoEntry;
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

    /**
     * Mock directory
     */
    public static class MockDirectory extends MockUp<File> {
        @Mock
        public boolean canRead(Invocation inv) {
            return true;
        }

        @Mock
        public boolean isDirectory(Invocation inv) {
            return true;
        }
    }

    @Test
    public void testNoFiles() {
        new MockUploader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new PrintStream(out));
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
    public void testDirectory() {
        new MockUploader();
        new MockDirectory();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new PrintStream(out));
        try {
            main.run(new String[] { "--upload", "--url=http://example.org", "TestFile.esa" });
            fail("ClientException not thrown");
        } catch (ClientException ex) {
            assertEquals("Wrong return code", 1, ex.getReturnCode());
            assertEquals("Wrong error message", "File TestFile.esa is a directory", ex.getMessage());
            assertEquals("Wrong help display", HelpDisplay.NO_HELP, ex.getHelpDisplay());
        }
    }

    @Test
    public void testNoUrl() {
        new MockUploader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new PrintStream(out));
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

        Main main = new Main(new PrintStream(out));
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

        Main main = new Main(new PrintStream(out));
        main.run(new String[] { "--upload", "--url=http://example.org", "TestFile.esa" });

        assertEquals("Wrong files uploaded", Arrays.asList("TestFile.esa"), uploader.getFilesUploaded());
        assertThat("Output incorrect", out.toString(), containsString("Uploading TestFile.esa ... done"));
    }

    @Test
    public void testMultipleUpload() throws ClientException {
        new MockFile();
        MockUploader uploader = new MockUploader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Main main = new Main(new PrintStream(out));
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

        Main main = new Main(new PrintStream(out));
        main.run(new String[] { "--upload", "--url=http://example.org", "--username=jbloggs", "--password=foobar", "TestFile.esa" });

        assertEquals("Wrong files uploaded", Arrays.asList("TestFile.esa"), uploader.getFilesUploaded());
        assertThat("Output incorrect", out.toString(), containsString("Uploading TestFile.esa ... done"));
        assertEquals("Wrong username", "jbloggs", uploader.getLoginInfoEntry().getUserId());
        assertEquals("Wrong password", "foobar", uploader.getLoginInfoEntry().getPassword());
        assertNull("Softlayer username incorrectly set", uploader.getLoginInfoEntry().getSoftlayerUserId());
        assertNull("Softlayer password incorrectly set", uploader.getLoginInfoEntry().getSoftlayerPassword());
        assertNull("Attachment username incorrectly set", uploader.getLoginInfoEntry().getAttachmentBasicAuthUserId());
        assertNull("Attachment password incorrectly set", uploader.getLoginInfoEntry().getAttachmentBasicAuthPassword());
        assertNotNull("ApiKey is null", uploader.getLoginInfoEntry().getApiKey());
    }

    /**
     * Test that we prompt on the command line for a password when requested.
     */
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
        Main main = new Main(new PrintStream(out));
        main.run(new String[] { "--upload", "--url=http://example.org", "--username=jbloggs", "--password", "TestFile.esa" });

        assertEquals("Wrong files uploaded", Arrays.asList("TestFile.esa"), uploader.getFilesUploaded());
        assertThat("Output incorrect", out.toString(), containsString("Uploading TestFile.esa ... done"));
        assertEquals("Wrong username", "jbloggs", uploader.getLoginInfoEntry().getUserId());
        assertEquals("Wrong password", "thePassw0rd", uploader.getLoginInfoEntry().getPassword());
        assertEquals("Wrong number of calls to readPassword", 1, mockConsole.count);
        assertNull("Softlayer username incorrectly set", uploader.getLoginInfoEntry().getSoftlayerUserId());
        assertNull("Softlayer password incorrectly set", uploader.getLoginInfoEntry().getSoftlayerPassword());
        assertNull("Attachment username incorrectly set", uploader.getLoginInfoEntry().getAttachmentBasicAuthUserId());
        assertNull("Attachment password incorrectly set", uploader.getLoginInfoEntry().getAttachmentBasicAuthPassword());
        assertNotNull("ApiKey is null", uploader.getLoginInfoEntry().getApiKey());

        // test that we don't prompt when the password is provided
        main.run(new String[] { "--upload", "--url=http://example.org", "--username=jbloggs", "--password=foobar", "TestFile.esa" });
        // call count should not have increased here
        assertEquals("Wrong number of calls to readPassword", 1, mockConsole.count);

        // test that we don't prompt when the password is not mentioned
        main.run(new String[] { "--upload", "--url=http://example.org", "TestFile.esa" });
        // call count should not have increased here
        assertEquals("Wrong number of calls to readPassword", 1, mockConsole.count);
    }
}
