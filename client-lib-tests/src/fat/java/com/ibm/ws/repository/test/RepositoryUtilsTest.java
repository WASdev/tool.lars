/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
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
package com.ibm.ws.repository.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

@RunWith(Parameterized.class)
public class RepositoryUtilsTest {

    private final RepositoryConnection repoConnection;

    @Rule
    public final RepositoryFixture fixture;

    @Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return FatUtils.getRestFixtureParameters();
    }

    public RepositoryUtilsTest(RepositoryFixture fixture) {
        this.fixture = fixture;
        repoConnection = fixture.getAdminConnection();
    }

    /**
     * Tests that the test repository is available
     */
    @Test
    public void testRepositoryAvailable() {
        assertTrue("The test repository should be available", repoConnection.isRepositoryAvailable());
    }

    /**
     * Tests that the test repository is available
     *
     * @throws RequestFailureException
     * @throws IOException
     */
    @Test
    public void testRepositoryStatusOK() throws IOException, RequestFailureException {

        // An exception is thrown if this fails
        repoConnection.checkRepositoryStatus();
    }

    /**
     * Tests that an invalid repository is not available
     */
    @Test
    public void testInvalidRepositoryNotAvailable() {
        RestRepositoryConnection invalidLoginInfo = new RestRepositoryConnection("I", "don't", "exist", "http://dont.exist.com");
        //RestRepositoryConnection invalidLoginInfo = new RestRepositoryConnection("I", "don't", "exist", "http://dont252qmadjf842sgs7842d.exist.com");
        assertFalse("An invalid test repository should not be available", invalidLoginInfo.isRepositoryAvailable());
    }

    @Test
    public void testInvalidRepositoryThrowsException() throws IOException, RequestFailureException {
        RestRepositoryConnection invalidLoginInfo = new RestRepositoryConnection("I", "don't", "exist", "http://dont.exist.com");
        //RestRepositoryConnection invalidLoginInfo = new RestRepositoryConnection("I", "don't", "exist", "http://dont252qmadjf842sgs7842d.exist.com");
        try {
            invalidLoginInfo.checkRepositoryStatus();
            fail("Should not have been able to reach here, repository status should have thrown an exception");
        } catch (IOException io) {
            // expected
        }
    }

}
