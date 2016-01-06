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
package com.ibm.ws.repository.strategies.test;

import static com.ibm.ws.lars.testutils.BasicChecks.populateResource;
import static com.ibm.ws.lars.testutils.ReflectionTricks.reflectiveCallNoPrimitives;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.strategies.writeable.AddNewStrategy;
import com.ibm.ws.repository.strategies.writeable.AddThenDeleteStrategy;
import com.ibm.ws.repository.strategies.writeable.AddThenHideOldStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

@RunWith(Parameterized.class)
public class AddThenHideOldStrategyTest extends StrategyTestBaseClass {

    public AddThenHideOldStrategyTest(RepositoryFixture fixture) {
        super(fixture);
    }

    @Test
    public void testAddingToRepoUsingReplaceExistingStrategy() throws RepositoryBackendException, RepositoryResourceException, URISyntaxException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException {
        EsaResourceImpl resource = new EsaResourceImpl(repoConnection);
        populateResource(resource);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());
        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());

        Collection<? extends RepositoryResource> allResources = new RepositoryConnectionList(repoConnection).getAllResources();
        assertEquals("There should be 2 resources in the repo", 2, allResources.size());
        for (RepositoryResource res : allResources) {
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
     * The website treats a null web display policy as though it was visible so add then hide should hide it.
     *
     * @throws URISyntaxException
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    @Test
    public void testHidingNullDisplayPolicy() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResourceImpl resource = new EsaResourceImpl(repoConnection);
        populateResource(resource);
        resource.setWebDisplayPolicy(null);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());
        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());

        Collection<? extends RepositoryResource> allResources = new RepositoryConnectionList(repoConnection).getAllResources();
        assertEquals("There should be 2 resources in the repo", 2, allResources.size());
        for (RepositoryResource res : allResources) {
            if (res.getDescription().equals("Original")) {
                assertEquals("The original resource should now be hidden", DisplayPolicy.HIDDEN, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else if (res.getDescription().equals("New")) {
                assertNull("The new resource should still be visible due to null display policy", reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
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
        EsaResourceImpl draft = new EsaResourceImpl(repoConnection);
        populateResource(draft);
        draft.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        draft.setDescription("Draft");
        draft.setProviderName("IBM");
        draft.uploadToMassive(new AddNewStrategy());
        EsaResourceImpl published = new EsaResourceImpl(repoConnection);
        published.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        published.setDescription("Published");
        published.setVersion("version 2");
        published.setProviderName("IBM");
        published.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));

        assertEquals("this resource should have gone into the published state", State.PUBLISHED, published.getState());
        EsaResourceImpl newResource = new EsaResourceImpl(repoConnection);
        newResource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        newResource.setDescription("New");
        newResource.setVersion("version 3");
        newResource.setProviderName("IBM");
        // This shouldn't throw an exception for the test to pass
        newResource.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, newResource.getState());

        Collection<? extends RepositoryResource> allResources = new RepositoryConnectionList(repoConnection).getAllResources();
        assertEquals("There should be 3 resources in the repo", 3, allResources.size());
        for (RepositoryResource res : allResources) {
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
        EsaResourceImpl resource1 = new EsaResourceImpl(repoConnection);
        populateResource(resource1);
        resource1.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource1.setDescription("Original");
        resource1.uploadToMassive(new AddNewStrategy());
        EsaResourceImpl resource2 = new EsaResourceImpl(repoConnection);
        resource2.setDescription("New");
        populateResource(resource2);
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource2.uploadToMassive(new AddThenHideOldStrategy());
        Collection<? extends RepositoryResource> allResources = repoConnection.getAllResourcesWithDupes();
        assertEquals("There should be 1 resources in the repo", 1, allResources.size());
        assertEquals("There resource should be the second one added", resource2, allResources.iterator().next());
        assertEquals("The feature web display policy should be visible", DisplayPolicy.VISIBLE,
                     reflectiveCallNoPrimitives(allResources.iterator().next(), "getWebDisplayPolicy", (Object[]) null));
    }

    @Test
    public void testDontHideWhenAddingHidden() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResourceImpl resource = new EsaResourceImpl(repoConnection);
        populateResource(resource);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());
        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.setWebDisplayPolicy(DisplayPolicy.HIDDEN);
        resource.uploadToMassive(new AddThenHideOldStrategy());

        Collection<? extends RepositoryResource> allResources = new RepositoryConnectionList(repoConnection).getAllResources();
        assertEquals("There should be 2 resources in the repo", 2, allResources.size());
        for (RepositoryResource res : allResources) {
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
        EsaResourceImpl resource = new EsaResourceImpl(repoConnection);
        populateResource(resource);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());
        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.uploadToMassive(new AddThenHideOldStrategy(null, State.DRAFT));
        assertEquals("this resource should have gone into the draft state", State.DRAFT, resource.getState());

        Collection<? extends RepositoryResource> allResources = new RepositoryConnectionList(repoConnection).getAllResources();
        assertEquals("There should be 2 resources in the repo", 2, allResources.size());
        for (RepositoryResource res : allResources) {
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
        EsaResourceImpl resource1 = new EsaResourceImpl(repoConnection);
        populateResource(resource1);
        resource1.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource1.setDescription("Original");
        resource1.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource1.getState());

        // Do an update to get the cache loaded
        EsaResourceImpl resource2 = new EsaResourceImpl(repoConnection);
        populateResource(resource2);
        resource2.setDescription("New");
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource2.uploadToMassive(new AddThenHideOldStrategy());

        // Now update this using the add then delete so that the one in the cache will be deleted
        EsaResourceImpl resource3 = new EsaResourceImpl(repoConnection);
        populateResource(resource3);
        resource3.setDescription("Even Newer");
        resource3.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource3.uploadToMassive(new AddThenDeleteStrategy());

        // Now add a new one which doesn't match but has the same vanity URL, we should discover that resource2 has been deleted and hide resource3
        EsaResourceImpl resource4 = new EsaResourceImpl(repoConnection);
        populateResource(resource4);
        resource4.setVersion("2");
        resource4.setDescription("Final resource");
        resource4.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource4.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource4.getState());

        Collection<? extends RepositoryResource> allResources = new RepositoryConnectionList(repoConnection).getAllResources();
        assertEquals("There should be 2 resources in the repo: " + allResources, 2, allResources.size());
        for (RepositoryResource res : allResources) {
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
     * Having two visible assets with the same vanity URL means that repository
     * is in an inconsistent state. There is some logic to look for this inconsistency
     * in the AddThenHideStrategy. This test is testing the checking logic. It sets
     * up the repository in a dodgy state by uploading two visible assets with the
     * same URL (using AddNew), and then tests that an error is thrown by the checking
     * logic when you try to add a third asset using AddThenHideOld
     *
     */
    @Test
    public void testDuplicateVisibleVanityURLs() throws RepositoryBackendException, RepositoryResourceException {

        // Need to set name/appliesTo for the the assets to ensure they are treated as different
        // and hence the second asset will get uploaded (instead of ignored)
        // Also set the vanity URL to be the same for both.
        EsaResourceImpl resource1 = new EsaResourceImpl(repoConnection);
        populateResource(resource1);
        resource1.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource1.setProviderName("IBM");
        resource1.setName("name1");
        resource1.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0");
        resource1.setVanityURL("foourl");
        resource1.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource1.getState());

        EsaResourceImpl resource2 = new EsaResourceImpl(repoConnection);
        populateResource(resource2);
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource2.setProviderName("IBM");
        resource2.setName("name2");
        resource2.setVanityURL("foourl");
        resource2.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.1");
        resource2.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource2.getState());

        // Should now have 2 visible assets with the URL 'foourl'. Uploading another
        // asset with the same URL (using AddThenHideOld), should trigger the checks for
        // duplicate vanity URLs and hence an error.
        EsaResourceImpl resource3 = new EsaResourceImpl(repoConnection);
        populateResource(resource3);
        resource3.setProviderName("IBM");
        resource3.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource3.setName("name3");
        resource3.setVanityURL("foourl");
        resource3.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.2");
        try {
            resource3.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
            fail("The expected exception was not thrown");
        } catch (RepositoryResourceException e) {
            assertThat(e.getMessage(), containsString("There was more than 1 resource with the vanity URL foourl in state visible"));
        }

    }

    @Override
    protected UploadStrategy createStrategy(State ifMatching, State ifNoMatching) {
        return new AddThenHideOldStrategy(ifMatching, ifNoMatching);
    }
}
