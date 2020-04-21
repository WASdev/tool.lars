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

package com.ibm.ws.lars.testutils;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.concurrent.CountDownLatch;
import java.util.List;

/**
 * Launches a program, providing options for checking its output and return code.
 * <p>
 * Note there is no timeout and the output from the program is stored in a buffer so it is not
 * suitable for testing programs with a very large output or which may hang.
 */
public class TestProcess {

    private static final String NOT_YET_RUN = "Cannot check output before program has been run";
    private final ProcessBuilder processBuilder;
    private Process process;
    private String systemInput;
    private StringBuilder outputBuilder;
    private StringBuilder errorBuilder;
    private Integer returnCode;

    /**
     * Set up the TestProcess
     *
     * @param commandLine the command line (program followed by arguments) to execute
     */
    public TestProcess(List<String> commandLine) {
        processBuilder = new ProcessBuilder(commandLine);
        returnCode = null;
    }

    /**
     * Set up the TestProcess
     *
     * @param commandLine the command line (program followed by arguments) to execute
     * @param systemInput the system input to be read during execution of the process
     */
    public TestProcess(List<String> commandLine, String systemInput) {
        this(commandLine);
        this.systemInput = systemInput;
    }

    /**
     * Run the program passed in the constructor and wait for it to finish
     *
     * @throws IOException if any exceptions are thrown when starting or reading from the process
     */
    public void run() throws IOException {
        process = processBuilder.start();
        outputBuilder = new StringBuilder();
        errorBuilder = new StringBuilder();

        if (systemInput != null) {
            OutputStreamWriter osw = new OutputStreamWriter(process.getOutputStream());
            osw.write(systemInput);
            osw.flush();
        }

        CountDownLatch latch = new CountDownLatch (2);
        
        ProcessStreamReader outReader = new ProcessStreamReader(process.getInputStream(), outputBuilder, latch);
        new Thread(outReader).start();

        ProcessStreamReader errReader = new ProcessStreamReader(process.getErrorStream(), errorBuilder, latch);
        new Thread(errReader).start();

        while (true) {
            try {
                returnCode = process.waitFor();
                break;
            } catch (InterruptedException e) {
            }
        }
        
        try {
            // Although the process itself may have finished the threads that read the process may not have done yet
            latch.await();
        } catch (InterruptedException e) {
            // Just catch it and drop out
        }

        if (outReader.getException() != null) {
            throw outReader.getException();
        }

        if (errReader.getException() != null) {
            throw errReader.getException();
        }
    }

    /**
     * Assert that the program wrote the given string to stdout
     * <p>
     * This can only be called after calling <code>run</code>
     *
     * @param string the string to look for
     */
    public void assertOutputContains(String string) {
        if (returnCode == null) {
            fail(NOT_YET_RUN);
        }
        if (outputBuilder.indexOf(string) == -1) {
            fail("Program output did not contain " + string + "\n" + "Actual output:\n" + outputBuilder.toString());
        }
    }

    /**
     * Assert that the program wrote the given string to stderr
     * <p>
     * This can only be called after calling <code>run</code>
     *
     * @param string the string to look for
     */
    public void assertErrorContains(String string) {
        if (returnCode == null) {
            fail(NOT_YET_RUN);
        }
        if (errorBuilder.indexOf(string) == -1) {
            fail("Program error output did not contain " + string + "\n" + "Actual output:\n" + errorBuilder.toString());
        }
    }

    public String getOutput() {
        if (returnCode == null) {
            fail(NOT_YET_RUN);
        }

        return outputBuilder.toString();
    }

    /**
     * Assert that the program exited with the given return code
     * <p>
     * This can only be called after calling <code>run</code>
     *
     * @param returnCode the return code
     */
    public void assertReturnCode(int returnCode) {
        if (this.returnCode == null) {
            fail("Cannot check return code before program has been run");
        }
        if (this.returnCode.intValue() != returnCode) {
            fail("Incorrect return code, expected <" + returnCode + "> but was <" + this.returnCode + ">\n"
                 + "Program output:\n" + outputBuilder.toString() + "\n"
                 + "Program error output:\n" + errorBuilder.toString() + "\n");
        }
    }

    /**
     * Handles reading from an input stream into a string builder
     */
    private class ProcessStreamReader implements Runnable {
        private final Reader in;
        private final StringBuilder out;
        private IOException ex;
        private CountDownLatch latch;

        public ProcessStreamReader(InputStream in, StringBuilder out, CountDownLatch latch) {
            this.in = new BufferedReader(new InputStreamReader(in));
            this.out = out;
            this.latch = latch;
            ex = null;
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            try {
                char[] buffer = new char[1024];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    out.append(buffer, 0, length);
                }
            } catch (IOException e) {
                this.ex = e;
            } finally {
                latch.countDown();
            }
        }

        public IOException getException() {
            return ex;
        }
    }

}
