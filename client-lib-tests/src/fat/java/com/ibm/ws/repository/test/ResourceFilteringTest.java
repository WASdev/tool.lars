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
package com.ibm.ws.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.FilterPredicate;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.SimpleProductDefinition;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.ProductRelatedResourceImpl;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.strategies.writeable.AddNewStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;
import com.ibm.ws.repository.transport.client.RepositoryClient;
import com.ibm.ws.repository.transport.client.RestClient;

/**
 * Tests filtering of massive resources
 */
@RunWith(Parameterized.class)
public class ResourceFilteringTest {

    private final FilterResources filterResources;
    private final RepositoryConnection repoConnection;
    private final RepositoryConnection userRepoConnection;

    private static Logger logger = Logger.getLogger(ResourceFilteringTest.class.getName());

    private static List<RepoData> repoDataCache;

    public ResourceFilteringTest(RepoData repoData) {
        filterResources = repoData.resources;
        repoConnection = repoData.fixture.getAdminConnection();
        userRepoConnection = repoData.fixture.getUserConnection();
    }

    /*
     * The setup for this test is a little complex.
     * 
     * The tests in this class expect to run against a repository loaded with assets in setupRepoForFilterTests()
     * 
     * As in the other test classes, we need to run these tests against multiple repositories.
     * 
     * getClassRule() returns a set of rules which ensure we have the repositories we want set up at the start
     * of the test and cleaned up at the end.
     * 
     * getParameters() ensures we actually run the tests against each of the repositories
     * 
     * setupClass() loads each of the repositories with the initial test data
     */

    /**
     * Returns a RepoData for each repository we are testing against
     */
    @Parameters(name = "{0}")
    public static Object[][] getParameters() {
        List<Object[]> parameters = new ArrayList<Object[]>();
        for (RepoData data : getRepoDataList()) {
            parameters.add(new Object[] { data });
        }
        return parameters.toArray(new Object[parameters.size()][]);
    }

    /**
     * Create a RuleChain containing a RepositoryFixture for each of the repositories we are testing against
     *
     * @return a RuleChain
     */
    @ClassRule
    public static TestRule getClassRule() {
        // Put all the fixtures into one chain rule because we want to set up all the repositories at the start of the test, test with them and tear them down at the end
        RuleChain chain = RuleChain.emptyRuleChain();
        for (RepoData repoData : getRepoDataList()) {
            chain = chain.around(repoData.fixture);
        }
        return chain;
    }

