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

import static com.ibm.ws.lars.testutils.BasicChecks.checkCopyFields;
import static com.ibm.ws.lars.testutils.BasicChecks.populateResource;
import static com.ibm.ws.lars.testutils.FatUtils.FAT_REPO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.RepositoryFixture;
import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.resources.EsaResource;
import com.ibm.ws.massive.resources.EsaResource.FilterableAttribute;
import com.ibm.ws.massive.resources.MassiveResource;
import com.ibm.ws.massive.resources.MassiveResource.DownloadPolicy;
import com.ibm.ws.massive.resources.RepositoryResourceException;
import com.ibm.ws.massive.resources.UpdateInPlaceStrategy;

public class MassiveEsaResourceTest {

    @Rule
    public RepositoryFixture repo = FAT_REPO;
    private final LoginInfoEntry _loginInfoEntry = repo.getLoginInfoEntry();

    @Test
    public void testIsDownloadable() {
        assertEquals("ESAs should only be downloadable by installer",
                     DownloadPolicy.INSTALLER, new EsaResource(_loginInfoEntry).getDownloadPolicy());
    }

    /**
     * Test to make sure when you set a short name there is also a lower case one set
     */
    @Test
    public void testLowerCaseShortName() {
        EsaResource esa = new EsaResource(_loginInfoEntry);
        String shortName = "ShortNameValue";
        esa.setShortName(shortName);
        assertEquals("The lower case version should have been set", shortName.toLowerCase(), esa.getLowerCaseShortName());
        esa.setShortName(null);
        assertNull("The lower case version should have been unset", esa.getLowerCaseShortName());
    }

    @Test
    public void testCopyFields() throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoSuchMethodException,
                    SecurityException, InvocationTargetException, IOException {
        checkCopyFields(new EsaResource(_loginInfoEntry), new EsaResource(_loginInfoEntry));
    }

    /**
     * Repo1 has 3 features, esa1, esa2 and esa3
     * Repo2 has 2 features, esa2 and esa4
     * Repo3 has 3 features, esa2, esa3 and esa4
     *
     * @throws NoRepoAvailableException
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @Test
    @Ignore
    // multi-repository
    public void testReadEsasFromMultipleRepositories() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
//        LoginInfoEntry repo2 = loginInfoProvider.createNewRepoFromProperties();
//        LoginInfoEntry repo3 = loginInfoProvider.createNewRepoFromProperties();
        LoginInfoEntry repo2 = null;
        LoginInfoEntry repo3 = null;

        // _loginInfo contains just repo1

        // Creates a collection with just repo2 in (for uploading)
        LoginInfo repo1Only = new LoginInfo(_loginInfoEntry);
        LoginInfo repo2Only = new LoginInfo(repo2);
        LoginInfo repo3Only = new LoginInfo(repo3);

        // Creates a collection with both repos in (for reading from). Get repo1 from _loginInfo
        LoginInfo allRepos = new LoginInfo(_loginInfoEntry);
        allRepos.add(repo2);
        allRepos.add(repo3);

        // Add an asset to repo1
        EsaResource esaRes1 = createEsaResource("esa1");
        esaRes1.uploadToMassive(new UpdateInPlaceStrategy());

        // Add an asset to repo1
        EsaResource esaRes2 = createEsaResource("esa2");
        esaRes2.uploadToMassive(new UpdateInPlaceStrategy());

        // Add an asset to repo1
        EsaResource esaRes3 = createEsaResource("esa3");
        esaRes3.uploadToMassive(new UpdateInPlaceStrategy());

        // Add esa2 again to repo2.
        esaRes2.setLoginInfoEntry(repo2);
        esaRes2.uploadToMassive(new UpdateInPlaceStrategy());

        // Add esa4 to repo 2
        EsaResource esaRes4 = createEsaResource("esa4");
        esaRes4.setLoginInfoEntry(repo2);
        esaRes4.uploadToMassive(new UpdateInPlaceStrategy());

        // Add esa2 and esa3 to repo 3.
        esaRes2.setLoginInfoEntry(repo3);
        esaRes2.uploadToMassive(new UpdateInPlaceStrategy());
        esaRes3.setLoginInfoEntry(repo3);
        esaRes3.uploadToMassive(new UpdateInPlaceStrategy());

        // Create a new esa resource using the same provideFeature (instead of reusing the
        // same esa like we have done above). So Esa5
        EsaResource esaRes4dupe = createEsaResource("esa4");
        esaRes4dupe.setLoginInfoEntry(repo3);
        esaRes4dupe.uploadToMassive(new UpdateInPlaceStrategy());

        // Should only get one asset from repo1
        assertEquals("Should be 3 assets in repo1", 3, MassiveResource.getAllResources(repo1Only).size());

        // Should get one asset from repo2
        assertEquals("Should be 2 assets in repo2", 2, MassiveResource.getAllResources(repo2Only).size());

        // Should get two assets from repo3
        assertEquals("Should be 3 assets in repo3", 3, MassiveResource.getAllResources(repo3Only).size());

        // Should get two assets when using both repos
        assertEquals("Should get two assets when using both repos", 4, MassiveResource.getAllResources(allRepos).size());
    }

    /**
     * This test sees if the {@link EsaResource#getMatchingEsas(String, LoginInfo)} method works.
     *
     * @throws URISyntaxException
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    @Test
    public void testFilteringEsas() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {
        // Add ESAs that would match on the 3 different attributes (and one that doesn't) and make sure we get back the right one when we filter by that attribute
        String filterString = "wibble";
        EsaResource bySymbolicName = createEsaResource(filterString);
        bySymbolicName.uploadToMassive(new UpdateInPlaceStrategy());

        EsaResource byShortName = createEsaResource("foo");
        byShortName.setShortName(filterString);
        byShortName.uploadToMassive(new UpdateInPlaceStrategy());

        EsaResource byLowerCaseShortName = createEsaResource("bar");
        String byLowerCaseFilterString = "wobble";
        byLowerCaseShortName.setShortName(byLowerCaseFilterString.toUpperCase());
        byLowerCaseShortName.uploadToMassive(new UpdateInPlaceStrategy());

        EsaResource noMatch = createEsaResource("baz");
        noMatch.uploadToMassive(new UpdateInPlaceStrategy());

        testMatch(FilterableAttribute.SYMBOLIC_NAME, filterString, bySymbolicName);
        testMatch(FilterableAttribute.SHORT_NAME, filterString, byShortName);
        testMatch(FilterableAttribute.LOWER_CASE_SHORT_NAME, byLowerCaseFilterString, byLowerCaseShortName);
    }

    /**
     * Tests that you can run the {@link EsaResource#getMatchingEsas(FilterableAttribute, String, LoginInfo)} method correctly and get the expected result
     *
     * @param attribute
     * @param filterString
     * @param expectedResource
     * @throws RepositoryBackendException
     */
    private void testMatch(FilterableAttribute attribute, String filterString, EsaResource expectedResource) throws RepositoryBackendException {
        Collection<EsaResource> matched = EsaResource.getMatchingEsas(attribute, filterString, new LoginInfo(_loginInfoEntry));
        assertEquals("There should only be one match", 1, matched.size());
        assertTrue("The match should be the right resource", matched.contains(expectedResource));
    }

    private EsaResource createEsaResource(String provideFeature) throws URISyntaxException {
        EsaResource esaRes = new EsaResource(_loginInfoEntry);

        populateResource(esaRes);
        esaRes.setProvideFeature(provideFeature);

        return esaRes;
    }

}
