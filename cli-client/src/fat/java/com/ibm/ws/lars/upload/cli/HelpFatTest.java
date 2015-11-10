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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.TestProcess;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;

/**
 * Basic test of the 'help' action of the command line client
 */
public class HelpFatTest {

    @Rule
    public RepositoryFixture repoServer = FatUtils.FAT_REPO;

    @Test
    public void testHelpInvalidOption() throws IOException {
        TestProcess helpProcess = new TestProcess(Arrays.asList(FatUtils.SCRIPT, "help"));
        helpProcess.run();
        helpProcess.assertReturnCode(0);

        // The test is run using the script/bat file, so the help output should list the options with
        // no leading '--' . This is looking for (good):
        //
        //     delete
        //          Delete one or more assets from the repository, specified by asset ID
        //
        // instead of (bad, only when run using java -jar):
        //
        //   --delete
        //          Delete one or more assets from the repository, specified by asset ID
        String helpRegex = System.lineSeparator() + "\\s+delete" + System.lineSeparator() + "\\s+Delete one or more";
        Pattern helpPattern = Pattern.compile(helpRegex);
        Matcher helpMatcher = helpPattern.matcher(helpProcess.getOutput());
        assertTrue("The format of the help output was unexpected:\n" + helpProcess.getOutput(), helpMatcher.find());
    }
}
