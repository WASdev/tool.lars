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

package com.ibm.ws.lars.rest;

import static com.ibm.ws.lars.rest.SortOptions.SortOrder.ASCENDING;
import static com.ibm.ws.lars.rest.SortOptions.SortOrder.DESCENDING;
import static com.ibm.ws.lars.rest.matchers.ServerAssetByIdMatcher.assetsWithIds;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.mongodb.MongoClientOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.lars.rest.Condition.Operation;
import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.exceptions.NonExistentArtefactException;
import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetCursor;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.AttachmentContentMetadata;
import com.ibm.ws.lars.rest.mongo.PersistenceBean;
import com.ibm.ws.lars.testutils.BasicChecks;
import com.ibm.ws.lars.testutils.FatUtils;
import com.mongodb.DB;
import com.mongodb.MongoClient;

import mockit.Mocked;

public class PersistenceBeanTest {

    // TODO Should the db name be configurable?
    private static final String DB_NAME = "testdb";

    private MongoClient mongoClient;
    private PersistenceBean persistenceBean;
    private DB db;

    // We needed to mock the logger here because something about running other jmockit tests
    // which mock it meant that the Logger class wasn't getting initialized properly and would
    // null pointer unexpectedly. Probably a bug in jmockit but the easiest thing is just to
    // always mock the logger.
    @Mocked
    Logger logger;

    @Before
    public void setUp() throws Exception {

        // Inject a MongoDB connection into our persistence bean to test it
        //
        // At present, we use a real live MongoDB but maybe in the future
        // we should mock it.
        //
        // Also we currently don't bother with Mongo security but clearly we
        // might like to think about that at some point
        mongoClient = new MongoClient("localhost:" + FatUtils.DB_PORT, new MongoClientOptions.Builder().build());

        db = mongoClient.getDB(DB_NAME);

        persistenceBean = new PersistenceBean();

        // @Inject the DB
        Field dbField = PersistenceBean.class.getDeclaredField("db");
        dbField.setAccessible(true);
        dbField.set(persistenceBean, db);

        // This is a @PostConstruct method so must call it
        persistenceBean.createGridFS();

        // Ensure we start the test with nothing in the DB
        db.dropDatabase();

        // Mainly to make sure there is text index for the search tests.
        persistenceBean.initialize();

    }

    @After
    public void tearDown() throws Exception {

        db.dropDatabase();
        mongoClient.close();
    }

    private void assertEmpty() throws IOException {
        AssetCursor allAssets = persistenceBean.retrieveAllAssets();
        assertTrue(allAssets.size() == 0);
    }

    /**
     * Simple test that does create. retrieve, update and delete on one object.
     *
     * @throws IOException
     * @throws InvalidJsonAssetException
     * @throws NonExistentArtefactException
     */
    @Test
    public void testCRUD() throws IOException, InvalidJsonAssetException, NonExistentArtefactException {

        // Verify that an empty collection is indeed empty
        assertEmpty();

        // Create
        Asset asset = new Asset();
        String[] keys = new String[] { "name", "wibble", "abyte", "ashort", "anint", "along", "achar" };
        Object[] values = new Object[] { "this is my name", "wibble", (byte) 23, (short) -1, 65536, 12345678901L, 'a' };
        putAll(asset.getProperties(), keys, values);

        // Retrieve
        Asset returnedAsset = persistenceBean.createAsset(asset);
        String id = returnedAsset.get_id();
        assertNotNull("id should not be null", id);

        // verify that the objects are equal in every way apart from their ids
        asset.set_id(returnedAsset.get_id());
        // This fails, as the id needs to go in at the front
        // (LinkedHashMap checks the order)
        // assertEquals(asset, returnedMap);

        // Update

        returnedAsset.put("anint", 24);
        persistenceBean.updateAsset(id, returnedAsset);
        Asset returnedAssetAfterPut = persistenceBean.retrieveAsset(id);
        assertEquals(returnedAsset, returnedAssetAfterPut);

        // Delete
        persistenceBean.deleteAsset(id);
        assertEmpty();

    }

