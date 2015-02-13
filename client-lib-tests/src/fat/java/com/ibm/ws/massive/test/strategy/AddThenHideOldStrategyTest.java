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
import static com.ibm.ws.lars.testutils.ReflectionTricks.getAllResourcesWithDupes;
import static com.ibm.ws.lars.testutils.ReflectionTricks.reflectiveCallNoPrimitives;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.resources.AddNewStrategy;
import com.ibm.ws.massive.resources.AddThenDeleteStrategy;
import com.ibm.ws.massive.resources.AddThenHideOldStrategy;
import com.ibm.ws.massive.resources.EsaResource;
import com.ibm.ws.massive.resources.MassiveResource;
import com.ibm.ws.massive.resources.MassiveResource.DisplayPolicy;
import com.ibm.ws.massive.resources.MassiveResource.State;
import com.ibm.ws.massive.resources.RepositoryResourceException;
import com.ibm.ws.massive.resources.RepositoryResourceUpdateException;
import com.ibm.ws.massive.resources.UploadStrategy;

/**
 *
 */
public class AddThenHideOldStrategyTest extends StrategyTestBaseClass {

    /**
     * @throws FileNotFoundException
     * @throws IOException
     */
    public AddThenHideOldStrategyTest() throws FileNotFoundException, IOException {
        super();
    }

    @Before
    public void clearCache() {
        AddThenHideOldStrategy.clearCache();
    }

