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

package com.ibm.ws.repository.connections.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;

/**
 *
 */
public class RepositoryConnectionTest {

    @Test
    public void testMassiveRepoLocation() {
        String repoURL = "htttp://blah";
        RestRepositoryConnection lie = new RestRepositoryConnection("a", "b", "c", repoURL);
        RepositoryResourceImpl mr = new SampleResourceImpl(lie);
        assertEquals("The repo url in the resource is not the one we set",
                     repoURL, mr.getRepositoryConnection().getRepositoryLocation());
    }

    @Test
    public void testDirectoryRepoLocation() {
        File root = new File("C:/root");
        DirectoryRepositoryConnection dirCon = new DirectoryRepositoryConnection(root);
        RepositoryResourceImpl mr = new SampleResourceImpl(dirCon);
        assertEquals("The repo url in the resource is not the one we set",
                     root.getAbsolutePath(), mr.getRepositoryConnection().getRepositoryLocation());
    }
}
