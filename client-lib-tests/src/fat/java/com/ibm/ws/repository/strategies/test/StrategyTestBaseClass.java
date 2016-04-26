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
package com.ibm.ws.repository.strategies.test;

import static com.ibm.ws.lars.testutils.BasicChecks.populateResource;
import static com.ibm.ws.repository.common.enums.ResourceType.PRODUCTSAMPLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.strategies.writeable.UpdateInPlaceStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

@RunWith(Parameterized.class)
public abstract class StrategyTestBaseClass {

    protected SampleResourceImpl _testRes;

    protected final RepositoryConnection repoConnection;

    private final static File resourceDir = new File("resources");

    @Rule
    public RepositoryFixture fixture;

    @Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return FatUtils.getRestFixtureParameters();
    }

    public StrategyTestBaseClass(RepositoryFixture fixture) {
        this.fixture = fixture;
        this.repoConnection = fixture.getAdminConnection();
    }

    @Before
    public void beforeTest() throws RepositoryException, URISyntaxException {
        _testRes = createAsset();
    }

    @Test
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
                SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
                assertEquals("Make sure state was set to target state", targetState, readBack.getState());
            }
        }
    }

    protected abstract UploadStrategy createStrategy(State ifMatching, State ifNoMatching);

    protected State[] getValidTargetStates(State initial) {
        return State.values();
    }

    protected void checkSame(RepositoryResource res1, RepositoryResource res2, RepositoryResource res3, boolean IdsSame, boolean resourceSame) throws RepositoryBackendException {

        RepositoryResourceImpl resource1 = (RepositoryResourceImpl) res1;
        RepositoryResourceImpl resource2 = (RepositoryResourceImpl) res2;
        RepositoryResourceImpl resource3 = (RepositoryResourceImpl) res3;

        assertTrue("Resource2 should be equivalent to resource3", resource2.equivalent(resource3));
        assertTrue("Resource2 should be equivalent to resource1", resource2.equivalent(resource1));
        if (IdsSame) {
            assertTrue("The id's of the resource 1 and 2 should be the same", resource2.getId().equals(resource1.getId()));;
        } else {
            assertFalse("The id's of the resource 1 and 2 should be different", resource2.getId().equals(resource1.getId()));;
        }
        if (resourceSame) {
            assertTrue("Resource2 should be equal to resource1", resource2.equals(resource1));
        } else {
            assertFalse("Resource2 should not be equal to resource1", resource2.equals(resource1));
        }
        assertEquals("There should only be one resource in the repo", 1, new RepositoryConnectionList(repoConnection).getAllResources().size());
    }

    protected void checkUpdated(RepositoryResource res1, RepositoryResource res2, RepositoryResource res3, boolean IdsSame) throws RepositoryBackendException {

        RepositoryResourceImpl resource1 = (RepositoryResourceImpl) res1;
        RepositoryResourceImpl resource2 = (RepositoryResourceImpl) res2;
        RepositoryResourceImpl resource3 = (RepositoryResourceImpl) res3;

        assertTrue("Resource2 should be equivalent to resource3", resource2.equivalent(resource3));
        assertFalse("The read back resource should be different to the one we read back before", resource2.equivalent(resource1));
        if (IdsSame) {
            assertTrue("The id's of the resource 1 and 2 should be the same", resource2.getId().equals(resource1.getId()));;
        } else {
            assertFalse("The id's of the resource 1 and 2 should be different", resource2.getId().equals(resource1.getId()));;
        }
        assertEquals("There should only be one resource in the repo", 1, new RepositoryConnectionList(repoConnection).getAllResources().size());
    }

    protected SampleResourceImpl createAsset() throws RepositoryException, URISyntaxException {
        // Build the resource
        final SampleResourceImpl sampleRes = new SampleResourceImpl(repoConnection);
        populateResource(sampleRes);
        sampleRes.setType(PRODUCTSAMPLE);
        sampleRes.addAttachment(new File(resourceDir, "license_enus.txt"), AttachmentType.LICENSE);
        sampleRes.addAttachment(new File(resourceDir, "TestAttachment.txt"), AttachmentType.DOCUMENTATION);
        return sampleRes;
    }

}
