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

package com.ibm.ws.massive.test;

import static com.ibm.ws.lars.testutils.FatUtils.FAT_REPO;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.RepositoryFixture;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryUtils;

/**
 * Tests for {@link RepositoryUtils}
 */
public class RepositoryUtilsTest {

    @Rule
    public RepositoryFixture repo = FAT_REPO;

    /**
     * Tests that the test repository is available
     */
    @Test
    public void testRepositoryAvailable() {
        assertTrue("The test repository should be available", RepositoryUtils.isRepositoryAvailable(repo.getLoginInfoEntry()));
    }

    /**
     * Tests that an invalid repository is not available
     */
    @Test
    public void testInvalidRepositoryNotAvailable() {
        LoginInfoEntry invalidLoginInfo = new LoginInfoEntry("I", "don't", "exist", "http://dont.exist.com");
        assertFalse("An invalid test repository should not be available", RepositoryUtils.isRepositoryAvailable(invalidLoginInfo));
    }

}