    /**
     * Tests that attachment (JSON) metadata can be stored in and retrieved from the
     * PersistenceBean.
     */
    @Test
    public void testStoreAndRetrieveAttachmentMetadata() throws NonExistentArtefactException {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("AnInt", 12);
        properties.put("AString", "This is a string");
        Attachment attachment = Attachment.createAttachmentFromMap(properties);
        Attachment createdAttachment = persistenceBean.createAttachmentMetadata(attachment);
        Attachment returnedAttachment = persistenceBean.retrieveAttachmentMetadata(createdAttachment.get_id());

        assertNotNull(returnedAttachment.get_id());
        returnedAttachment.getProperties().remove(Attachment._ID);

        assertEquals(attachment, returnedAttachment);
    }

    /**
     * Tests that attachment content (ie arbitrary binary data) can be stored in and retrieved from
     * the PersistenceBean.
     */
    @Test
    public void testStoreAndRetrieveAttachmentContent() throws IOException, NonExistentArtefactException {
        byte[] content = "This is a very small amount of content".getBytes();
        AttachmentContentMetadata contentMetadata = persistenceBean.createAttachmentContent("MrAttachment.txt",
                                                                                            "test/plain",
                                                                                            new ByteArrayInputStream(content));
        assertNotNull(contentMetadata.filename);
        assertEquals(content.length, contentMetadata.length);

        try (InputStream contentStream = persistenceBean.retrieveAttachmentContent(contentMetadata.filename).getContentStream()) {
            byte[] returnedContent = BasicChecks.slurp(contentStream);
            assertTrue(Arrays.equals(content, returnedContent));
        }
    }

    /**
     * Verifies that an exception is thrown when we attempt to retrieve a non-existent asset.
     */
    @Test(expected = NonExistentArtefactException.class)
    public void testRetrieveNonExistentAsset() throws NonExistentArtefactException {
        persistenceBean.retrieveAsset("123456789012345678901234");
    }

    /**
     * Verifies that an exception is thrown when we attempt to retrieve the metadata for a
     * non-existent attachment.
     */
    @Test(expected = NonExistentArtefactException.class)
    public void testRetrieveNonExistentAttachmentMetadata() throws NonExistentArtefactException {
        persistenceBean.retrieveAttachmentMetadata("123456789012345678901234");
    }

    /**
     * Verifies that an exception is thrown when we attempt to retrieve the content for a
     * non-existent attachment
     */
    @Test(expected = NonExistentArtefactException.class)
    public void testRetrieveNonExistentAttachmentContent() throws NonExistentArtefactException {
        persistenceBean.retrieveAttachmentContent("123456789012345678901234");
    }

