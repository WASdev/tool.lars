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

import static com.ibm.ws.lars.rest.TestUtils.assertAssetList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.lars.rest.Condition.Operation;
import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.AttachmentContentMetadata;
import com.ibm.ws.lars.testutils.FatUtils;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

public class PersistenceBeanTest {

    // TODO read all this stuff from config somewhere
    private static final String DB_NAME = "testdb";
    private static final WriteConcern WRITE_CONCERN = WriteConcern.JOURNAL_SAFE;

    private MongoClient mongoClient;
    private PersistenceBean persistenceBean;
    private DB db;

    @Before
    public void setUp() throws Exception {

        // Inject a MongoDB connection into our persistence bean to test it
        //
        // At present, we use a real live MongoDB but maybe in the future
        // we should mock it.
        //
        // Also we currently don't bother with Mongo security but clearly we
        // might like to think about that at some point
        mongoClient = new MongoClient("localhost:" + FatUtils.DB_PORT);
        mongoClient.setWriteConcern(WRITE_CONCERN);

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

    }

    @After
    public void tearDown() throws Exception {

        db.dropDatabase();
        mongoClient.close();
    }

    private void assertEmpty() throws IOException {
        AssetList allAssets = persistenceBean.retrieveAllAssets();
        assertTrue(allAssets.size() == 0);
    }

