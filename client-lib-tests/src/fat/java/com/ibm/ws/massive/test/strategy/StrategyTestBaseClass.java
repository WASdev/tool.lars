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

package com.ibm.ws.massive.test.strategy;

import static com.ibm.ws.lars.testutils.BasicChecks.populateResource;
import static com.ibm.ws.lars.testutils.FatUtils.FAT_REPO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.RepositoryFixture;
import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.RepositoryException;
import com.ibm.ws.massive.resources.MassiveResource;
import com.ibm.ws.massive.resources.MassiveResource.AttachmentType;
import com.ibm.ws.massive.resources.MassiveResource.State;
import com.ibm.ws.massive.resources.MassiveResource.Type;
import com.ibm.ws.massive.resources.RepositoryResourceException;
import com.ibm.ws.massive.resources.SampleResource;
import com.ibm.ws.massive.resources.UpdateInPlaceStrategy;
import com.ibm.ws.massive.resources.UploadStrategy;

public abstract class StrategyTestBaseClass {

    @Rule
    public RepositoryFixture repo = FAT_REPO;
    protected LoginInfoEntry _loginInfoEntry = repo.getLoginInfoEntry();
    protected SampleResource _testRes;

    protected final static File resourceDir = new File("resources");

    @Before
    public void beforeTest() throws RepositoryException, URISyntaxException {
        _testRes = createAsset();
    }

    @Test
    @Ignore
    // needs update assets
    public void checkLifeCycleOnUpdate() throws RepositoryBackendException, RepositoryResourceException {
        int featured = 0;
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        for (State startingState : State.values()) {
            for (State targetState : getValidTargetStates(startingState)) {
                _testRes.moveToState(startingState);
                // Increment featured weight to force an update
                featured++;
                _testRes.setFeaturedWeight("" + featured);
                _testRes.uploadToMassive(createStrategy(targetState, targetState));
                SampleResource readBack = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
                System.out.println("checking " + startingState + " was move to state " + targetState);
                assertEquals("Make sure state was set to target state", targetState, readBack.getState());

            }
        }
    }

    protected abstract UploadStrategy createStrategy(State ifMatching, State ifNoMatching);

    protected State[] getValidTargetStates(State initial) {
        return State.values();
    }

    protected void checkSame(MassiveResource resource1, MassiveResource resource2, MassiveResource resource3, boolean IdsSame, boolean resourceSame) throws RepositoryBackendException {
        assertTrue("Resource2 should be equivalent to resource3", resource2.equivalent(resource3));
        assertTrue("Resource2 should be equivalent to resource1", resource2.equivalent(resource1));
        if (IdsSame) {
            assertTrue("The id's of the resource 1 and 2 should be the same", resource2.get_id().equals(resource1.get_id()));;
        } else {
            assertFalse("The id's of the resource 1 and 2 should be different", resource2.get_id().equals(resource1.get_id()));;
        }
        if (resourceSame) {
            assertTrue("Resource2 should be equal to resource1", resource2.equals(resource1));
        } else {
            assertFalse("Resource2 should not be equal to resource1", resource2.equals(resource1));
        }
        assertEquals("There should only be one resource in the repo", 1, MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry)).size());
    }

    protected void checkUpdated(MassiveResource resource1, MassiveResource resource2, MassiveResource resource3, boolean IdsSame) throws RepositoryBackendException {
        assertTrue("Resource2 should be equivalent to resource3", resource2.equivalent(resource3));
        assertFalse("The read back resource should be different to the one we read back before", resource2.equivalent(resource1));
        if (IdsSame) {
            assertTrue("The id's of the resource 1 and 2 should be the same", resource2.get_id().equals(resource1.get_id()));;
        } else {
            assertFalse("The id's of the resource 1 and 2 should be different", resource2.get_id().equals(resource1.get_id()));;
        }
        assertEquals("There should only be one resource in the repo", 1, MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry)).size());
    }

    protected SampleResource createAsset() throws RepositoryException, URISyntaxException {
        // Build the resource
        final SampleResource sampleRes = new SampleResource(_loginInfoEntry);
        populateResource(sampleRes);
        sampleRes.setType(Type.PRODUCTSAMPLE);

        sampleRes.addAttachment(new File(resourceDir, "license_enus.txt"), AttachmentType.LICENSE);
        sampleRes.addAttachment(new File(resourceDir, "TestAttachment.txt"), AttachmentType.DOCUMENTATION);
        return sampleRes;
    }

}
