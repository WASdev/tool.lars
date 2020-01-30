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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;

public class MainTest {

    private static class MainRunner {
        public String stdout;

        private final String expectedExceptionMessage;
        private final int expectedStdoutLines;

        public MainRunner(String expectedExceptionMessage, int expectedStdoutLines) {
            this.expectedExceptionMessage = expectedExceptionMessage;
            this.expectedStdoutLines = expectedStdoutLines;
        }

        public void run(String... args) throws ClientException {
            InputStream input = new ByteArrayInputStream(new byte[0]);
            ByteArrayOutputStream stdoutBAOS = new ByteArrayOutputStream();
            try (PrintStream output = new PrintStream(stdoutBAOS)) {
                Main main = new Main(input, output);
                Exception exception = null;
                try {
                    main.run(args);
                } catch (ClientException e) {
                    if (expectedExceptionMessage != null) {
                        exception = e;
                        assertEquals("Unexpected exception message", expectedExceptionMessage, e.getMessage());
                    } else {
                        throw e;
                    }
                }
                if (expectedExceptionMessage != null && exception == null) {
                    fail("The expected client exception was not thrown");
                }

                stdout = stdoutBAOS.toString();

                assertEquals("The help output didn't contain the expected number of lines:\n" + stdout,
                             expectedStdoutLines,
                             FatUtils.countLines(stdout));
            }
        }
    }

    /**
     * Simple test to check that help is output if no arguments are given
     *
     * @throws ClientException
     */
    @Test
    public void testRun() throws ClientException {
        MainRunner runner = new MainRunner("No options were given", 47);
        runner.run();
        assertThat(runner.stdout, containsString("Usage: java -jar larsClient.jar action [options] [arguments]"));
    }

    @Test
    public void shouldPrintHelpMessageIfHelpOptionSpecified() throws ClientException {
        MainRunner runner = new MainRunner(null, 45);
        runner.run("--help");
        assertThat(runner.stdout, containsString("Usage: java -jar larsClient.jar action [options] [arguments] ..."));
        assertThat(runner.stdout, containsString("Show help for larsClient."));
        assertThat(runner.stdout, containsString("Upload ESAs to the repository."));
        assertThat(runner.stdout, containsString("Delete one or more assets from the repository, specified by id."));
        assertThat(runner.stdout, containsString("List all the assets currently in the repository"));
    }

    @Test
    public void shouldPrintHelpMessageIfHelpInvokedOnNonExistentComment() throws ClientException {
        // there is no such command as "cheese"
        MainRunner runner = new MainRunner(null, 45);
        runner.run("--help", "cheese");
    }

    @Test
    public void shouldPrintHelpForHelp() throws ClientException {
        MainRunner runner = new MainRunner(null, 27);
        runner.run("--help", "help");
        assertThat(runner.stdout, containsString("Show help for larsClient."));
    }

    @Test
    public void shouldPrintHelpForUpload() throws ClientException {
        MainRunner runner = new MainRunner(null, 31);
        runner.run("--help", "upload");
        assertThat(runner.stdout, containsString("Uploads one or more features to a LARS server."));
    }

    @Test
    public void shouldPrintHelpForDelete() throws ClientException {
        MainRunner runner = new MainRunner(null, 27);
        runner.run("--help", "delete");
        assertThat(runner.stdout, containsString("Delete one or more assets from the repository, specified by id."));
    }

    @Test
    public void shouldPrintHelpForFindAndDelete() throws ClientException {
        MainRunner runner = new MainRunner(null, 30);
        runner.run("--help", "findAndDelete");
        assertThat(runner.stdout,
                   containsString("Finds and deletes all the assets in the repository"));
    }

    @Test
    public void shouldPrintHelpForListAll() throws ClientException {
        MainRunner runner = new MainRunner(null, 27);
        runner.run("--help", "listAll");
        assertThat(runner.stdout, containsString("List all the assets currently in the repository"));
    }
}