    /**
     * Load the test data into each of the repositories
     */
    @BeforeClass
    public static void setupClass() throws RepositoryResourceException, RepositoryBackendException, IOException, ClassNotFoundException,
                    NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        for (RepoData data : getRepoDataList()) {
            data.resources = setupRepoForFilterTests(data.fixture);
        }
    }

    /**
     * Tests that if you filter for a specific version you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringWithRightVersion() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");

        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(Collections.singleton(productDefinition),
                                                                                                                                       null,
                                                                                                                                       null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                       FilterResources.Resources.FEATURE_WITH_EDITIONS, FilterResources.Resources.FEATURE_TWO_PRODUCTS,
                                                                       FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE));

        Collection<? extends RepositoryResource> samples = result.get(ResourceType.PRODUCTSAMPLE);
        filterResources.validateReturnedResources(samples, EnumSet.of(FilterResources.Resources.SAMPLE));

        Collection<? extends RepositoryResource> addons = result.get(ResourceType.ADDON);
        filterResources.validateReturnedResources(addons, EnumSet.of(FilterResources.Resources.ADDON));
    }

    @Test
    public void testResourceFiltering() throws RepositoryBackendException {

        // This test should test all the possible attributes, so assert that the number
        // of values hasn't changed. If it does, add a new test!
        assertTrue("An attribute has been added/or removed, so a new test is needed (or one removed)",
                   FilterableAttribute.values().length == 9);

        RepositoryConnectionList connection = new RepositoryConnectionList(repoConnection);

        // Test filtering on all attributes individually
        Collection<RepositoryResource> result = connection.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.TYPE, ResourceType.PRODUCTSAMPLE));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SAMPLE));

        result = connection.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.PRODUCT_ID, "com.ibm.ws.plw"));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_TWO_PRODUCTS,
                                                                     FilterResources.Resources.FEATURE_OTHER_PRODUCT_SAME_VERSION,
                                                                     FilterResources.Resources.FEATURE_OTHER_PRODUCT));

        result = connection.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.VISIBILITY, Visibility.INSTALL));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE));

        result = connection.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.PRODUCT_MIN_VERSION, "1.0.0.0"));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_OTHER_PRODUCT,
                                                                     FilterResources.Resources.FEATURE_TWO_PRODUCTS));

        result = connection.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.PRODUCT_HAS_MAX_VERSION, Boolean.FALSE));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                     FilterResources.Resources.SAMPLE));

        result = connection.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.SYMBOLIC_NAME, "com.ibm.ws.simpleFeature-1.0"));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE));

        result = connection.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.SHORT_NAME, "simpleFeature-1.0"));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE));

        result = connection.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.LOWER_CASE_SHORT_NAME, "simplefeature-1.0"));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE));

        result = connection.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.VANITY_URL, "features-com.ibm.ws.simpleFeature-1.0"));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE));

        // test a combination of filters
        result = connection.getMatchingResources(
                                                 FilterPredicate.areEqual(FilterableAttribute.VISIBILITY, Visibility.PUBLIC),
                                                 FilterPredicate.areEqual(FilterableAttribute.PRODUCT_ID, "com.ibm.ws.plw"),
                                                 FilterPredicate.areEqual(FilterableAttribute.PRODUCT_MIN_VERSION, "8.5.5.0"));
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_TWO_PRODUCTS,
                                                                     FilterResources.Resources.FEATURE_OTHER_PRODUCT_SAME_VERSION));

    }

    /**
     * Tests that if you filter for a specific version on find you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindWithRightVersion() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");

        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1",
                                                                                                                         Collections.singleton(productDefinition),
                                                                                                                         null, null);

        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                     FilterResources.Resources.FEATURE_WITH_EDITIONS, FilterResources.Resources.FEATURE_TWO_PRODUCTS,
                                                                     FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE, FilterResources.Resources.SAMPLE,
                                                                     FilterResources.Resources.ADDON));
    }

    /**
     * Tests that if you filter for version lower than most of the resources in the repo you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringWithLowVersion() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.0.0.0", "Archive", "ILAN", "DEVELOPERS");

        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(Collections.singleton(productDefinition),
                                                                                                                                       null,
                                                                                                                                       null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        assertEquals("Expected there to be 1 feature", 1, features.size());
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION));

        Collection<? extends RepositoryResource> samples = result.get(ResourceType.PRODUCTSAMPLE);
        assertNull("No samples should be returned", samples);

        Collection<? extends RepositoryResource> addons = result.get(ResourceType.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you filter on search for version lower than most of the resources in the repo you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindWithLowVersion() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.0.0.0", "Archive", "ILAN", "DEVELOPERS");

        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1",
                                                                                                                         Collections.singleton(productDefinition),
                                                                                                                         null, null);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION));
    }

    /**
     * Tests that if you filter for version higher than most of the resources in the repo you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringWithHighVersion() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.4", "Archive", "ILAN", "DEVELOPERS");

        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(Collections.singleton(productDefinition),
                                                                                                                                       null,
                                                                                                                                       null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION, FilterResources.Resources.FEATURE_WITH_LATER_VERSION));

        Collection<? extends RepositoryResource> samples = result.get(ResourceType.PRODUCTSAMPLE);
        filterResources.validateReturnedResources(samples, EnumSet.of(FilterResources.Resources.SAMPLE));

        Collection<? extends RepositoryResource> addons = result.get(ResourceType.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you filter on find for version higher than most of the resources in the repo you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindWithHighVersion() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.4", "Archive", "ILAN", "DEVELOPERS");

        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1",
                                                                                                                         Collections.singleton(productDefinition),
                                                                                                                         null, null);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION, FilterResources.Resources.FEATURE_WITH_LATER_VERSION,
                                                                     FilterResources.Resources.SAMPLE));
    }

    /**
     * Tests that if you filter for a specific type you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringByType() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");

        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(Collections.singleton(productDefinition),
                                                                                                                                       Collections.singleton(ResourceType.PRODUCTSAMPLE),
                                                                                                                                       null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        assertNull("Expected there to be no features", features);

        Collection<? extends RepositoryResource> samples = result.get(ResourceType.PRODUCTSAMPLE);
        filterResources.validateReturnedResources(samples, EnumSet.of(FilterResources.Resources.SAMPLE));

        Collection<? extends RepositoryResource> addons = result.get(ResourceType.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you filter on find for a specific type you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindByType() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");

        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1",
                                                                                                                         Collections.singleton(productDefinition),
                                                                                                                         Collections.singleton(ResourceType.PRODUCTSAMPLE),
                                                                                                                         null);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SAMPLE));
    }

    /**
     * Tests that if you filter for two types you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringByMultipleTypes() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");

        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.PRODUCTSAMPLE);
        types.add(ResourceType.ADDON);
        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(Collections.singleton(productDefinition),
                                                                                                                                       types,
                                                                                                                                       null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        assertNull("Expected there to be no features", features);

        Collection<? extends RepositoryResource> samples = result.get(ResourceType.PRODUCTSAMPLE);
        filterResources.validateReturnedResources(samples, EnumSet.of(FilterResources.Resources.SAMPLE));

        Collection<? extends RepositoryResource> addons = result.get(ResourceType.ADDON);
        filterResources.validateReturnedResources(addons, EnumSet.of(FilterResources.Resources.ADDON));
    }

    /**
     * Tests that if you filter on find for two types you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindByMultipleTypes() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");

        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.PRODUCTSAMPLE);
        types.add(ResourceType.ADDON);
        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1",
                                                                                                                         Collections.singleton(productDefinition),
                                                                                                                         types, null);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SAMPLE, FilterResources.Resources.ADDON));
    }

    /**
     * Tests that if you filter for a specific type and no product defintiion you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringByTypeOnly() throws RepositoryBackendException {
        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.FEATURE);
        types.add(ResourceType.PRODUCTSAMPLE);
        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(null, types, null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                       FilterResources.Resources.FEATURE_WITH_LATER_VERSION,
                                                                       FilterResources.Resources.FEATURE_OTHER_PRODUCT_SAME_VERSION,
                                                                       FilterResources.Resources.FEATURE_WITH_EDITIONS, FilterResources.Resources.FEATURE_TWO_PRODUCTS,
                                                                       FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE, FilterResources.Resources.FEATURE_OTHER_PRODUCT));

        Collection<? extends RepositoryResource> samples = result.get(ResourceType.PRODUCTSAMPLE);
        filterResources.validateReturnedResources(samples, EnumSet.of(FilterResources.Resources.SAMPLE));

        Collection<? extends RepositoryResource> addons = result.get(ResourceType.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you filter on find for a specific type and no product defintiion you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindByTypeOnly() throws RepositoryBackendException {
        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.FEATURE);
        types.add(ResourceType.PRODUCTSAMPLE);
        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1", null, types,
                                                                                                                         null);
        filterResources.validateReturnedResources(result, EnumSet.complementOf(EnumSet.of(FilterResources.Resources.ADDON)));
    }

    /**
     * Tests that if you are just getting features you can filter them by visibility
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringFeaturesByVisibility() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        RepositoryConnectionList repoList = new RepositoryConnectionList(repoConnection);
        Map<ResourceType, Collection<? extends RepositoryResource>> result = repoList.getResources(Collections.singleton(productDefinition),
                                                                                                   Collections.singleton(ResourceType.FEATURE),
                                                                                                   Visibility.PUBLIC);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_TWO_PRODUCTS));

        result = repoList.getResources(Collections.singleton(productDefinition),
                                       Collections.singleton(ResourceType.FEATURE), Visibility.PRIVATE);
        features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION));

        result = repoList.getResources(Collections.singleton(productDefinition),
                                       Collections.singleton(ResourceType.FEATURE), Visibility.INSTALL);
        features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE));

        result = repoList.getResources(Collections.singleton(productDefinition),
                                       Collections.singleton(ResourceType.FEATURE), Visibility.PROTECTED);
        features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.FEATURE_WITH_EDITIONS));
    }

    /**
     * Tests that if you are just getting features you can filter them by visibility when running a find
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindFeaturesByVisibility() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        RepositoryConnectionList repoList = new RepositoryConnectionList(userRepoConnection);

        Collection<? extends RepositoryResource> result = repoList.findResources("keyword1", Collections.singleton(productDefinition), Collections.singleton(ResourceType.FEATURE),
                                                                                 Visibility.PUBLIC);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_TWO_PRODUCTS));

        result = repoList.findResources("keyword1", Collections.singleton(productDefinition),
                                        Collections.singleton(ResourceType.FEATURE), Visibility.PRIVATE);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION));

        result = repoList.findResources("keyword1", Collections.singleton(productDefinition),
                                        Collections.singleton(ResourceType.FEATURE), Visibility.INSTALL);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE));

        result = repoList.findResources("keyword1", Collections.singleton(productDefinition),
                                        Collections.singleton(ResourceType.FEATURE), Visibility.PROTECTED);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_WITH_EDITIONS));
    }

    /**
     * Tests that if you are just getting features and other types you can filter them by visibility, note that this takes a different code path to when doing just features as
     * above hence the two tests
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringByVisibilityAndMultipleTypes() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        RepositoryConnectionList repoList = new RepositoryConnectionList(repoConnection);

        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.FEATURE);
        types.add(ResourceType.ADDON);
        Map<ResourceType, Collection<? extends RepositoryResource>> result = repoList.getResources(Collections.singleton(productDefinition), types, Visibility.PUBLIC);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_TWO_PRODUCTS));
        Collection<? extends RepositoryResource> addons = result.get(ResourceType.ADDON);
        filterResources.validateReturnedResources(addons, EnumSet.of(FilterResources.Resources.ADDON));

        result = repoList.getResources(Collections.singleton(productDefinition), types, Visibility.PRIVATE);
        features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION));
        addons = result.get(ResourceType.ADDON);
        filterResources.validateReturnedResources(addons, EnumSet.of(FilterResources.Resources.ADDON));

        result = repoList.getResources(Collections.singleton(productDefinition), types, Visibility.INSTALL);
        features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE));
        addons = result.get(ResourceType.ADDON);
        filterResources.validateReturnedResources(addons, EnumSet.of(FilterResources.Resources.ADDON));

        result = repoList.getResources(Collections.singleton(productDefinition), types, Visibility.PROTECTED);
        features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.FEATURE_WITH_EDITIONS));
        addons = result.get(ResourceType.ADDON);
        addons = result.get(ResourceType.ADDON);
        filterResources.validateReturnedResources(addons, EnumSet.of(FilterResources.Resources.ADDON));
    }

    /**
     * Tests that if you filter for a different product ID you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringWithDifferentProduct() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.plw", "1.0.0.0", "Archive", "ILAN", "DEVELOPERS");

        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(Collections.singleton(productDefinition),
                                                                                                                                       null,
                                                                                                                                       null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.FEATURE_TWO_PRODUCTS, FilterResources.Resources.FEATURE_OTHER_PRODUCT));

        Collection<? extends RepositoryResource> samples = result.get(ResourceType.PRODUCTSAMPLE);
        assertNull("No samples should be returned", samples);

        Collection<? extends RepositoryResource> addons = result.get(ResourceType.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you filter on find for a different product ID you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindWithDifferentProduct() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.plw", "1.0.0.0", "Archive", "ILAN", "DEVELOPERS");

        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1",
                                                                                                                         Collections.singleton(productDefinition),
                                                                                                                         null, null);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_TWO_PRODUCTS, FilterResources.Resources.FEATURE_OTHER_PRODUCT));
    }

    /**
     * Tests that if you filter for a different edition you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringWithEdition() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "ND");

        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(Collections.singleton(productDefinition),
                                                                                                                                       Collections.singleton(ResourceType.FEATURE),
                                                                                                                                       null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        assertEquals("Expected there to be 4 features", 4, features.size());
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                       FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE, FilterResources.Resources.FEATURE_TWO_PRODUCTS));
    }

    /**
     * Tests that if you filter on find for a different edition you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindWithEdition() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "ND");

        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1",
                                                                                                                         Collections.singleton(productDefinition),
                                                                                                                         Collections.singleton(ResourceType.FEATURE),
                                                                                                                         null);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                     FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE, FilterResources.Resources.FEATURE_TWO_PRODUCTS));
    }

    /**
     * Tests that if you filter for a different install type you get back the expected result
     *
     * @throws RepositoryBackendException
     */

    @Test
    public void testFilteringWithInstallType() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "InstallationManager", "ILAN", "DEVELOPERS");

        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(Collections.singleton(productDefinition),
                                                                                                                                       Collections.singleton(ResourceType.FEATURE),
                                                                                                                                       null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                       FilterResources.Resources.FEATURE_WITH_EDITIONS, FilterResources.Resources.FEATURE_TWO_PRODUCTS));
    }

    /**
     * Tests that if you filter on find for a different install type you get back the expected result
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindWithInstallType() throws RepositoryBackendException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "InstallationManager", "ILAN", "DEVELOPERS");

        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1",
                                                                                                                         Collections.singleton(productDefinition),
                                                                                                                         Collections.singleton(ResourceType.FEATURE),
                                                                                                                         null);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                     FilterResources.Resources.FEATURE_WITH_EDITIONS, FilterResources.Resources.FEATURE_TWO_PRODUCTS));
    }

    /**
     * Tests that if you filter for two different products you get everything back
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringWithMultipleProducts() throws RepositoryBackendException {
        ProductDefinition productDefinition1 = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        ProductDefinition productDefinition2 = new SimpleProductDefinition("com.ibm.ws.plw", "1.0.0.0", "Archive", "ILAN", "DEVELOPERS");
        Collection<ProductDefinition> products = new HashSet<ProductDefinition>();
        products.add(productDefinition1);
        products.add(productDefinition2);

        Map<ResourceType, Collection<? extends RepositoryResource>> result = new RepositoryConnectionList(repoConnection).getResources(products,
                                                                                                                                       Collections.singleton(ResourceType.FEATURE),
                                                                                                                                       null);
        Collection<? extends RepositoryResource> features = result.get(ResourceType.FEATURE);
        filterResources.validateReturnedResources(features, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                       FilterResources.Resources.FEATURE_WITH_EDITIONS, FilterResources.Resources.FEATURE_TWO_PRODUCTS,
                                                                       FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE, FilterResources.Resources.FEATURE_OTHER_PRODUCT));
    }

    /**
     * Tests that if you filter on find for two different products you get everything back
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFilteringOnFindWithMultipleProducts() throws RepositoryBackendException {
        ProductDefinition productDefinition1 = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        ProductDefinition productDefinition2 = new SimpleProductDefinition("com.ibm.ws.plw", "1.0.0.0", "Archive", "ILAN", "DEVELOPERS");
        Collection<ProductDefinition> products = new HashSet<ProductDefinition>();
        products.add(productDefinition1);
        products.add(productDefinition2);

        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1",
                                                                                                                         products,
                                                                                                                         Collections.singleton(ResourceType.FEATURE),
                                                                                                                         null);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.SIMPLE_FEATURE, FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                     FilterResources.Resources.FEATURE_WITH_EDITIONS, FilterResources.Resources.FEATURE_TWO_PRODUCTS,
                                                                     FilterResources.Resources.FEATURE_WITH_INSTALL_TYPE, FilterResources.Resources.FEATURE_OTHER_PRODUCT));
    }

    /**
     * Tests you can do a find using multiple words
     *
     * @throws RepositoryBackendException
     * @throws IllegalArgumentException
     * @throws SecurityException
     */
    @Test
    public void testFindMultipleWords() throws RepositoryBackendException, SecurityException, IllegalArgumentException {
        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1 keyword3", null,
                                                                                                                         null, null);
        filterResources.validateReturnedResources(result, EnumSet.allOf(FilterResources.Resources.class));

        // All the resources contain keyword1 so should all be returned but a few have keyword 3 as well so should
        // have a higher hit result and come first
        logger.log(Level.INFO, "Passing what we BELIEVE to be the top three scored hits to validate (we expect the 3 values to be");
        logger.log(Level.INFO, "Feature with later version, Feature with editions & Feature With No Version):");
        Set<RepositoryResource> firstThree = new HashSet<RepositoryResource>();
        Iterator<? extends RepositoryResource> resultIterator = result.iterator();
        RepositoryResource mr = resultIterator.next();
        logger.log(Level.INFO, "1=" + mr.getName());
        firstThree.add(mr);

        mr = resultIterator.next();
        logger.log(Level.INFO, "2=" + mr.getName());
        firstThree.add(mr);

        mr = resultIterator.next();
        logger.log(Level.INFO, "3=" + mr.getName());
        firstThree.add(mr);

        filterResources.validateReturnedResources(firstThree, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION,
                                                                         FilterResources.Resources.FEATURE_WITH_LATER_VERSION,
                                                                         FilterResources.Resources.FEATURE_WITH_EDITIONS));
    }

    /**
     * Test doing a find by a word only contained in a few resources
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFindLimitedResults() throws RepositoryBackendException {
        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword3", null, null,
                                                                                                                         null);
        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_WITH_NO_VERSION, FilterResources.Resources.FEATURE_WITH_LATER_VERSION,
                                                                     FilterResources.Resources.FEATURE_WITH_EDITIONS));
    }

    /**
     * Test you can do a find for a phrase.
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFindPhrase() throws RepositoryBackendException {
        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("\"keyword1 keyword3\"",
                                                                                                                         null,
                                                                                                                         null, null);

        filterResources.validateReturnedResources(result, EnumSet.of(FilterResources.Resources.FEATURE_WITH_LATER_VERSION));
    }

    /**
     * Tests that you can do a find and include the asset name to give it a better hit
     *
     * @throws RepositoryBackendException
     */
    @Test
    public void testFindIncludingName() throws RepositoryBackendException {
        Collection<? extends RepositoryResource> result = new RepositoryConnectionList(userRepoConnection).findResources("keyword1 Addon",
                                                                                                                         null, null, null);
        filterResources.validateReturnedResources(result, EnumSet.allOf(FilterResources.Resources.class));
        filterResources.validateReturnedResources(Collections.singleton(result.iterator().next()), EnumSet.of(FilterResources.Resources.ADDON));
    }

    /**
     * Testing creating a client.
     *
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    @Test
    public void testCreateClient() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        RestRepositoryConnection lie = new RestRepositoryConnection("a", "b", "c", "d");
        SampleResourceImpl sr = new SampleResourceImpl(lie);
        Field field = RepositoryResourceImpl.class.getDeclaredField("_client");
        field.setAccessible(true);
        RepositoryClient client = (RepositoryClient) field.get(sr);
        assertTrue("The client should be a massive one", client.getClass().getName().equals(RestClient.class.getName()));
    }

    private static List<RepoData> getRepoDataList() {
        if (repoDataCache == null) {
            repoDataCache = new ArrayList<RepoData>();
            for (Object[] param : FatUtils.getRestFixtureParameters()) {
                RepoData data = new RepoData();
                data.fixture = (RepositoryFixture) param[0];
                repoDataCache.add(data);
            }
        }

        return repoDataCache;
    }

    private static class RepoData {
        RepositoryFixture fixture;
        FilterResources resources;

        @Override
        public String toString() {
            return fixture.toString();
        }
    }

    /**
     * <p>This will create the following resources:</p>
     * <table>
     * <thead><th>Name</th><th>Type</th><th>Description</th><th>Visibility</th><th>Applies To</th></thead>
     * <tbody>
     * <tr><td>Simple Feature</td><td>Feature</td><td>keyword1</td><td>Public</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0</td></tr>
     * <tr><td>Feature With No Version</td><td>Feature</td><td>keyword1 keyword2 keyword3</td><td>Private</td><td>com.ibm.ws.wlp</td></tr>
     * <tr><td>Feature with later version</td><td>Feature</td><td>keyword1 keyword3</td><td>Public</td><td>com.ibm.ws.wlp; productVersion=8.5.5.4</td></tr>
     * <tr><td>Feature other product</td><td>Feature</td><td>keyword1</td><td>Public</td><td>com.ibm.ws.plw; productVersion=1.0.0.0</td></tr>
     * <tr><td>Feature other product but same version</td><td>Feature</td><td>keyword1</td><td>Public</td><td>com.ibm.ws.plw; productVersion=8.5.5.4</td></tr>
     * <tr><td>Feature two products</td><td>Feature</td><td>keyword1</td><td>Public</td><td>com.ibm.ws.plw; productVersion=1.0.0.0, com.ibm.ws.wlp; productVersion=8.5.5.0</td></tr>
     * <tr><td>Feature with editions</td><td>Feature</td><td>keyword1 keyword2 keyword3</td><td>Protected</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0;
     * productEdition="BASE,DEVELOPERS"</td></tr>
     * <tr><td>Feature with install type</td><td>Feature</td><td>keyword1</td><td>Install</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0; productInstallType=Archive</td></tr>
     * <tr><td>Sample</td><td>Prouct Sample</td><td>keyword1</td><td>n/a</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0+</td></tr>
     * <tr><td>Addon</td><td>Addon</td><td>keyword1</td><td>n/a</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0</td></tr>
     * </tbody>
     * </table>
     *
     * @return a data object containing the resources
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     * @throws IOException
     * @throws ProtocolException
     * @throws MalformedURLException
     */
    private static FilterResources setupRepoForFilterTests(RepositoryFixture fixture) throws RepositoryResourceException, RepositoryBackendException,
                    MalformedURLException, ProtocolException, IOException {
        RepositoryConnection repoConnection = fixture.getAdminConnection();

        UploadStrategy uploader = new AddNewStrategy(null, State.PUBLISHED);
        EsaResourceImpl simpleFeature = new EsaResourceImpl(repoConnection);
        simpleFeature.setName("Simple Feature");
        simpleFeature.setDescription("keyword1");
        simpleFeature.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0");
        simpleFeature.setVisibility(Visibility.PUBLIC);
        simpleFeature.setProvideFeature("com.ibm.ws.simpleFeature-1.0");
        simpleFeature.setShortName("simpleFeature-1.0");
        simpleFeature.uploadToMassive(uploader);

        EsaResourceImpl featureWithNoVersion = new EsaResourceImpl(repoConnection);
        featureWithNoVersion.setName("Feature With No Version");
        featureWithNoVersion.setDescription("keyword1 keyword2 keyword3");
        featureWithNoVersion.setAppliesTo("com.ibm.ws.wlp");
        featureWithNoVersion.setVisibility(Visibility.PRIVATE);
        featureWithNoVersion.setProvideFeature("com.ibm.ws.noVersionFeature-1.0");
        featureWithNoVersion.setShortName("noVersionFeature-1.0");
        featureWithNoVersion.uploadToMassive(uploader);

        EsaResourceImpl featureWithLaterVersion = new EsaResourceImpl(repoConnection);
        featureWithLaterVersion.setName("Feature with later version");
        featureWithLaterVersion.setDescription("keyword1 keyword3");
        featureWithLaterVersion.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.4");
        featureWithLaterVersion.setVisibility(Visibility.PUBLIC);
        featureWithLaterVersion.uploadToMassive(uploader);

        EsaResourceImpl featureOtherProduct = new EsaResourceImpl(repoConnection);
        featureOtherProduct.setName("Feature other product");
        featureOtherProduct.setDescription("keyword1");
        featureOtherProduct.setAppliesTo("com.ibm.ws.plw; productVersion=1.0.0.0");
        featureOtherProduct.setVisibility(Visibility.PUBLIC);
        featureOtherProduct.uploadToMassive(uploader);

        EsaResourceImpl featureOtherProductSameVersion = new EsaResourceImpl(repoConnection);
        featureOtherProductSameVersion.setName("Feature other product same version");
        featureOtherProductSameVersion.setDescription("keyword1");
        featureOtherProductSameVersion.setAppliesTo("com.ibm.ws.plw; productVersion=8.5.5.0");
        featureOtherProductSameVersion.setVisibility(Visibility.PUBLIC);
        featureOtherProductSameVersion.uploadToMassive(uploader);

        EsaResourceImpl featureTwoProducts = new EsaResourceImpl(repoConnection);
        featureTwoProducts.setName("Feature two products");
        featureTwoProducts.setDescription("keyword1");
        featureTwoProducts.setAppliesTo("com.ibm.ws.plw; productVersion=1.0.0.0, com.ibm.ws.wlp; productVersion=8.5.5.0");
        featureTwoProducts.setVisibility(Visibility.PUBLIC);
        featureTwoProducts.uploadToMassive(uploader);

        EsaResourceImpl featureWithEditions = new EsaResourceImpl(repoConnection);
        featureWithEditions.setName("Feature with editions");
        featureWithEditions.setDescription("keyword1 keyword2 keyword3");
        featureWithEditions.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0; productEdition=\"BASE,DEVELOPERS\"");
        featureWithEditions.setVisibility(Visibility.PROTECTED);
        featureWithEditions.uploadToMassive(uploader);

        EsaResourceImpl featureWithInstallType = new EsaResourceImpl(repoConnection);
        featureWithInstallType.setName("Feature with install type");
        featureWithInstallType.setDescription("keyword1");
        featureWithInstallType.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0; productInstallType=Archive");
        featureWithInstallType.setVisibility(Visibility.INSTALL);
        featureWithInstallType.uploadToMassive(uploader);

        SampleResourceImpl sample = new SampleResourceImpl(repoConnection);
        sample.setName("Sample");
        sample.setDescription("keyword1");
        sample.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0+");
        sample.setType(ResourceType.PRODUCTSAMPLE);
        sample.uploadToMassive(uploader);

        ProductResourceImpl addon = new ProductResourceImpl(repoConnection);
        addon.setName("Addon");
        addon.setDescription("keyword1");
        addon.setType(ResourceType.ADDON);
        addon.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0");
        addon.setProductId("com.ibm.websphere.appserver");
        addon.uploadToMassive(uploader);

        logger.log(Level.INFO, "Refreshing elastic search and checking for whether the Addon entry (the last added) is indexed");
        fixture.refreshTextIndex(addon.getId());

        return new FilterResources(simpleFeature, featureWithNoVersion, featureWithLaterVersion, featureOtherProduct, featureOtherProductSameVersion, featureTwoProducts, featureWithEditions, featureWithInstallType, sample, addon);
    }

    private static class FilterResources {

        public enum Resources {
            SIMPLE_FEATURE, FEATURE_WITH_NO_VERSION, FEATURE_WITH_LATER_VERSION, FEATURE_OTHER_PRODUCT, FEATURE_OTHER_PRODUCT_SAME_VERSION, FEATURE_TWO_PRODUCTS,
            FEATURE_WITH_EDITIONS, FEATURE_WITH_INSTALL_TYPE, SAMPLE, ADDON
        }

        private final Map<Resources, RepositoryResourceImpl> allResources;

        public FilterResources(EsaResourceImpl simpleFeature, EsaResourceImpl featureWithNoVersion, EsaResourceImpl featureWithLaterVersion, EsaResourceImpl featureOtherProduct,
                               EsaResourceImpl featureOtherProductSameVersion, EsaResourceImpl featureTwoProducts, EsaResourceImpl featureWithEditions,
                               EsaResourceImpl featureWithInstallType,
                               SampleResourceImpl sample, ProductRelatedResourceImpl addon) {
            super();
            allResources = new HashMap<FilterResources.Resources, RepositoryResourceImpl>();
            allResources.put(Resources.SIMPLE_FEATURE, simpleFeature);
            allResources.put(Resources.FEATURE_WITH_NO_VERSION, featureWithNoVersion);
            allResources.put(Resources.FEATURE_WITH_LATER_VERSION, featureWithLaterVersion);
            allResources.put(Resources.FEATURE_OTHER_PRODUCT, featureOtherProduct);
            allResources.put(Resources.FEATURE_OTHER_PRODUCT_SAME_VERSION, featureOtherProductSameVersion);
            allResources.put(Resources.FEATURE_TWO_PRODUCTS, featureTwoProducts);
            allResources.put(Resources.FEATURE_WITH_EDITIONS, featureWithEditions);
            allResources.put(Resources.FEATURE_WITH_INSTALL_TYPE, featureWithInstallType);
            allResources.put(Resources.SAMPLE, sample);
            allResources.put(Resources.ADDON, addon);
        }

        public void validateReturnedResources(Collection<? extends RepositoryResource> resources, EnumSet<Resources> expectedResources) {
            assertEquals("Wrong number of resources returned\n" + resources, expectedResources.size(), resources.size());
            for (Resources expectedResource : expectedResources) {
                assertTrue(expectedResource.toString() + " should be included", resources.contains(allResources.get(expectedResource)));
            }
        }
    }

}