    @Test
    public void testAddingToRepoUsingReplaceExistingStrategy() throws RepositoryBackendException, RepositoryResourceException, URISyntaxException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException {
        EsaResource resource = new EsaResource(_loginInfoEntry);
        populateResource(resource);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));

        Collection<MassiveResource> allResources = MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry));
        assertEquals("There should be 2 resources in the repo", 2, allResources.size());
        for (MassiveResource res : allResources) {
            if (res.getDescription().equals("Original")) {
                assertEquals("The original resource should now be hidden", DisplayPolicy.HIDDEN, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else if (res.getDescription().equals("New")) {
                assertEquals("The new resource should be visible", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    /**
     * Test if you have a draft asset with the same vanity URL then it is ignored
     */
    @Test
    public void testDraftAssetsIgnored() throws RepositoryBackendException, RepositoryResourceException, URISyntaxException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException {
        EsaResource draft = new EsaResource(_loginInfoEntry);
        populateResource(draft);
        draft.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        draft.setDescription("Draft");
        draft.uploadToMassive(new AddNewStrategy());
        EsaResource published = new EsaResource(_loginInfoEntry);
        published.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        published.setDescription("Published");
        published.setVersion("version 2");
        published.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        EsaResource newResource = new EsaResource(_loginInfoEntry);
        newResource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        newResource.setDescription("New");
        newResource.setVersion("version 3");
        // This shouldn't throw an exception for the test to pass
        newResource.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));

        Collection<MassiveResource> allResources = MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry));
        assertEquals("There should be 3 resources in the repo", 3, allResources.size());
        for (MassiveResource res : allResources) {
            if (res.getDescription().equals("Draft")) {
                assertEquals("The draft resource should remain visible", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else if (res.getDescription().equals("Published")) {
                assertEquals("The original resource should now be hidden", DisplayPolicy.HIDDEN, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else if (res.getDescription().equals("New")) {
                assertEquals("The new resource should now be visible", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else {
                fail("Unexpected resource found in the test: " + res);
            }
        }
    }

    @Test
    public void testOverwritingExisting() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResource resource1 = new EsaResource(_loginInfoEntry);
        populateResource(resource1);
        resource1.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource1.setDescription("Original");
        resource1.uploadToMassive(new AddNewStrategy());
        EsaResource resource2 = new EsaResource(_loginInfoEntry);
        resource2.setDescription("New");
        populateResource(resource2);
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource2.uploadToMassive(new AddThenHideOldStrategy());
        Collection<MassiveResource> allResources = getAllResourcesWithDupes(_loginInfoEntry);
        assertEquals("There should be 1 resources in the repo", 1, allResources.size());
        assertEquals("There resource should be the second one added", resource2, allResources.iterator().next());
        assertEquals("The feature web display policy should be visible", DisplayPolicy.VISIBLE,
                     reflectiveCallNoPrimitives(allResources.iterator().next(), "getWebDisplayPolicy", (Object[]) null));
    }

    @Test
    public void testDontHideWhenAddingHidden() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResource resource = new EsaResource(_loginInfoEntry);
        populateResource(resource);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.setWebDisplayPolicy(DisplayPolicy.HIDDEN);
        resource.uploadToMassive(new AddThenHideOldStrategy());

        Collection<MassiveResource> allResources = MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry));
        assertEquals("There should be 2 resources in the repo", 2, allResources.size());
        for (MassiveResource res : allResources) {
            if (res.getDescription().equals("Original")) {
                assertEquals("The original resource should be visibile still", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else if (res.getDescription().equals("New")) {
                assertEquals("The new resource should be hidden", DisplayPolicy.HIDDEN, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testDontHideWhenAddingDraft() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResource resource = new EsaResource(_loginInfoEntry);
        populateResource(resource);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.uploadToMassive(new AddThenHideOldStrategy(null, State.DRAFT));

        Collection<MassiveResource> allResources = MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry));
        assertEquals("There should be 2 resources in the repo", 2, allResources.size());
        for (MassiveResource res : allResources) {
            if (res.getDescription().equals("Original")) {
                assertEquals("The original resource should be visibile still", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else if (res.getDescription().equals("New")) {
                assertEquals("The new resource should be visible", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testDeleteAssetAfterCacheSetup() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResource resource1 = new EsaResource(_loginInfoEntry);
        populateResource(resource1);
        resource1.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource1.setDescription("Original");
        resource1.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));

        // Do an update to get the cache loaded
        EsaResource resource2 = new EsaResource(_loginInfoEntry);
        populateResource(resource2);
        resource2.setDescription("New");
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource2.uploadToMassive(new AddThenHideOldStrategy());

        // Now update this using the add then delete so that the one in the cache will be deleted
        EsaResource resource3 = new EsaResource(_loginInfoEntry);
        populateResource(resource3);
        resource3.setDescription("Even Newer");
        resource3.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource3.uploadToMassive(new AddThenDeleteStrategy());

        // Now add a new one which doesn't match but has the same vanity URL, we should discover that resource2 has been deleted and hide resource3
        EsaResource resource4 = new EsaResource(_loginInfoEntry);
        populateResource(resource4);
        resource4.setVersion("2");
        resource4.setDescription("Final resource");
        resource4.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource4.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));

        Collection<MassiveResource> allResources = MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry));
        assertEquals("There should be 2 resources in the repo: " + allResources, 2, allResources.size());
        for (MassiveResource res : allResources) {
            if (res.getDescription().equals("Even Newer")) {
                assertEquals("The original resource should now be hidden", DisplayPolicy.HIDDEN, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else if (res.getDescription().equals("Final resource")) {
                assertEquals("The new resource should be visible", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else {
                fail("Unexpected resource found in the test " + res);
            }
        }
    }

    /**
     * Test that when you have duplicate vanity URLs we don't populate a half baked map
     *
     * @throws URISyntaxException
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    @Test
    public void testHalfPopulateMapNotLeft() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException {
        EsaResource resource1 = new EsaResource(_loginInfoEntry);
        populateResource(resource1);
        resource1.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource1.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));

        EsaResource resource2 = new EsaResource(_loginInfoEntry);
        populateResource(resource2);
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource2.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));

        EsaResource resource3 = new EsaResource(_loginInfoEntry);
        populateResource(resource2);
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        try {
            resource3.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
            fail("There are duplicate vanity URLs so should not be able to use add then hide strategy");
        } catch (RepositoryResourceUpdateException e) {
            // Expected
        }

        try {
            resource3.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
            fail("There are duplicate vanity URLs so should not be able to use add then hide strategy even when running it twice!");
        } catch (RepositoryResourceUpdateException e) {
            // Expected
        }
    }

    @Override
    protected UploadStrategy createStrategy(State ifMatching, State ifNoMatching) {
        return new AddThenHideOldStrategy(ifMatching, ifNoMatching);
    }
}
