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

import static com.ibm.ws.lars.testutils.matchers.SummaryResultMatcher.summaryResult;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.lars.rest.RepositoryContext.Protocol;
import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.AttachmentList;
import com.ibm.ws.lars.testutils.FatUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

/**
 * Tests for the LARS REST API, which is designed to be compatible with the legacy Massive server.
 * Most tests should run against LARS or Massive, although some may not work exactly the same
 * against both, as LARS intends to do the 'right' thing in error cases, where this doesn't break
 * existing clients
 *
 * Tests required: GET,POST,PUT,DELETE ON /assets GET,POST,PUT,DELETE ON /assets/{asset_id}
 *
 * With variations for good/error cases
 */
@RunWith(Parameterized.class)
public class ApiTest {

    /** ID for an asset which should never exist */
    public static final String NON_EXISTENT_ID = "ffffffffffffffffffffffff";

    private static final long RANDOM_SEED = 0xACEDEADBEEFL;
    private Random random;

    @Rule
    public final RepositoryContext repository;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { Protocol.HTTP },
                                             { Protocol.HTTPS } });
    }

    public ApiTest(Protocol protocol) {
        this.repository = RepositoryContext.createAsAdmin(true, protocol);
    }

    @Before
    public void setUp() throws FileNotFoundException, IOException, InvalidJsonAssetException {

        random = new Random(RANDOM_SEED);
    }

    @After
    public void tearDown() throws IOException, InvalidJsonAssetException {

    }

    @Test
    public void testGetAllAssets() throws ClientProtocolException, IOException, InvalidJsonAssetException {

        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);
        Attachment attachmentNoContent = AssetUtils.getTestAttachmentNoContent();
        String attachmentName = "nocontent.txt";
        Attachment createdAttachment = repository.doPostAttachmentNoContent(returnedAsset.get_id(),
                                                                            attachmentName,
                                                                            attachmentNoContent);

        AssetList assets = repository.doGetAllAssets();

        assertEquals("Wrong number of assets", 1, assets.size());
        Asset retrievedAsset = assets.get(0);
        Object attachments = retrievedAsset.get("attachments");
        assertTrue("The attachments field should be null", attachments == null);

        Asset assetAgain = repository.getAsset(returnedAsset.get_id());
        Object attachmentsAgain = assetAgain.get("attachments");
        List<?> attachmentsList = (List<?>) attachmentsAgain;
        assertEquals("Wrong number of attachments", 1, attachmentsList.size());
        Map<?, ?> attachmentMap = (Map<?, ?>) attachmentsList.get(0);
        assertEquals("", createdAttachment.get_id(), attachmentMap.get("_id"));
    }

    @Test
    public void testGetAssetInvalid() throws Exception {
        String message = repository.getBadAsset("foo_bar_asset_id", 400);
        // massive gives a 500, LARS gives a 404
        assertEquals("Unexpected error message returned from server", "Invalid asset id", repository.parseErrorObject(message));
    }

    /**
     * Test an asset id that doesn't exist
     *
     * @throws Exception
     */
    @Test
    public void testGetAssetNonExistent() throws Exception {
        String message = repository.getBadAsset(NON_EXISTENT_ID, 404);

        // Massive gives a 400, LARS gives a 404 which is more helpful
        assertEquals("Incorrect error message from server",
                     "asset not found for id: ffffffffffffffffffffffff",
                     repository.parseErrorObject(message));
        return;
    }

    /**
     * Attempt to delete an asset with an invalid asset id
     *
     */
    @Test
    public void testDeleteAssetInvalidId() throws Exception {
        // Massive returns a 500 with an error of:
        // "Cannot read property 'createdBy' of null"
        // which really does look like a 500.
        String message = repository.deleteAsset("foobar", 400);
        assertEquals("Unexpected message from server", "Invalid asset id", repository.parseErrorObject(message));
        return;
    }

    /**
     * Attempt to delete an asset with a non-existent id
     *
     */
    @Test
    public void testDeleteAssetNonExistentId() throws Exception {
        repository.deleteAsset(NON_EXISTENT_ID, 404);
    }

    /**
     * Tests calling delete on /assets directly. This should be a 'not implemented error'
     *
     * @throws Exception
     */
    @Test
    public void testDeleteAssets() throws Exception {
        repository.deleteAsset(null, 405);
    }

    /**
     * Attempt to post but with an ID specified.
     *
     * Massive gives a 404 with an error on this. LARS doesn't implement this, so jax-rs gives a
     * 405, method not allowed. Hmm, what is right?
     *
     */
    @Test
    public void testPostAssetWithId() throws Exception {
        repository.doPost("/assets/" + NON_EXISTENT_ID, AssetUtils.getTestAsset().toJson(), 405);
    }

    /**
     * Massive gives a 500 error on this. 400 bad request seems better
     *
     * @throws Exception
     */
    @Test
    public void testPostAssetInvalidJson() throws Exception {
        String message = repository.parseErrorObject(repository.doPost("/assets", "foobar", 400));

        assertEquals("Unexpected error message",
                     "Invalid asset definition",
                     message);
    }

    /**
     * Update/PUT is not implemented this should fail
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        repository.updateAssetNoAttachments(AssetUtils.getTestAsset(), 405);
    }

    @Test
    public void testPutStateBadState() throws Exception {
        Asset newAsset = repository.addAssetNoAttachments(AssetUtils.getTestAsset());
        String message = repository.updateAssetState(newAsset.get_id(), "no state", 400);
        String expectedError = "Either the supplied JSON was badly formed, or it did not contain a valid 'action' field: {\"action\":\"no state\"}";
        assertEquals("Unexpected error message from server", expectedError, repository.parseErrorObject(message));
        repository.deleteAsset(newAsset.get_id(), -1);
    }

    @Test
    public void testPutStateInvalidAction() throws Exception {
        Asset newAsset = repository.addAssetNoAttachments(AssetUtils.getTestAsset());
        String message = repository.updateAssetState(newAsset.get_id(), Asset.StateAction.UNPUBLISH.getValue(), 400);
        String expectedError = "Invalid action " + Asset.StateAction.UNPUBLISH.getValue() +
                               " performed on the asset with state " + newAsset.getState().getValue();
        assertEquals("Unexpected error message from server", expectedError, repository.parseErrorObject(message));
        repository.deleteAsset(newAsset.get_id(), -1);
    }

    @Test
    public void testPutStateNonExistendId() throws Exception {
        String message = repository.updateAssetState(NON_EXISTENT_ID, Asset.StateAction.UNPUBLISH.getValue(), 404);
        assertEquals("Unexpected error message from server",
                     "asset not found for id: ffffffffffffffffffffffff",
                     repository.parseErrorObject(message));
    }

    @Test
    public void testGeneratedFields() throws Exception {
        Asset testAsset = AssetUtils.getTestAsset();
        testAsset = repository.addAssetNoAttachments(testAsset);

        Date now = new Date();
        Date createdOn = IsoDate.parse(testAsset.getCreatedOn());
        assertTrue("Created date is not close to current date", Math.abs(now.getTime() - createdOn.getTime()) < 2000);

        Date updatedOn = IsoDate.parse(testAsset.getLastUpdatedOn());
        assertTrue("Updated date is not close to current date", Math.abs(now.getTime() - updatedOn.getTime()) < 2000);

        String createdBy = testAsset.getCreatedBy();
        assertEquals(repository.getUser(), createdBy);
    }

    /**
     * Tests a create -> get -> -> change state -> delete, with valid data
     *
     *
     * @throws Exception
     */
    @Test
    public void testGoldenPath() throws Exception {
        Asset testAsset = AssetUtils.getTestAsset();
        testAsset = repository.addAssetNoAttachments(testAsset);
        String id = testAsset.get_id();
        Asset gotAsset = repository.getAsset(id);
        AssetUtils.assertAssetsEqual("Asset has been modified", testAsset, gotAsset);
        assertEquals("Asset was not created in draft state", Asset.State.DRAFT, gotAsset.getState());

        repository.updateAssetState(id, Asset.StateAction.PUBLISH.getValue(), 200);
        Asset publishedAsset = repository.getAsset(id);
        assertEquals("The asset is not in the expected state", Asset.State.AWAITING_APPROVAL, publishedAsset.getState());

        repository.deleteAsset(id, -1);
        assertTrue(repository.repositoryIsEmpty());
    }

    /**
     * Tests taking an attachment through create -> retrieve -> delete lifecycle.
     */
    @Test
    public void testCRUDAttachmentWithContent() throws Exception {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);

        Attachment attachmentWithContent = AssetUtils.getTestAttachmentWithContent();

        AttachmentList attachments = repository.doGetAllAttachmentsForAsset(returnedAsset.get_id());
        assertTrue("Asset should have zero attachments before creating any attachments", attachments.isEmpty());

        String attachmentName = "attachment.txt";
        byte[] content = "This is the content.\nIt is quite short.".getBytes("UTF-8");

        Attachment createdAttachment = repository.doPostAttachmentWithContent(returnedAsset.get_id(),
                                                                              attachmentName,
                                                                              attachmentWithContent,
                                                                              content,
                                                                              ContentType.APPLICATION_OCTET_STREAM);

        Attachment returnedAttachment = repository.doGetOnlyAttachment(returnedAsset.get_id(), createdAttachment.get_id());

        AssetUtils.assertAttachmentMetadataEquivalent(repository,
                                                      "Expected returned attachment to match the one that was uploaded",
                                                      returnedAttachment,
                                                      returnedAsset.get_id(),
                                                      createdAttachment.get_id(),
                                                      attachmentName,
                                                      "application/octet-stream",
                                                      content.length,
                                                      attachmentWithContent);

        byte[] retrievedContent = repository.doGetAttachmentContent(returnedAsset.get_id(),
                                                                    createdAttachment.get_id(),
                                                                    attachmentName);
        assertTrue("retrieved attachment content should match created content", Arrays.equals(content, retrievedContent));

        repository.doDeleteAttachment(returnedAsset.get_id(), createdAttachment.get_id());

        AttachmentList attachmentsAfterDeletion = repository.doGetAllAttachmentsForAsset(returnedAsset.get_id());
        assertTrue("Asset should have zero attachments after deletion of only attachment", attachmentsAfterDeletion.isEmpty());
    }

    /**
     * Tries to upload an attachment that has both content and a url and verifies that the returns
     * HTTP 400 Bad Request.
     */
    @Test
    public void testErrorAttachmentWithContentAndURL() throws Exception {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);

        Attachment attachmentWithContent = AssetUtils.getTestAttachmentWithContent();
        attachmentWithContent.setUrl("any_url");

        String attachmentName = "attachment.txt";
        byte[] content = "This is the content.\nIt is quite short.".getBytes(StandardCharsets.UTF_8);

        repository.doPostBadAttachmentWithContent(returnedAsset.get_id(),
                                                  attachmentName,
                                                  attachmentWithContent,
                                                  content,
                                                  ContentType.APPLICATION_OCTET_STREAM,
                                                  400,
                                                  "An attachment should not have the URL set if it is created with content");
    }

    /**
     * Tries to upload an attachment that has both content and a linkType. Verifies that the server
     * does not allow this.
     */
    @Test
    public void testAttachmentWithContentAndLinktype() throws Exception {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);

        Attachment attachmentWithContent = AssetUtils.getTestAttachmentWithContent();
        attachmentWithContent.setLinkType("any_type");

        String attachmentName = "attachment.txt";
        byte[] content = "This is the content.\nIt is quite short.".getBytes("UTF-8");

        repository.doPostBadAttachmentWithContent(returnedAsset.get_id(),
                                                  attachmentName,
                                                  attachmentWithContent,
                                                  content,
                                                  ContentType.APPLICATION_OCTET_STREAM,
                                                  400,
                                                  "The link type must not be set for an attachment with content");
    }

    /**
     * Takes an attachment with no content through create -> retrieve -> delete.
     */
    @Test
    public void testCRUDAttachmentNoContent() throws ClientProtocolException, IOException, InvalidJsonAssetException {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);

        Attachment attachmentNoContent = AssetUtils.getTestAttachmentNoContent();

        AttachmentList attachments = repository.doGetAllAttachmentsForAsset(returnedAsset.get_id());
        assertTrue("Asset should have zero attachments before creating any attachments", attachments.isEmpty());

        String attachmentName = "nocontent.txt";

        Attachment createdAttachment = repository.doPostAttachmentNoContent(returnedAsset.get_id(),
                                                                            attachmentName,
                                                                            attachmentNoContent);

        Attachment returnedAttachment = repository.doGetOnlyAttachment(returnedAsset.get_id(), createdAttachment.get_id());

        AssetUtils.assertAttachmentNoContentMetadataEquivalent(returnedAttachment,
                                                               returnedAsset.get_id(),
                                                               createdAttachment.get_id(),
                                                               attachmentName,
                                                               attachmentNoContent);

        repository.doDeleteAttachment(returnedAsset.get_id(), createdAttachment.get_id());

        AttachmentList attachmentsAfterDeletion = repository.doGetAllAttachmentsForAsset(returnedAsset.get_id());
        assertTrue("Asset should have zero attachments after deletion of only attachment", attachmentsAfterDeletion.isEmpty());
    }

    /**
     * Tries to upload an attachment with no content and no URL. Verifies that the server does not
     * allow this.
     */
    @Test
    public void testAttachmentNoContentNoUrl() throws ClientProtocolException, IOException, InvalidJsonAssetException {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);

        Attachment attachmentNoContent = AssetUtils.getTestAttachmentNoContent();
        attachmentNoContent.setUrl(null);
        String attachmentName = "nocontent.txt";
        repository.doPostBadAttachmentNoContent(returnedAsset.get_id(),
                                                attachmentName,
                                                attachmentNoContent,
                                                400,
                                                "The URL of the supplied attachment was null");

    }

    /**
     * Tries to upload an attachment with no content and no link type and verifies that the server
     * does not allow this.
     */
    @Test
    public void testErrorAttachmentNoContentNoLinkType() throws ClientProtocolException, IOException, InvalidJsonAssetException {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);

        Attachment attachmentNoContent = AssetUtils.getTestAttachmentNoContent();
        attachmentNoContent.setLinkType(null);
        String attachmentName = "nocontent.txt";
        repository.doPostBadAttachmentNoContent(returnedAsset.get_id(),
                                                attachmentName,
                                                attachmentNoContent,
                                                400,
                                                "The link type for the attachment was not set.");
    }

    /**
     * @return byte array containing pseudo-random attachment content.
     */
    private byte[] createSomeAttachmentContent(int x) {
        int length = random.nextInt(50000);
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * Creates an asset with multiple attachments and then verify that these attachments can be
     * retrieved correctly and with the same content that we uploaded.
     */
    @Test
    public void testAddLotsOfAttachments() throws ClientProtocolException, IOException, InvalidJsonAssetException {
        int numAttachments = 100;

        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);

        ArrayList<Attachment> attachments = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        ArrayList<byte[]> contents = new ArrayList<>();
        ArrayList<String> attachmentIds = new ArrayList<>();

        long totalTime = 0;

        for (int i = 0; i < numAttachments; i++) {
            Attachment attachment = new Attachment();
            attachment.getProperties().put("randomProperty", "value" + i);
            attachments.add(attachment);
            names.add("attachment" + i);
            byte[] content = createSomeAttachmentContent(i);
            contents.add(content);

            long startTime = System.currentTimeMillis();
            Attachment returnedAttachment = repository.doPostAttachmentWithContent(returnedAsset.get_id(),
                                                                                   "attachment" + i,
                                                                                   attachment,
                                                                                   content,
                                                                                   ContentType.APPLICATION_OCTET_STREAM);
            long endTime = System.currentTimeMillis();
            totalTime += (endTime - startTime);

            attachmentIds.add(returnedAttachment.get_id());
        }

        System.out.println("Total time to upload " + numAttachments + " assets: " + totalTime);

        AttachmentList returnedAttachments = repository.doGetAllAttachmentsForAsset(returnedAsset.get_id());
        assertEquals("Returned attachment list should have correct size", numAttachments, returnedAttachments.size());

        Map<String, Attachment> attachmentsById = new HashMap<>();
        for (Attachment a : returnedAttachments) {
            attachmentsById.put(a.get_id(), a);
        }

        for (int i = 0; i < numAttachments; i++) {
            Attachment returnedAttachment = attachmentsById.get(attachmentIds.get(i));
            AssetUtils.assertAttachmentMetadataEquivalent(repository,
                                                          "attachments not equal at index " + i,
                                                          returnedAttachment,
                                                          returnedAsset.get_id(),
                                                          attachmentIds.get(i),
                                                          names.get(i),
                                                          ContentType.APPLICATION_OCTET_STREAM.getMimeType(),
                                                          contents.get(i).length,
                                                          attachments.get(i));

            byte[] returnedContent = repository.doGetAttachmentContent(returnedAsset.get_id(),
                                                                       returnedAttachment.get_id(),
                                                                       returnedAttachment.getName());
            assertArrayEquals("attachment content should match what was uploaded; index " + i, contents.get(i), returnedContent);
        }
    }

    /**
     * Attempts to retrieve an attachment that does not exist and verifies that we get HTTP 404 back
     * from the server.
     */
    @Test
    public void testRetrieveNonExistentAttachment() throws IOException, InvalidJsonAssetException {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);

        // This method basically asserts that we get HTTP 404 Not Found back from the server.
        repository.doGetAttachmentContentInError(returnedAsset.get_id(),
                                                 NON_EXISTENT_ID,
                                                 "non-existent",
                                                 404,
                                                 "attachment not found for id: " + NON_EXISTENT_ID);
    }

    /**
     * Attempts to retrieve an attachment that does exist from a URL that contains a non-existent
     * asset id and verifies that the server does not allow this.
     *
     */
    @Test
    public void testErrorRetrieveAttachmentOnNonExistentAsset() throws InvalidJsonAssetException, IOException {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);
        Attachment attachmentNoContent = AssetUtils.getTestAttachmentNoContent();
        String attachmentName = "nocontent.txt";
        Attachment createdAttachment = repository.doPostAttachmentNoContent(returnedAsset.get_id(),
                                                                            attachmentName,
                                                                            attachmentNoContent);

        repository.doGetAttachmentContentInError(NON_EXISTENT_ID,
                                                 createdAttachment.get_id(),
                                                 attachmentName,
                                                 400,
                                                 "Attachment " + createdAttachment.get_id() + " does not have assetId " + NON_EXISTENT_ID);
    }

    /**
     * Deletes a non-existent attachment and verify that we get HTTP 204 No Content back from the
     * server. This is because the current behaviour of LARS is to return 204 whether the attachment
     * existed or not. The post-condition of the call is the same: the attachment is not there.
     */
    @Test
    public void testDeleteNonExistentAttachment() throws IOException, InvalidJsonAssetException {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = repository.addAssetNoAttachments(testAsset);

        // This method basically asserts that we get HTTP 204 No Content back from the server.
        // Which the server is meant to give us, whether there was an attachment or not.
        repository.doDeleteAttachment(returnedAsset.get_id(), NON_EXISTENT_ID);
    }

    @Test
    public void testGetAllAssetsFiltered() throws IOException, InvalidJsonAssetException {

        Asset testAsset = addLittleAsset("foo", "bar");
        Asset testAsset2 = addLittleAsset("foo", "bar");
        Asset testAsset3 = addLittleAsset("foo", "barry");
        addLittleAsset("foo", "baz");
        addLittleAsset("fooser", "bar");
        addLittleAsset("limit", "nolimit");
        addLittleAsset("offset", "bigoffset");

        Asset testAsset8 = addLittleAsset(new String[] { "foo", "baz", "bill", "ben", "jack", "jill" });
        addLittleAsset(new String[] { "foo", "baz", "bill", "ben", "john", "janet" });

        AssetList assets = repository.getAllAssets("foo=bar");
        assertEquals("Unexpected number of assets retrieved", 2, assets.size());
        String id_1 = testAsset.get_id();
        String id_2 = testAsset2.get_id();
        for (Asset retrievedAsset : assets) {
            String retrieved_id = retrievedAsset.get_id();
            if (!retrieved_id.equals(id_1) && !retrieved_id.equals(id_2)) {
                fail("Expected to retrieve id " + id_1 + " or " + id_2 + " but got " + retrieved_id);
            }
        }

        AssetList assets2 = repository.getAllAssets("ben=bill");
        assertEquals("Shouldn't have retrieved any assets", 0, assets2.size());

        // This is to test that various parameters are ignored as filters by the server.
        // This is because Massive uses them for other purposes, but unhelpfully,
        // they are not clearly distinguished from filter parameters.
        // LARS doesn't currently support the operations defined by these parameters,
        // with the exception of 'q', which is used for searching
        AssetList assets3 = repository.getAllAssets("limit=nolimit");

        assertEquals("limit should not be treated as a filter, so all assets should be retrieved", 9, assets3.size());

        AssetList assets4 = repository.getAllAssets("offset=bigoffset");
        assertEquals("offset should not be treated as a filter, so all assets should be retrieved", 9, assets4.size());

        AssetList assets8 = repository.getAllAssets("fields=fields_to_search_on");
        assertEquals("fields should not be treated as a filter, so all assets should be retrieved", 9, assets8.size());

        AssetList assets5 = repository.getAllAssets("foo=baz&jack=jill");
        assertEquals("Unexpected number of assets retrieved", 1, assets5.size());
        assertEquals("The wrong asset was retrieved.", testAsset8.get_id(), assets5.get(0).get_id());

        AssetList assets6 = repository.getAllAssets("foo=randomvalue&foo=bar&foo=barry");
        assertEquals("Unexpected number of assets retrieved", 1, assets6.size());
        assertEquals("The wrong asset was retrieved.", testAsset3.get_id(), assets6.get(0).get_id());

    }

    @Test
    public void testGetAllAssetsWithSearch() throws IOException, InvalidJsonAssetException {

        addLittleAsset("foo", "bar");
        addLittleAsset("foo", "bar");
        addLittleAsset("foo", "barry");
        addLittleAsset("foo", "baz");
        addLittleAsset("fooser", "bar");
        addLittleAsset("limit", "nolimit");
        addLittleAsset("offset", "bigoffset");

        Asset testAsset8 = addLittleAsset(new String[] { "foo", "baz", "bill", "ben", "jack", "jill", "name", "searchable" });
        addLittleAsset(new String[] { "foo", "baz", "bill", "ben", "john", "janet" });

        AssetList assets7 = repository.getAllAssets("q=searchable");
        assertEquals("Wrong number of assets retrieved", 1, assets7.size());
        assertEquals("The wrong asset was retrieved.", testAsset8.get_id(), assets7.get(0).get_id());

        AssetList assets1 = repository.getAllAssets("q=searchable&foo=baz");
        assertEquals("Wrong number of assets retrieved", 1, assets1.size());
        assertEquals("The wrong asset was retrieved.", testAsset8.get_id(), assets1.get(0).get_id());

        // An empty search is treated is 'get everything' by massive.
        AssetList assets2 = repository.getAllAssets("q=");
        assertEquals("Wrong number of assets retrieved", 9, assets2.size());

        // Testing phrases
        Asset testAsset9 = addLittleAsset(new String[] { "bill", "ben", "jack", "jill", "name", "a long name which can be searched" });
        Asset testAsset10 = addLittleAsset(new String[]
        { "bill", "ben", "jack", "jill", "name", "long", "description", "name", "tags", "which can" });

        // This should get just 9, as it is searching for a phrase
        String searchQuery = URLEncoder.encode("\"long name which\"", StandardCharsets.UTF_8.name());
        AssetList assets9 = repository.getAllAssets("q=" + searchQuery);
        assertEquals("Wrong number of assets retrieved", 1, assets9.size());
        assertEquals("The wrong asset was retrieved.", testAsset9.get_id(), assets9.get(0).get_id());

        // This should get both 9 and 10 as it should be searching on separate words
        String searchQuery2 = URLEncoder.encode("name long can", StandardCharsets.UTF_8.name());
        AssetList assets10 = repository.getAllAssets("q=" + searchQuery2);
        assertEquals("Wrong number of assets retrieved", 2, assets10.size());
        String id_one = assets10.get(0).get_id();
        String id_two = assets10.get(1).get_id();
        assertTrue("The wrong asset was retrieved", id_one.equals(testAsset9.get_id()) || id_one.equals(testAsset10.get_id()));
        assertTrue("The wrong asset was retrieved", id_two.equals(testAsset9.get_id()) || id_two.equals(testAsset10.get_id()));

    }

    @Test
    public void testGetAllAssetsNotFiltered() throws InvalidJsonAssetException, IOException {

        addLittleAsset("foo", "bar");
        addLittleAsset("foo", "bar");
        addLittleAsset("foo", "barry");
        addLittleAsset("foo", "baz");
        addLittleAsset("fooser", "bar");
        addLittleAsset("limit", "nolimit");
        addLittleAsset("offset", "bigoffset");

        addLittleAsset(new String[] { "foo", "baz", "bill", "ben", "jack", "jill" });
        addLittleAsset(new String[] { "foo", "baz", "bill", "ben", "john", "janet" });

        AssetList assets = repository.getAllAssets("foo=!bar");
        assertEquals("Unexpected number of assets retrieved", 7, assets.size());

        AssetList assets2 = repository.getAllAssets("foo=!wobble");
        assertEquals("Unexpected number of assets retrieved", 9, assets2.size());

        AssetList assets3 = repository.getAllAssets("foo=!bar&bill=ben");
        assertEquals("Unexpected number of assets retrieved", 2, assets3.size());

    }

    @Test
    public void testGetAllAssetsOrFiltered() throws Exception {
        addLittleAsset("weather", "hot", "ground", "flat");
        addLittleAsset("weather", "hot", "ground", "hilly");
        addLittleAsset("weather", "hot", "ground", "mountainous");
        addLittleAsset("weather", "cold", "ground", "flat");
        addLittleAsset("weather", "cold", "ground", "hilly");
        addLittleAsset("weather", "cold", "ground", "mountainous");
        addLittleAsset("weather", "warm", "ground", "flat");
        addLittleAsset("weather", "warm", "ground", "hilly");
        addLittleAsset("weather", "warm", "ground", "mountainous");

        AssetList result1 = repository.getAllAssets("weather=hot|cold");
        assertEquals("Unexpected number of assets retrieved", 6, result1.size());

        AssetList result2 = repository.getAllAssets("weather=hot|cold&ground=!mountainous");
        assertEquals("Unexpected number of assets retrieved", 4, result2.size());

        AssetList result3 = repository.getAllAssets("weather=hot|warm&ground=hilly|mountainous");
        assertEquals("Unexpected number of assets retrieved", 4, result3.size());

        // Edge cases
        // ----------

        // !hot|warm == (!hot) OR (warm) == !hot -> 6 results
        AssetList result4 = repository.getAllAssets("weather=!hot|warm");
        assertEquals("Unexpected number of assets retrieved", 6, result4.size());

        // !value is ignored if it is not the first value in the list
        // warm|!hot == warm -> 3 results
        AssetList result5 = repository.getAllAssets("weather=warm|!hot");
        assertEquals("Unexpected number of assets retrieved", 3, result5.size());

        // !hot|hot == (!hot) OR (hot) == everything -> 9 results
        AssetList result6 = repository.getAllAssets("weather=!hot|hot");
        assertEquals("Unexpected number of assets retrieved", 9, result6.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAssetSummary() throws Exception {
        addLittleAsset("weather", "hot", "ground", "flat", "description", "lovely");
        addLittleAsset("weather", "hot", "ground", "hilly", "description", "ok");
        addLittleAsset("weather", "hot", "ground", "mountainous", "description", "exhausting");
        addLittleAsset("weather", "cold", "ground", "flat", "description", "bleak");
        addLittleAsset("weather", "cold", "ground", "hilly", "description", "ok");
        addLittleAsset("weather", "cold", "ground", "mountainous");
        addLittleAsset("weather", "warm", "ground", "flat");
        addLittleAsset("weather", "warm", "ground", "hilly", "description", "dull");
        addLittleAsset("weather", "warm", "ground", "mountainous");

        List<Map<String, Object>> result;
        result = repository.getAssetSummary("fields=weather");
        assertThat(result, contains(summaryResult("weather", "hot", "cold", "warm")));

        result = repository.getAssetSummary("fields=weather,ground");
        assertThat(result, containsInAnyOrder(summaryResult("weather", "hot", "cold", "warm"),
                                              summaryResult("ground", "flat", "hilly", "mountainous")));

        result = repository.getAssetSummary("fields=description&ground=flat");
        assertThat(result, contains(summaryResult("description", "lovely", "bleak")));

        result = repository.getAssetSummary("fields=description&ground=mountainous");
        assertThat(result, contains(summaryResult("description", "exhausting")));

        result = repository.getAssetSummary("fields=weather&q=ok");
        assertThat(result, contains(summaryResult("weather", "hot", "cold")));

        result = repository.getAssetSummary("fields=foo");
        assertThat(result, contains(summaryResult("foo")));

        repository.getBadAssetSummary("fields=", 400);

        repository.getBadAssetSummary("", 400);
    }

    // Add and asset with an extra properties
    private Asset addLittleAsset(String... values) throws IOException, InvalidJsonAssetException {
        Asset littleAsset = AssetUtils.getTestAsset();
        for (int i = 0; i < values.length; i = i + 2) {
            littleAsset.setProperty(values[i], values[i + 1]);
        }
        return repository.addAssetNoAttachments(littleAsset);
    }

    /**
     * This test is for the handling of internal server errors. It provokes the error by directly
     * inserting bad data into the mongo database, and then sending a rest query to attempt (and
     * fail) to retrieve it.
     *
     * @throws IOException
     * @throws ParseException
     * @throws InvalidJsonAssetException
     */
    @Test
    public void test500Mapping() throws InvalidJsonAssetException, ParseException, IOException {

        // First, make a direct connection to the database, and insert a dodgy asset

        String ID = "_id";
        MongoClient mongoClient = new MongoClient("localhost:" + FatUtils.DB_PORT);
        mongoClient.setWriteConcern(WriteConcern.JOURNAL_SAFE);
        DB db = mongoClient.getDB(FatUtils.TEST_DB_NAME);
        DBCollection assetCollection = db.getCollection("assets");
        DBObject obj = new BasicDBObject();
        obj.put("foo", "bar");
        assetCollection.insert(obj);

        // Check that the 'asset' can be retrieved from the database.
        Object id = obj.get(ID);
        BasicDBObject query = new BasicDBObject(ID, id);
        DBObject resultObj = assetCollection.findOne(query);
        if (resultObj == null) {
            fail("The inserted 'asset' couldn't be found in the database using " + id);
        }

        // Check that the 'asset' can be retrieved via the REST interface
        AssetList assets = repository.doGetAllAssets();
        assertEquals("Unexpected number of assets retrieved", 1, assets.size());
        Asset retrievedAsset = assets.get(0);

        // Attempt to update the state of the 'asset'. This should fail and cause
        // a 500 error
        String message = repository.updateAssetState(retrievedAsset.get_id(), Asset.StateAction.UNPUBLISH.getValue(), 500);
        assertEquals("Message was wrong", "Internal server error, please contact the server administrator", repository.parseErrorObject(message));

        // Remove dodgy data from the database for the next test. Don't drop anything
        // as it will remove the database indexing.
        assetCollection.remove(new BasicDBObject());
        mongoClient.close();
    }
}
