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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.strategies.writeable.AddNewStrategy;
import com.ibm.ws.repository.strategies.writeable.AddThenDeleteStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

@RunWith(Parameterized.class)
public class AddThenDeleteStrategyTest extends StrategyTestBaseClass {

    public AddThenDeleteStrategyTest(RepositoryFixture fixture) {
        super(fixture);
    }

    @Test
    public void testAddingToRepoUsingAddThenDeleteStrategyStrategy() throws RepositoryBackendException, RepositoryResourceException {
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Make sure there has not been a new asset uploaded (this strategy will mean an upload will
        // always create a new asset, hence new id which would make equiv fail).
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResourceImpl readBack2 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkSame(readBack, readBack2, _testRes, true, true);

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResourceImpl readBack3 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkUpdated(readBack2, readBack3, _testRes, false);
    }

    @Test
    public void testAddingToRepoUsingAddThenDeleteStrategyWithForceReplace() throws RepositoryBackendException, RepositoryResourceException {
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Make sure there has not been a new asset uploaded (this strategy will mean an upload will
        // always create a new asset, hence new id which would make equiv fail).
        _testRes.uploadToMassive(new AddThenDeleteStrategy(State.DRAFT, State.DRAFT, true));
        SampleResourceImpl readBack2 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkSame(readBack, readBack2, _testRes, false, false);

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResourceImpl readBack3 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkUpdated(readBack2, readBack3, _testRes, false);
    }

    @Test
    public void testAddingToRepoUsingAddThenDeleteStrategyWithMatchingResource() throws RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResourceImpl readBack2 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack2.equivalent(_testRes));
        assertTrue("The read back resource should be equivalent to the previous resource one we put in", readBack2.equivalent(readBack));
        assertEquals("There should be 2 resources in the repo", 2, repoConnection.getAllResourcesWithDupes().size());

        _testRes.uploadToMassive(new AddThenDeleteStrategy(null, State.DRAFT, true, readBack2));
        SampleResourceImpl readBack3 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack2.equivalent(_testRes));
        assertTrue("The read back resource should be equivalent to the previous resource one we put in", readBack2.equivalent(readBack));
        Collection<? extends RepositoryResource> all = repoConnection.getAllResourcesWithDupes();
        assertEquals("There should be 2 resources in the repo", 2, all.size());
        for (RepositoryResource r : all) {
            String resId = r.getId();
            assertTrue("The resource id (" + resId + ") should be either the same as readBack (" + readBack.getId() +
                       ") or readBack3 (" + readBack3.getId() + ") but not same as readBack2 (" + readBack2.getId() + ")",
                       resId.equals(readBack.getId()) || resId.equals(readBack3.getId()));
        }
    }

    @Test
    public void testDeleteMutliple() throws RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Add a second match
        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResourceImpl readBack2 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        Collection<? extends RepositoryResource> all = repoConnection.getAllResourcesWithDupes();
        assertEquals("There should be 2 resources in the repo", 2, all.size());

        _testRes.setFeaturedWeight("8");
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResourceImpl readBack3 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkUpdated(readBack2, readBack3, _testRes, false);
        all = repoConnection.getAllResourcesWithDupes();
        assertEquals("There should be 1 resource in the repo", 1, all.size());
        assertTrue("The resource in massive should be the same as the one we read back after the update", readBack3.equivalentWithoutAttachments(all.iterator().next()));
    }

    @Test
    public void testDeleteDuplicates() throws RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Add a second match
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResourceImpl readBack2 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        Collection<? extends RepositoryResource> all = repoConnection.getAllResourcesWithDupes();
        assertEquals("There should be 2 resources in the repo", 2, all.size());

        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResourceImpl readBack3 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        all = repoConnection.getAllResourcesWithDupes();
        assertEquals("There should be 1 resource in the repo", 1, all.size());
        assertTrue("The resource in massive should be the same as the one we read back after the update", readBack3.equivalentWithoutAttachments(all.iterator().next()));
        String id3 = readBack3.getId();
        assertTrue("We should have done an update on one of the two original assets and deleted the other one", (id3.equals(readBack.getId()) || id3.equals(readBack2.getId())));
    }

    @Override
    protected UploadStrategy createStrategy(State ifMatching, State ifNoMatching) {
        return new AddThenDeleteStrategy(ifMatching, ifNoMatching, false);
    }

}
