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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.RepositoryFixture;
import com.ibm.ws.lars.testutils.TestProcess;
import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.resources.EsaResource;
import com.ibm.ws.massive.resources.MassiveResource;
import com.ibm.ws.massive.resources.MassiveResource.LicenseType;
import com.ibm.ws.massive.resources.MassiveResource.State;

/**
 * Test the upload action of the command line client
 */
public class UploadFatTest {

    @Rule
    public RepositoryFixture repoServer = FatUtils.FAT_REPO;

    @Test
    public void testRealEsa() throws Exception {
        LoginInfo loginInfo = repoServer.getLoginInfo();
        String esaPath = "resources/com.ibm.websphere.appserver.adminCenter-1.0.esa";
        File esaFile = new File(esaPath);

        TestProcess tp = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                       "upload",
                                                       "--url=" + FatUtils.SERVER_URL,
                                                       "--username=" + repoServer.getLoginInfoEntry().getUserId(),
                                                       "--password=" + repoServer.getLoginInfoEntry().getPassword(),
                                                       esaPath));
        tp.run();

        tp.assertReturnCode(0);
        assertEquals("Incorrect resource count", 1, MassiveResource.getAllResources(loginInfo).size());
        assertEquals("Incorrect feature count", 1, EsaResource.getAllFeatures(loginInfo).size());

        EsaResource resource = EsaResource.getAllFeatures(loginInfo).iterator().next();
        assertEquals("Incorrect state", State.PUBLISHED, resource.getState());
        assertEquals("Incorrect license type", LicenseType.UNSPECIFIED, resource.getLicenseType());
        assertEquals("Incorrect size", esaFile.length(), resource.getMainAttachmentSize());

        tp.assertOutputContains("done");
    }

    @Test
    public void testUserEsa() throws Exception {
        LoginInfo loginInfo = repoServer.getLoginInfo();
        String esaPath = "resources/userFeature.esa";
        File esaFile = new File(esaPath);

        TestProcess tp = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                       "upload",
                                                       "--url=" + FatUtils.SERVER_URL,
                                                       "--username=" + repoServer.getLoginInfoEntry().getUserId(),
                                                       "--password=" + repoServer.getLoginInfoEntry().getPassword(),
                                                       esaPath));
        tp.run();

        tp.assertReturnCode(0);
        assertEquals("Incorrect resource count", 1, MassiveResource.getAllResources(loginInfo).size());
        assertEquals("Incorrect feature count", 1, EsaResource.getAllFeatures(loginInfo).size());

        EsaResource resource = EsaResource.getAllFeatures(loginInfo).iterator().next();
        assertEquals("Incorrect state", State.PUBLISHED, resource.getState());
        assertEquals("Incorrect license type", null, resource.getLicenseType());
        assertEquals("Incorrect size", esaFile.length(), resource.getMainAttachmentSize());

        tp.assertOutputContains("done");
    }

    @Test
    public void testMultipleEsas() throws Exception {
        LoginInfo loginInfo = repoServer.getLoginInfo();

        TestProcess tp = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                       "upload",
                                                       "--url=" + FatUtils.SERVER_URL,
                                                       "--username=" + repoServer.getLoginInfoEntry().getUserId(),
                                                       "--password=" + repoServer.getLoginInfoEntry().getPassword(),
                                                       "resources/com.ibm.websphere.appserver.adminCenter-1.0.esa",
                                                       "resources/userFeature.esa"));
        tp.run();

        tp.assertReturnCode(0);
        assertEquals("Incorrect resource count", 2, MassiveResource.getAllResources(loginInfo).size());
        assertEquals("Incorrect feature count", 2, EsaResource.getAllFeatures(loginInfo).size());
    }

    @Test
    public void testMissingEsa() throws Exception {
        LoginInfo loginInfo = repoServer.getLoginInfo();

        TestProcess tp = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                       "upload",
                                                       "--url=" + FatUtils.SERVER_URL,
                                                       "--username=" + repoServer.getLoginInfoEntry().getUserId(),
                                                       "--password=" + repoServer.getLoginInfoEntry().getPassword(),
                                                       "resources/userFeature.esa",
                                                       "resources/somethingInvalid"));
        tp.run();

        tp.assertReturnCode(1);

        // Use a File object to get the right path separators for the OS
        String fileName = new File("resources/somethingInvalid").toString();

        tp.assertOutputContains("File " + fileName + " can't be read");

        assertEquals("Incorrect resource count", 0, MassiveResource.getAllResources(loginInfo).size());
        assertEquals("Incorrect feature count", 0, EsaResource.getAllFeatures(loginInfo).size());
    }

    @Test
    public void testAbsolutePath() throws Exception {
        LoginInfo loginInfo = repoServer.getLoginInfo();

        File esaFile = new File("resources/userFeature.esa");

        TestProcess tp = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                       "upload",
                                                       "--url=" + FatUtils.SERVER_URL,
                                                       "--username=" + repoServer.getLoginInfoEntry().getUserId(),
                                                       "--password=" + repoServer.getLoginInfoEntry().getPassword(),
                                                       esaFile.getAbsolutePath()));
        tp.run();

        tp.assertReturnCode(0);
        assertEquals("Incorrect resource count", 1, MassiveResource.getAllResources(loginInfo).size());
        assertEquals("Incorrect feature count", 1, EsaResource.getAllFeatures(loginInfo).size());

        EsaResource resource = EsaResource.getAllFeatures(loginInfo).iterator().next();
        assertEquals("Incorrect state", State.PUBLISHED, resource.getState());
        assertEquals("Incorrect license type", null, resource.getLicenseType());
        assertEquals("Incorrect size", esaFile.length(), resource.getMainAttachmentSize());

        tp.assertOutputContains("done");
    }

    @SuppressWarnings("unchecked")
    // needed to call generic varagrs method :-/
    @Test
    public void shouldUploadDirectory() throws Exception {
        LoginInfo loginInfo = repoServer.getLoginInfo();

        // Note: at the time of writing, dirWith3ESAs has a subdirectory that contains
        // ESAs. We do *not* expect the contents of the subdirectory to be uploaded
        // (and the test will fail if they *are* uploaded).
        TestProcess tp = new TestProcess(Arrays.asList(FatUtils.SCRIPT,
                                                       "upload",
                                                       "--url=" + FatUtils.SERVER_URL,
                                                       "--username=" + repoServer.getLoginInfoEntry().getUserId(),
                                                       "--password=" + repoServer.getLoginInfoEntry().getPassword(),
                                                       "resources/dirWith3ESAs"));
        tp.run();

        tp.assertReturnCode(0);
        assertEquals("Incorrect resource count", 3, MassiveResource.getAllResources(loginInfo).size());
        Collection<EsaResource> features = EsaResource.getAllFeatures(loginInfo);
        assertEquals("Incorrect feature count", 3, features.size());

        assertThat("Should upload directory contents",
                   features,
                   containsInAnyOrder(hasProperty("name", equalTo("com.ibm.ws.test.simple")),
                                      hasProperty("name", equalTo("com.ibm.ws.test.userFeature")),
                                      hasProperty("name", equalTo("com.ibm.ws.test.versionrange"))));
    }
}
