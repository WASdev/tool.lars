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
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;

public class MainTest {

    /**
     * Simple test to check that help is output if no arguments are given
     */
    @Test
    public void testRun() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayOutputStream ebaos = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(baos); PrintStream error = new PrintStream(ebaos)) {
            Main main = new Main(output);
            try {
                main.run(new String[] {});
            } catch (ClientException e) {
                assertEquals("Unexpected exception message", "No options were given", e.getMessage());
                String outputString = baos.toString();
                assertTrue("The expected help output wasn't produced, was:\n" + outputString, outputString.contains("Usage: java"));
                int numberOfLines = FatUtils.countLines(outputString);
                assertEquals("The help output didn't contain the expected number of lines:\n" + outputString,
                             46, numberOfLines);

                String errorOutput = ebaos.toString();
                assertEquals("No output was expected to stderr", "", errorOutput);
                return;
            }
            fail("The expected client exception was not thrown");
        }

    }
}