    /**
     * Simple test that does create. retrieve, update and delete on one object.
     *
     * @throws InvalidJsonAssetException
     * @throws AssetNotFoundException
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
            byte[] returnedContent = TestUtils.slurp(contentStream);
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

        Map<String, List<Condition>> filters = new HashMap<>();

        filters.put("name", Arrays.asList(eq("new name1")));
        AssetList assets = persistenceBean.retrieveAllAssets(filters);
        assertEquals("Should only have got 1 asset back", 1, assets.size());
        assertEquals("Got the wrong asset back", asset1.get_id(), assets.get(0).get_id());

        Map<String, List<Condition>> filters2 = new HashMap<>();
        filters2.put("layer1.layer1field", Arrays.asList(eq("layer1value")));
        AssetList assets2 = persistenceBean.retrieveAllAssets(filters2);
        assertEquals("Should have got 2 asset back", 2, assets2.size());
        for (Asset retrievedAsset : assets2) {
            if (!retrievedAsset.get_id().equals(asset1.get_id()) && !retrievedAsset.get_id().equals(asset2.get_id())) {
                fail("The wrong asset was retrieved. Asset id " + retrievedAsset.get_id() + " was retrieved. Expected " + asset1.get_id() + " or " + asset2.get_id());
            }
        }

        Map<String, List<Condition>> filters3 = new HashMap<>();
        filters3.put("name", Arrays.asList(eq("new name1"), eq("new name2")));
        AssetList assets3 = persistenceBean.retrieveAllAssets(filters3);
        assertEquals("Should have got 2 asset back", 2, assets3.size());
        for (Asset retrievedAsset : assets3) {
            if (!retrievedAsset.get_id().equals(asset1.get_id()) && !retrievedAsset.get_id().equals(asset2.get_id())) {
                fail("The wrong asset was retrieved. Asset id " + retrievedAsset.get_id() + " was retrieved. Expected " + asset1.get_id() + " or " + asset2.get_id());
            }
        }

    }

    @Test
    public void testRetrieveAllFiltered2() throws Exception {
        Asset asset1 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name1\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        Asset asset2 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name1\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name2\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name3\", \"layer1\":{\"layer1field\":\"layer1value3\",\"layer2\":\"layer2value\"}}"));

        // Test with an empty set of filters
        Map<String, List<Condition>> emptyFilters = Collections.emptyMap();
        AssetList assets = persistenceBean.retrieveAllAssets(emptyFilters);
        assertEquals("An empty filter should get all assets", 4, assets.size());

        Map<String, List<Condition>> filters = new HashMap<>();
        // test which retrieves no assets
        filters.put("blurgh", Arrays.asList(eq("new name1")));
        AssetList assets2 = persistenceBean.retrieveAllAssets(filters);
        assertEquals("Should not have got any assets back", 0, assets2.size());

        // test which uses multiple entries in the map
        filters.remove("blurgh");
        filters.put("name", Arrays.asList(eq("new name1")));
        filters.put("layer1.layer1field", Arrays.asList(eq("layer1value")));

        AssetList assets3 = persistenceBean.retrieveAllAssets(filters);
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

    /**
     * Test for {@link PersistenceBean#retrieveAllAssets(Map, Map)}
     *
     * @throws InvalidJsonAssetException
     *
     * @throws Exception
     */
    @Test
    public void testRetrieveAllAssetsNotFiltered() throws InvalidJsonAssetException {
        Asset asset1 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name1\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        Asset asset2 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name1\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        Asset asset3 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name2\", \"layer1\":{\"layer1field\":\"layer1value\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));

        // Empty filters should get everything
        Map<String, List<Condition>> emptyFilters = Collections.emptyMap();
        AssetList allAssets = persistenceBean.retrieveAllAssets(emptyFilters);
        assertEquals("Unexpected number of assets returned", 3, allAssets.size());

        Map<String, List<Condition>> filters;

        // query that should return nothing
        filters = new HashMap<>();
        filters.put("layer1.layer1field", Arrays.asList(neq("layer1value")));
        AssetList emptyAssets = persistenceBean.retrieveAllAssets(filters);
        assertEquals("Unexpected number of assets returned", 0, emptyAssets.size());
        filters.clear();

        // basic not filter
        filters = new HashMap<>();
        filters.put("name", Arrays.asList(neq("new name1")));
        AssetList assets1 = persistenceBean.retrieveAllAssets(filters);
        assertEquals("Unexpected number of assets returned", 1, assets1.size());
        assertEquals("The wrong asset id was retrieved", asset3.get_id(), assets1.get(0).get_id());

        // not filter and a normal filter
        Asset asset4 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"name\":\"new name2\", \"layer1\":{\"layer1field\":\"layer1value2\",\"layer2\":{\"layer2field\":\"layer2value\",\"layer3\":{\"layer3field\":\"layer3value\"}}}}"));
        filters = new HashMap<>();
        filters.put("name", Arrays.asList(neq("new name1")));
        filters.put("layer1.layer1field", Arrays.asList(eq("layer1value2")));
        AssetList assets2 = persistenceBean.retrieveAllAssets(filters);
        assertEquals("Unexpected number of assets returned", 1, assets2.size());
        assertEquals("The wrong asset id was retrieved", asset4.get_id(), assets2.get(0).get_id());

    }

    @Test
    public void testRetrieveAllAssetsOrFiltered() throws InvalidJsonAssetException {
        Asset asset1 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"flat\"}"));
        Asset asset2 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"hilly\"}"));
        Asset asset3 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"hot\", \"ground\":\"mountainous\"}"));
        Asset asset4 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"flat\"}"));
        Asset asset5 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"hilly\"}"));
        Asset asset6 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"cold\", \"ground\":\"mountainous\"}"));
        Asset asset7 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"flat\"}"));
        Asset asset8 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"hilly\"}"));
        Asset asset9 = persistenceBean.createAsset(Asset.deserializeAssetFromJson("{\"weather\":\"warm\", \"ground\":\"mountainous\"}"));

        // Empty filters should get everything
        Map<String, List<Condition>> emptyFilters = Collections.emptyMap();
        AssetList allAssets = persistenceBean.retrieveAllAssets(emptyFilters);
        assertEquals("Unexpected number of assets returned", 9, allAssets.size());
        assertAssetList(allAssets, asset1, asset2, asset3, asset4, asset5, asset6, asset7, asset8, asset9);

        Map<String, List<Condition>> filters;

        // Simple OR filter
        filters = new HashMap<>();
        filters.put("weather", Arrays.asList(eq("hot"), eq("warm")));
        AssetList result1 = persistenceBean.retrieveAllAssets(filters);
        assertAssetList(result1, asset1, asset2, asset3, asset7, asset8, asset9);

        // OR with NOT
        filters = new HashMap<>();
        filters.put("weather", Arrays.asList(eq("hot"), eq("warm")));
        filters.put("ground", Arrays.asList(neq("mountainous")));
        AssetList result2 = persistenceBean.retrieveAllAssets(filters);
        assertAssetList(result2, asset1, asset2, asset7, asset8);

        // Two ORs
        filters = new HashMap<>();
        filters.put("weather", Arrays.asList(eq("hot"), eq("warm")));
        filters.put("ground", Arrays.asList(eq("hilly"), eq("mountainous")));
        AssetList result3 = persistenceBean.retrieveAllAssets(filters);
        assertAssetList(result3, asset2, asset3, asset8, asset9);
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
}
