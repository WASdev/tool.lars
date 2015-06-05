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

import static com.ibm.ws.lars.testutils.ReflectionTricks.getAllResourcesWithDupes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.resources.AddNewStrategy;
import com.ibm.ws.massive.resources.AddThenDeleteStrategy;
import com.ibm.ws.massive.resources.MassiveResource;
import com.ibm.ws.massive.resources.MassiveResource.State;
import com.ibm.ws.massive.resources.RepositoryResourceException;
import com.ibm.ws.massive.resources.SampleResource;
import com.ibm.ws.massive.resources.UploadStrategy;

public class AddThenDeleteStrategyTest extends StrategyTestBaseClass {

    public AddThenDeleteStrategyTest() throws FileNotFoundException,
        IOException {
        super();
    }

    @Test
    public void testAddingToRepoUsingAddThenDeleteStrategyStrategy() throws RepositoryBackendException, RepositoryResourceException {
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResource readBack = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Make sure there has not been a new asset uploaded (this strategy will mean an upload will
        // always create a new asset, hence new id which would make equiv fail).
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResource readBack2 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        checkSame(readBack, readBack2, _testRes, true, true);

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResource readBack3 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        checkUpdated(readBack2, readBack3, _testRes, false);
    }

    @Test
    public void testAddingToRepoUsingAddThenDeleteStrategyWithForceReplace() throws RepositoryBackendException, RepositoryResourceException {
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResource readBack = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Make sure there has not been a new asset uploaded (this strategy will mean an upload will
        // always create a new asset, hence new id which would make equiv fail).
        _testRes.uploadToMassive(new AddThenDeleteStrategy(State.DRAFT, State.DRAFT, true));
        SampleResource readBack2 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        checkSame(readBack, readBack2, _testRes, false, false);

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResource readBack3 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        checkUpdated(readBack2, readBack3, _testRes, false);
    }

    @Test
    public void testAddingToRepoUsingAddThenDeleteStrategyWithMatchingResource() throws RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResource readBack = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResource readBack2 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack2.equivalent(_testRes));
        assertTrue("The read back resource should be equivalent to the previous resource one we put in", readBack2.equivalent(readBack));
        assertEquals("There should be 2 resources in the repo", 2, getAllResourcesWithDupes(_loginInfoEntry).size());

        _testRes.uploadToMassive(new AddThenDeleteStrategy(null, State.DRAFT, true, readBack2));
        SampleResource readBack3 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack2.equivalent(_testRes));
        assertTrue("The read back resource should be equivalent to the previous resource one we put in", readBack2.equivalent(readBack));
        Collection<MassiveResource> all = getAllResourcesWithDupes(_loginInfoEntry);
        assertEquals("There should be 2 resources in the repo", 2, all.size());
        for (MassiveResource r : all) {
            String resId = r.get_id();
            assertTrue("The resource id (" + resId + ") should be either the same as readBack (" + readBack.get_id() +
                       ") or readBack3 (" + readBack3.get_id() + ") but not same as readBack2 (" + readBack2.get_id() + ")",
                       resId.equals(readBack.get_id()) || resId.equals(readBack3.get_id()));
        }
    }

    @Test
    public void testDeleteMutliple() throws RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResource readBack = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Add a second match
        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResource readBack2 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        Collection<MassiveResource> all = getAllResourcesWithDupes(_loginInfoEntry);
        assertEquals("There should be 2 resources in the repo", 2, all.size());

        _testRes.setFeaturedWeight("8");
        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResource readBack3 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        checkUpdated(readBack2, readBack3, _testRes, false);
        all = getAllResourcesWithDupes(_loginInfoEntry);
        assertEquals("There should be 1 resource in the repo", 1, all.size());
        assertTrue("The resource in massive should be the same as the one we read back after the update", readBack3.equivalentWithoutAttachments(all.iterator().next()));
    }

    @Test
    public void testDeleteDuplicates() throws RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResource readBack = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Add a second match
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResource readBack2 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        Collection<MassiveResource> all = getAllResourcesWithDupes(_loginInfoEntry);
        assertEquals("There should be 2 resources in the repo", 2, all.size());

        _testRes.uploadToMassive(new AddThenDeleteStrategy());
        SampleResource readBack3 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        all = getAllResourcesWithDupes(_loginInfoEntry);
        assertEquals("There should be 1 resource in the repo", 1, all.size());
        assertTrue("The resource in massive should be the same as the one we read back after the update", readBack3.equivalentWithoutAttachments(all.iterator().next()));
        String id3 = readBack3.get_id();
        assertTrue("We should have done an update on one of the two original assets and deleted the other one", (id3.equals(readBack.get_id()) || id3.equals(readBack2.get_id())));
    }

    @Test
    public void shouldReportDeletedResources() throws RepositoryBackendException, RepositoryResourceException {
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResource readBack = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());

        // Add a matching resource to cause the original resource to be deleted
        _testRes.setFeaturedWeight("5");
        AddThenDeleteStrategy addThenDelete = new AddThenDeleteStrategy();
        _testRes.uploadToMassive(addThenDelete);

        List<MassiveResource> deletedResources = addThenDelete.getDeletedResources();
        assertEquals("Should have deleted precisely one matching resource", 1, deletedResources.size());
        assertEquals("Should have deleted the matching resource", readBack.get_id(), deletedResources.get(0).get_id());
    }

    @Test
    public void shouldReportDeletedDuplicateResources() throws RepositoryBackendException, RepositoryResourceException {
        // Add the same resource three times, meaning that when we then try to upload
        // one identical matching resource, it will delete two of them (leaving just
        // one resource)

        _testRes.uploadToMassive(new AddNewStrategy());
        _testRes.uploadToMassive(new AddNewStrategy());
        _testRes.uploadToMassive(new AddNewStrategy());
        AddThenDeleteStrategy addThenDelete = new AddThenDeleteStrategy();
        _testRes.uploadToMassive(addThenDelete);

        List<MassiveResource> deletedResources = addThenDelete.getDeletedResources();
        assertEquals("Should delete two of the three duplicate resources", 2, deletedResources.size());
    }

    @Override
    protected UploadStrategy createStrategy(State ifMatching, State ifNoMatching) {
        return new AddThenDeleteStrategy(ifMatching, ifNoMatching, false);
    }
}
