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

package com.ibm.ws.massive.sa.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.massive.sa.client.JSONAssetConverter;
import com.ibm.ws.massive.sa.client.model.Asset;
import com.ibm.ws.massive.sa.client.model.Asset.State;
import com.ibm.ws.massive.sa.client.model.Asset.Type;
import com.ibm.ws.massive.sa.client.model.Attachment;
import com.ibm.ws.massive.sa.client.model.Feedback;
import com.ibm.ws.massive.sa.client.model.Provider;

public class JSONAssetConverterTest {

    private final String ID = "theId";
    private Calendar CREATED_DATE;
    private final State STATE = State.DRAFT;
    private final Type TYPE = Type.IFIX;
    private final int TWEETS = Integer.MAX_VALUE;
    private final String ATTACHMENT1_NAME = "name1";
    private final String ATTACHMENT2_NAME = "name2";

    /**
     * This method will set the data values to appropriate things for the test
     * object
     */
    @Before
    public void setupTestData() {
        CREATED_DATE = Calendar.getInstance();
    }

    /**
     * This test will write a list of assets to a stream that is fed back into
     * the read asset to make sure that it both the write and read work ok.
     *
     * @throws IOException
     */
    @Test
    public void testAssetListConversion() throws Exception {
        Asset asset = createTestAsset();

        // Pipe write output to read call, need to wrap object in an array as we
        // only have write single object and want to read array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write('[');
        JSONAssetConverter.writeValue(outputStream, asset);
        outputStream.write(']');
        List<Asset> readInAssets = JSONAssetConverter
                        .readValues(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("We only serialized one asset so should have one back", 1,
                     readInAssets.size());
        Asset readInAsset = readInAssets.get(0);
        testAsset(readInAsset);
    }

    /**
     * Test to make sure a single asset can be read in from a stream
     *
     * @throws Exception
     */
    @Test
    public void testAssetConverstion() throws Exception {
        Asset asset = createTestAsset();

        // Pipe write output to read call
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JSONAssetConverter.writeValue(outputStream, asset);
        Asset readInAsset = JSONAssetConverter
                        .readValue(new ByteArrayInputStream(outputStream.toByteArray()));
        testAsset(readInAsset);
    }

    /**
     * This test will write an Asset to a stream that is fed back into the read
     * generic type to make sure that it both the write and read work ok.
     *
     * @throws IOException
     */
    @Test
    public void testGenericListConversion() throws Exception {
        Asset asset = createTestAsset();

        // Pipe write output to read call, need to wrap object in an array as we
        // only have write single object and want to read array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write('[');
        JSONAssetConverter.writeValue(outputStream, asset);
        outputStream.write(']');

        // Use the generic method
        List<Asset> readInAssets = JSONAssetConverter.readValues(
                                                                 new ByteArrayInputStream(outputStream.toByteArray()),
                                                                 Asset.class);
        assertEquals("We only serialized one asset so should have one back", 1,
                     readInAssets.size());
        Asset readInAsset = readInAssets.get(0);
        testAsset(readInAsset);
    }

    /**
     * Test to make sure a single generic type can be read in from a stream
     *
     * @throws Exception
     */
    @Test
    public void testGenericConverstion() throws Exception {
        Asset asset = createTestAsset();

        // Pipe write output to read call
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JSONAssetConverter.writeValue(outputStream, asset);

        // Use the generic method
        Asset readInAsset = JSONAssetConverter.readValue(
                                                         new ByteArrayInputStream(outputStream.toByteArray()),
                                                         Asset.class);
        testAsset(readInAsset);

    }

    /**
     * This tests that you can convert an asset with a provider to make sure the Url field
     * encodes / decodes correctly. (Originally Url, then URI, now String).
     *
     * @throws Exception
     */
    @Test
    public void testProviderConversion() throws Exception {
        Provider testProvider = new Provider();
        testProvider.setUrl("http://www.ibm.com");

        String writtenProvider = JSONAssetConverter.writeValueAsString(testProvider);
        assertTrue("The written provider should contain the supplied URL", writtenProvider.contains("www.ibm.com"));
        Provider readInProvider = JSONAssetConverter.readValue(new ByteArrayInputStream(writtenProvider.getBytes()), Provider.class);
        assertEquals("Read in provider should be equal to the original", testProvider, readInProvider);
    }

    /**
     * Makes sure the values are set to the test values in the supplied asset
     *
     * @param asset
     *            The asset to test
     */
    private void testAsset(Asset asset) {
        // Test all the values we set
        assertEquals("Write and read should produce the same string value", ID,
                     asset.get_id());
        assertEquals("Write and read should produce the same Calendar value",
                     CREATED_DATE, asset.getCreatedOn());
        assertEquals(
                     "Write and read should produce the same simple enum value",
                     STATE, asset.getState());
        assertEquals(
                     "Write and read should produce the same complex enum value",
                     TYPE, asset.getType());
        Feedback readInFeedback = asset.getFeedback();
        assertNotNull("Nested object should of written and read",
                      readInFeedback);
        assertEquals("Write and read should produce the same int value",
                     TWEETS, readInFeedback.getTweets());
        List<Attachment> readInAttachments = asset.getAttachments();
        assertNull("The asset (obtained from a get all), should not have any attachments until they are read",
                   readInAttachments);

        // Also make sure that lists and objects that weren't set are still null
        assertNull("Strings that weren't set should be null",
                   asset.getDescription());
        assertNull("Objects that weren't set should be null",
                   asset.getCreatedBy());
    }

    /**
     * Creates a test asset with various sub-objects and different types set
     *
     * @return The asset
     */
    private Asset createTestAsset() {
        Asset asset = new Asset();

        // Set a few test fields, don't do all of them but a representative
        // sample
        // String
        asset.set_id(ID);

        // Calendar
        asset.setCreatedOn(CREATED_DATE);

        // Single object
        Feedback feedback = new Feedback();
        asset.setFeedback(feedback);

        // Simple enum (uses name())
        asset.setState(STATE);

        // Complex enum uses value
        asset.setType(TYPE);

        // int
        feedback.setTweets(TWEETS);

        // List of objects
        Attachment attachment1 = new Attachment();
        attachment1.setName(ATTACHMENT1_NAME);
        asset.addAttachement(attachment1);
        Attachment attachment2 = new Attachment();
        attachment2.setName(ATTACHMENT2_NAME);
        asset.addAttachement(attachment2);
        return asset;
    }

}