    @Test
    public void testRetrieveAllFiltered() throws Exception {
        Asset asset1 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name1\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        Asset asset2 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name2\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name3\", \"layer1\":{\"layer1field\":\"layer1value3\",\"layer2\":\"layer2value\"}}"));

        List<AssetFilter> filters = new ArrayList<>();
        filters.add(new AssetFilter("name", Arrays.asList(eq("new name1"))));
        List<Asset> assets = readAll(persistenceBean.retrieveAllAssets(filters, null, null, null));
        assertEquals("Should only have got 1 asset back", 1, assets.size());
        assertEquals("Got the wrong asset back", asset1.get_id(), assets.get(0).get_id());

        List<AssetFilter> filters2 = new ArrayList<>();
        filters2.add(new AssetFilter("layer1.layer1field", Arrays.asList(eq("layer1value"))));

        List<Asset> assets2 = readAll(persistenceBean.retrieveAllAssets(filters2, null, null, null));
        assertEquals("Should have got 2 asset back", 2, assets2.size());
        for (Asset retrievedAsset : assets2) {
            if (!retrievedAsset.get_id().equals(asset1.get_id()) && !retrievedAsset.get_id().equals(asset2.get_id())) {
                fail("The wrong asset was retrieved. Asset id " + retrievedAsset.get_id() + " was retrieved. Expected " + asset1.get_id() + " or " + asset2.get_id());
            }
        }

        List<AssetFilter> filters3 = new ArrayList<>();
        filters3.add(new AssetFilter("name", Arrays.asList(eq("new name1"), eq("new name2"))));

        List<Asset> assets3 = readAll(persistenceBean.retrieveAllAssets(filters3, null, null, null));
        assertEquals("Should have got 2 asset back", 2, assets3.size());
        for (Asset retrievedAsset : assets3) {
            if (!retrievedAsset.get_id().equals(asset1.get_id()) && !retrievedAsset.get_id().equals(asset2.get_id())) {
                fail("The wrong asset was retrieved. Asset id " + retrievedAsset.get_id() + " was retrieved. Expected " + asset1.get_id() + " or " + asset2.get_id());
            }
        }

        // With a search term as well
        List<AssetFilter> filters4 = new ArrayList<>();
        filters4.add(new AssetFilter("name", Arrays.asList(eq("new name1"), eq("new name2"))));
        List<Asset> assets4 = readAll(persistenceBean.retrieveAllAssets(filters4, "name1", null, null));
        assertEquals("Wrong number of assets retrieved", 1, assets4.size());
        Asset retrieved = assets4.get(0);
        assertEquals("Got the wrong asset back", asset1.get_id(), retrieved.get_id());
        // Checking that the sorting score isn't left in the asset
        assertEquals("The asset has been modified", asset1, retrieved);

    }

    @Test
    public void testRetrieveAllFiltered2() throws Exception {
        Asset asset1 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name1\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        Asset asset2 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name1\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name2\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name3\", \"layer1\":{\"layer1field\":\"layer1value3\",\"layer2\":\"layer2value\"}}"));

        // Test with an empty set of filters
        List<AssetFilter> emptyFilters = Collections.emptyList();
        List<Asset> assets = readAll(persistenceBean.retrieveAllAssets(emptyFilters, null, null, null));
        assertEquals("An empty filter should get all assets", 4, assets.size());

        List<AssetFilter> filters = new ArrayList<>();
        // test which retrieves no assets
        filters.add(new AssetFilter("blurgh", Arrays.asList(eq("new name1"))));
        List<Asset> assets2 = readAll(persistenceBean.retrieveAllAssets(filters, null, null, null));
        assertEquals("Should not have got any assets back", 0, assets2.size());

        // test which uses multiple entries in the map
        filters.clear();
        filters.add(new AssetFilter("name", Arrays.asList(eq("new name1"))));
        filters.add(new AssetFilter("layer1.layer1field", Arrays.asList(eq("layer1value"))));

        List<Asset> assets3 = readAll(persistenceBean.retrieveAllAssets(filters, null, null, null));
        assertEquals("Wrong number of assets retrieved", 2, assets3.size());
        String id1 = asset1.get_id();
        String id2 = asset2.get_id();
        for (Asset retrievedAsset : assets3) {
            String id = retrievedAsset.get_id();
            if (!id.equals(id1) && !id.equals(id2)) {
                fail("The wrong asset was retrieved. Expected id " + id1 + " or " + id2 + " but got " + id);
            }
        }

    }

    @SuppressWarnings("unused")
    @Test
    public void testRetrieveAllAssetsNotFiltered() throws InvalidJsonAssetException {
        Asset asset1 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name1\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        Asset asset2 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name1\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        Asset asset3 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name2\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));

        // Empty filters should get everything
        List<AssetFilter> emptyFilters = Collections.emptyList();
        List<Asset> allAssets = readAll(persistenceBean.retrieveAllAssets(emptyFilters, null, null, null));
        assertEquals("Unexpected number of assets returned", 3, allAssets.size());

        List<AssetFilter> filters;

        // query that should return nothing
        filters = new ArrayList<>();
        filters.add(new AssetFilter("layer1.layer1field", Arrays.asList(neq("layer1value"))));
        List<Asset> emptyAssets = readAll(persistenceBean.retrieveAllAssets(filters, null, null, null));
        assertEquals("Unexpected number of assets returned", 0, emptyAssets.size());
        filters.clear();

        // basic not filter
        filters = new ArrayList<>();
        filters.add(new AssetFilter("name", Arrays.asList(neq("new name1"))));
        List<Asset> assets1 = readAll(persistenceBean.retrieveAllAssets(filters, null, null, null));
        assertEquals("Unexpected number of assets returned", 1, assets1.size());
        assertEquals("The wrong asset id was retrieved", asset3.get_id(), assets1.get(0).get_id());

        // not filter and a normal filter
        Asset asset4 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name2\", \"layer1\":{\"layer1field\":\"layer1value2\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        filters = new ArrayList<>();
        filters.add(new AssetFilter("name", Arrays.asList(neq("new name1"))));
        filters.add(new AssetFilter("layer1.layer1field", Arrays.asList(eq("layer1value2"))));
        List<Asset> assets2 = readAll(persistenceBean.retrieveAllAssets(filters, null, null, null));
        assertEquals("Unexpected number of assets returned", 1, assets2.size());
        assertEquals("The wrong asset id was retrieved", asset4.get_id(), assets2.get(0).get_id());

        // not filter and a normal filter and a search term
        filters = new ArrayList<>();
        filters.add(new AssetFilter("name", Arrays.asList(neq("new name1"))));
        filters.add(new AssetFilter("layer1.layer1field", Arrays.asList(eq("layer1value2"))));
        List<Asset> assets3 = readAll(persistenceBean.retrieveAllAssets(filters, "\"new name2\"", null, null));
        assertEquals("Unexpected number of assets returned", 1, assets3.size());
        assertEquals("The wrong asset id was retrieved", asset4.get_id(), assets3.get(0).get_id());

    }

    @Test
    public void testRetrieveAllAssetsOrFiltered() throws InvalidJsonAssetException {
        Asset asset1 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"flat\", \"name\":\"a long name\"}"));
        Asset asset2 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"hilly\", \"name\":\"a long name\"}"));
        Asset asset3 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"mountainous\"}"));
        Asset asset4 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"flat\"}"));
        Asset asset5 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"hilly\"}"));
        Asset asset6 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"mountainous\"}"));
        Asset asset7 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"flat\", \"name\":\"a long name\"}"));
        Asset asset8 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"hilly\"}"));
        Asset asset9 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"mountainous\"}"));

        // Empty filters should get everything
        List<AssetFilter> emptyFilters = Collections.emptyList();
        List<Asset> allAssets = readAll(persistenceBean.retrieveAllAssets(emptyFilters, null, null, null));
        assertEquals("Unexpected number of assets returned", 9, allAssets.size());
        assertThat(allAssets, containsInAnyOrder(assetsWithIds(asset1, asset2, asset3, asset4, asset5, asset6, asset7, asset8, asset9)));

        List<AssetFilter> filters;

        // Simple OR filter
        filters = new ArrayList<>();
        filters.add(new AssetFilter("weather", Arrays.asList(eq("hot"), eq("warm"))));
        List<Asset> result1 = readAll(persistenceBean.retrieveAllAssets(filters, null, null, null));
        assertThat(result1, containsInAnyOrder(assetsWithIds(asset1, asset2, asset3, asset7, asset8, asset9)));

        // OR with NOT
        filters = new ArrayList<>();
        filters.add(new AssetFilter("weather", Arrays.asList(eq("hot"), eq("warm"))));
        filters.add(new AssetFilter("ground", Arrays.asList(neq("mountainous"))));
        List<Asset> result2 = readAll(persistenceBean.retrieveAllAssets(filters, null, null, null));
        assertThat(result2, containsInAnyOrder(assetsWithIds(asset1, asset2, asset7, asset8)));

        // Two ORs
        filters = new ArrayList<>();
        filters.add(new AssetFilter("weather", Arrays.asList(eq("hot"), eq("warm"))));
        filters.add(new AssetFilter("ground", Arrays.asList(eq("hilly"), eq("mountainous"))));
        List<Asset> result3 = readAll(persistenceBean.retrieveAllAssets(filters, null, null, null));
        assertThat(result3, containsInAnyOrder(assetsWithIds(asset2, asset3, asset8, asset9)));

        // OR with NOT and searchTerm
        filters = new ArrayList<>();
        filters.add(new AssetFilter("weather", Arrays.asList(eq("hot"), eq("warm"))));
        filters.add(new AssetFilter("ground", Arrays.asList(neq("mountainous"))));
        List<Asset> result4 = readAll(persistenceBean.retrieveAllAssets(filters, "long", null, null));
        assertThat(result4, containsInAnyOrder(assetsWithIds(asset1, asset2, asset7)));
    }

    @Test
    public void testGetDistinctValues() throws InvalidJsonAssetException {
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"flat\", \"name\":\"hot and flat\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"hilly\", \"name\":\"hot and hilly\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"mountainous\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"flat\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"hilly\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"mountainous\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"flat\", \"name\":\"warm and flat\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"hilly\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"mountainous\"}"));

        List<AssetFilter> filters;

        filters = Collections.emptyList();
        List<Object> weathers = persistenceBean.getDistinctValues("weather", filters, null);
        assertThat("Wrong list of possible weathers", weathers, containsInAnyOrder((Object) "hot", "warm", "cold"));

        List<Object> names = persistenceBean.getDistinctValues("name", filters, null);
        assertThat("Wrong list of possible names", names, containsInAnyOrder((Object) "hot and flat", "hot and hilly", "warm and flat"));

        filters = new ArrayList<>();
        filters.add(new AssetFilter("weather", Arrays.asList(eq("hot"))));
        List<Object> hotNames = persistenceBean.getDistinctValues("name", filters, null);
        assertThat("Wrong list of possible names with weather=hot", hotNames, containsInAnyOrder((Object) "hot and flat", "hot and hilly"));

        filters = Collections.emptyList();
        List<Object> searchNames = persistenceBean.getDistinctValues("name", filters, "hot");
        assertThat("Wrong list of possible names with searchTerm=hot", searchNames, containsInAnyOrder((Object) "hot and flat", "hot and hilly"));
    }

    @Test
    public void testPagination() throws Exception {
        Asset asset1 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"asset1\"}"));
        Asset asset2 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"asset2\"}"));
        Asset asset3 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"asset3\"}"));
        Asset asset4 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"asset4\"}"));

        List<AssetFilter> emptyFilter = Collections.emptyList();

        // Test 2 per page
        List<Asset> page1 = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, new PaginationOptions(0, 2), null));
        assertEquals("Wrong number of assets on page 1", 2, page1.size());
        List<Asset> page2 = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, new PaginationOptions(2, 2), null));
        assertEquals("Wrong number of assets on page 2", 2, page2.size());
        List<Asset> page3 = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, new PaginationOptions(4, 2), null));
        assertEquals("Wrong number of assets on page 3", 0, page3.size());
        assertThat(collatePages(page1, page2, page3), containsInAnyOrder(asset1, asset2, asset3, asset4));

        // Test 3 per page
        page1 = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, new PaginationOptions(0, 3), null));
        assertEquals("Wrong number of assets on page 1", 3, page1.size());
        page2 = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, new PaginationOptions(3, 3), null));
        assertEquals("Wrong number of assets on page 2", 1, page2.size());
        page3 = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, new PaginationOptions(6, 3), null));
        assertEquals("Wrong number of assets on page 3", 0, page3.size());
        assertThat(collatePages(page1, page2, page3), containsInAnyOrder(asset1, asset2, asset3, asset4));

        // Test with filter
        List<AssetFilter> conditions = new ArrayList<>();
        conditions.add(new AssetFilter("name", Arrays.asList(eq("asset2"), eq("asset3"), eq("asset4"))));
        page1 = readAll(persistenceBean.retrieveAllAssets(conditions, null, new PaginationOptions(0, 2), null));
        assertEquals("Wrong number of assets on page 1", 2, page1.size());
        page2 = readAll(persistenceBean.retrieveAllAssets(conditions, null, new PaginationOptions(2, 2), null));
        assertEquals("Wrong number of assets on page 2", 1, page2.size());
        assertThat(collatePages(page1, page2), containsInAnyOrder(asset2, asset3, asset4));
    }

    @Test
    public void testSortOptions() throws Exception {
        Asset asset1 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"asset1\"}"));
        Asset asset2 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"asset2\", \"score\":3}"));
        Asset asset3 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"asset3\", \"score\":2}"));
        Asset asset4 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"asset4\", \"score\":1}"));

        List<AssetFilter> emptyFilter = Collections.emptyList();

        List<Asset> result = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, null, new SortOptions("name", ASCENDING)));
        assertThat(result, contains(asset1, asset2, asset3, asset4));

        result = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, null, new SortOptions("name", DESCENDING)));
        assertThat(result, contains(asset4, asset3, asset2, asset1));

        // Missing values should be the "lowest"
        result = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, null, new SortOptions("score", ASCENDING)));
        assertThat(result, contains(asset1, asset4, asset3, asset2));

        result = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, null, new SortOptions("score", DESCENDING)));
        assertThat(result, contains(asset2, asset3, asset4, asset1));

        // Sort by something non-existent, the order is undefined but it should return all assets
        result = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, null, new SortOptions("wibble", ASCENDING)));
        assertThat(result, containsInAnyOrder(asset1, asset2, asset3, asset4));

        // Test sorting with pagination
        List<Asset> page1 = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, new PaginationOptions(0, 2), new SortOptions("name", ASCENDING)));
        List<Asset> page2 = readAll(persistenceBean.retrieveAllAssets(emptyFilter, null, new PaginationOptions(2, 2), new SortOptions("name", ASCENDING)));
        assertThat(collatePages(page1, page2), contains(asset1, asset2, asset3, asset4));
    }

    @Test
    public void testCountAllAssets() throws Exception {
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"flat\", \"name\":\"hot and flat\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"hilly\", \"name\":\"hot and hilly\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"mountainous\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"flat\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"hilly\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"mountainous\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"flat\", \"name\":\"warm and flat\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"hilly\"}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"mountainous\"}"));

        List<AssetFilter> emptyFilter = Collections.emptyList();

        // Test counting all assets
        int result = persistenceBean.countAllAssets(emptyFilter, null);
        assertEquals(9, result);

        // Test counting assets with a filter
        List<AssetFilter> filter = new ArrayList<>();
        filter.add(new AssetFilter("weather", Arrays.asList(eq("hot"))));
        result = persistenceBean.countAllAssets(filter, null);
        assertEquals(3, result);

        // Test counting assets with a search
        result = persistenceBean.countAllAssets(emptyFilter, "flat");
        assertEquals(2, result);

        // Test counting assets with a filter and a search
        filter.clear();
        filter.add(new AssetFilter("weather", Arrays.asList(eq("hot"))));
        result = persistenceBean.countAllAssets(filter, "flat");
        assertEquals(1, result);
    }

    /**
     * Collate the contents of several AssetLists into one List.
     * <p>
     * The asset lists are processed in order, each asset in each list is appended to the result
     * list.
     *
     * @param lists the AssetLists
     * @return the collated list
     */
    private static List<Asset> collatePages(List<?>... lists) {
        List<Asset> result = new ArrayList<Asset>();
        for (Object list : lists) {
            for (Object asset : (Iterable<?>) list) {
                result.add((Asset) asset);
            }
        }
        return result;
    }

    static void putAll(Map<String, Object> map, String[] keys, Object[] values) {
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
    }

    /**
     * Convenience method to create a condition checking for equality with the given value
     *
     * @param value the value
     * @return condition of equality with the given value
     */
    private static Condition eq(String value) {
        return new Condition(Operation.EQUALS, value);
    }

    /**
     * Convenience method to create a condition checking for non-equality with the given value
     *
     * @param value the value
     * @return condition of non-equality with the given value
     */
    private static Condition neq(String value) {
        return new Condition(Operation.NOT_EQUALS, value);
    }

    private static List<Asset> readAll(AssetCursor cursor) {
        List<Asset> result = new ArrayList<>();
        while (cursor.hasNext()) {
            result.add(cursor.next());
        }
        return result;
    }
}
