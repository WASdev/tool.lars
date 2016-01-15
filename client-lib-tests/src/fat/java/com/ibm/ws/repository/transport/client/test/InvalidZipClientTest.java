/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
package com.ibm.ws.repository.transport.client.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import com.ibm.ws.repository.transport.client.AbstractFileClient;
import com.ibm.ws.repository.transport.client.ZipClient;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

public class InvalidZipClientTest {

    private final static File resourceDir = new File("resources");

    @Test
    public void testRepositoryStatusNoZipFound() throws Exception {
        AbstractFileClient noZipClient = new ZipClient(new File("doesnotexist.zip"));
        try {
            noZipClient.checkRepositoryStatus();
            fail("An exception should have been thrown as the repo should not be reachable");
        } catch (FileNotFoundException e) {
            // We should get a FileNotFoundException from checkRepositoryStatus, any other exception should be
            // allowed to propagate back up to cause the test to fail
        }
    }

    @Test
    public void testRepositoryStatusInvalidZipFile() throws IOException, RequestFailureException {
        AbstractFileClient invalidFile = new ZipClient(new File(resourceDir, "TestAttachment.txt"));
        try {
            invalidFile.checkRepositoryStatus();
            fail("An exception should have been thrown as the repo should not be reachable");
        } catch (IOException e) {
            assertTrue("Wrong exception messages,  expected \"error in opening zip file\" but got \"" + e.getMessage() + "\"",
                       e.getMessage().contains("error in opening zip file"));
        }
    }
}
