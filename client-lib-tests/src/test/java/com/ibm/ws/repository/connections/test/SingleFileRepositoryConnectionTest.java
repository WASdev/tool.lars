/*******************************************************************************
 * Copyright (c) 2017 IBM Corp.
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
package com.ibm.ws.repository.connections.test;

import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.repository.connections.SingleFileRepositoryConnection;
import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * Tests specific to the SingleFileRepositoryConnection
 */
public class SingleFileRepositoryConnectionTest {

    private static final File FILE = new File("testSingleFileRepo");

    @After
    public void cleanup() {
        if (FILE.exists()) {
            FILE.delete();
        }
    }

    @Test
    public void testNoFileCheckStatus() {
        SingleFileRepositoryConnection repo = new SingleFileRepositoryConnection(FILE);
        try {
            repo.checkRepositoryStatus();
            fail("Repository with no file reported available");
        } catch (Exception ex) {
            // Expected
        }
    }

    @Test
    public void testNoFileGetAll() {
        SingleFileRepositoryConnection repo = new SingleFileRepositoryConnection(FILE);
        try {
            repo.getAllResources();
            fail("No exception thrown");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testCreate() throws Exception {
        SingleFileRepositoryConnection repo = SingleFileRepositoryConnection.createEmptyRepository(FILE);
        repo.checkRepositoryStatus();
        assertThat(repo.getAllResources(), is(emptyCollectionOf(RepositoryResource.class)));

        // File should have been created, second repo should be valid
        SingleFileRepositoryConnection repo2 = new SingleFileRepositoryConnection(FILE);
        repo2.checkRepositoryStatus();
        assertThat(repo2.getAllResources(), is(emptyCollectionOf(RepositoryResource.class)));
    }

}
