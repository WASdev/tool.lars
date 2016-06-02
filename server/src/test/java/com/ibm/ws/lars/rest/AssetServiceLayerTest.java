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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ibm.ws.lars.rest.exceptions.AssetPersistenceException;
import com.ibm.ws.lars.rest.exceptions.InvalidIdException;
import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.exceptions.NonExistentArtefactException;
import com.ibm.ws.lars.rest.injection.AssetServiceLayerInjection;
import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.RepositoryObject;
import com.ibm.ws.lars.rest.model.RepositoryResourceLifecycleException;
import com.ibm.ws.lars.testutils.BasicChecks;

/**
 *
 */
public class AssetServiceLayerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String TEST_USERNAME = "testUser";
    private Asset simpleObject;
    private Asset assetWithState;
    // private Asset complexObject;
    private Attachment attachmentWithContent;
    private byte[] attachmentContent;
    private UriInfo dummyUriInfo;

    private AssetServiceLayer service;
    Persistor memoryPersistor = new MemoryPersistor();

    @Before
    public void setUp() throws Exception {
        assetWithState = Asset.deserializeAssetFromJson("{\"state\":\"published\",\"name\":\"foo\"}");
        // complexObject = Asset.deserializeAssetFromJson("{\"state\":\"draft\",\"name\":\"foo\", \"arrayField\": " + jsonArray + "}");
        simpleObject = Asset.deserializeAssetFromJson("{\"name\":\"foo\"}");

        attachmentWithContent = Attachment.jsonToAttachment("{\"aField\":\"foo\"}");
        attachmentContent = "I am some very simple content for the attachment!".getBytes();

        service = new AssetServiceLayer();

        AssetServiceLayerInjection.setConfiguration(service, new Configuration());
        AssetServiceLayerInjection.setPersistenceBean(service, memoryPersistor);

        dummyUriInfo = new DummyUriInfo(new URI("http://localhost:9080/ma/v1/"));

    }

    @Test
    public void badStateTransitionTest() throws InvalidJsonAssetException, NonExistentArtefactException, RepositoryResourceLifecycleException {
        thrown.expect(RepositoryResourceLifecycleException.class);
        thrown.expectMessage("Invalid action approve performed on the asset with state draft");
        Asset simpleAsset = service.createAsset(simpleObject, TEST_USERNAME);
        service.updateAssetState(Asset.StateAction.APPROVE, simpleAsset.get_id());
    }

    @Test
    public void lifeCycleTest() throws Exception {
        Asset asset = service.createAsset(assetWithState, TEST_USERNAME);
        assertEquals("State should have been overwritten with draft", "draft", asset.getProperty("state"));
        // TODO: Is it possible to check the actual date is correct?
        assertNotNull("created date should not be null", asset.getCreatedOn());
        String lastUpdated = asset.getLastUpdatedOn();
        assertNotNull("updated date should not be null", lastUpdated);
        assertEquals("CreatedBy user is wrong", TEST_USERNAME, asset.getCreatedBy());

        String id = asset.get_id();
        Asset gotAsset = service.retrieveAsset(id, dummyUriInfo);
        assertEquals("The id of the asset has changed", id, gotAsset.get_id());

        AssetList assets = service.retrieveAllAssets();
        assertEquals("Too many assets", 1, assets.size());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // do nothing. Just to make sure the time stamp is different after the update
        }
        service.updateAssetState(Asset.StateAction.PUBLISH, id);
        Asset updatedAsset = service.retrieveAsset(id, dummyUriInfo);
        assertEquals("Wrong state", Asset.State.AWAITING_APPROVAL, updatedAsset.getState());
        String newUpdatedDate = updatedAsset.getLastUpdatedOn();
        assertFalse("The last updated date should have been updated", lastUpdated.equals(newUpdatedDate));

        Asset simpleAsset = service.createAsset(simpleObject, TEST_USERNAME);
        Asset simpleGotAsset = service.retrieveAsset(simpleAsset.get_id(), dummyUriInfo);
        assertEquals("Wrong state", Asset.State.DRAFT, simpleGotAsset.getState());

        AssetList lotsOfAssets = service.retrieveAllAssets();
        assertEquals("Wrong number of assets found", 2, lotsOfAssets.size());

        service.deleteAsset(id);
        service.deleteAsset(simpleGotAsset.get_id());

        AssetList emptyAssets = service.retrieveAllAssets();
        assertEquals("There should be no assets stored", 0, emptyAssets.size());
    }

    /**
     * Tests creating and retrieving an attachment with no content.
     */
    @Test
    public void testAddAttachmentNoContent() throws Exception {

        String attachmentJSON = "{\"url\":\"http://example.com\", \"linkType\":\"direct\"}";

        Asset asset = Asset.deserializeAssetFromJson("{\"name\":\"Mr Asset\"}");
        Asset returnedAsset = service.createAsset(asset, TEST_USERNAME);
        Attachment unalteredAttachment = Attachment.jsonToAttachment(attachmentJSON);
        Attachment attachment = Attachment.jsonToAttachment(attachmentJSON);

        Attachment returnedAttachment = service.createAttachmentNoContent(returnedAsset.get_id(), "Mr Attachment", attachment, dummyUriInfo);
        assertEquals("Attachment should have assetId set correctly", returnedAsset.get_id(), returnedAttachment.getAssetId());

        returnedAttachment.getProperties().remove(Attachment.ASSET_ID);
        assertNull("Attachment with no uploaded content should not have any data stored in gridFS", returnedAttachment.getGridFSId());

        assertNotNull("Attachment should have an id", returnedAttachment.get_id());
        returnedAttachment.getProperties().remove(RepositoryObject._ID);

        assertNotNull("Attachment should have uploadOn", returnedAttachment.getUploadOn());
        returnedAttachment.getProperties().remove(Attachment.UPLOAD_ON);

        assertEquals("Attachment should have the specified name", "Mr Attachment", returnedAttachment.getName());
        returnedAttachment.getProperties().remove(Attachment.NAME);

        assertEquals(unalteredAttachment, returnedAttachment);
    }

    /**
     * Verifies that an exception is thrown when we attachment to create an attachment with no
     * content and no url.
     */
    @Test
    public void testAddAttachmentNoContentNoUrl() throws InvalidJsonAssetException, AssetPersistenceException, NonExistentArtefactException {
        thrown.expect(InvalidJsonAssetException.class);
        thrown.expectMessage("The URL of the supplied attachment was null");
        String attachmentJSON = "{\"foo\":\"bar\"}";
        Asset asset = Asset.deserializeAssetFromJson("{\"name\":\"Mr Asset\"}");
        Asset returnedAsset = service.createAsset(asset, TEST_USERNAME);
        Attachment attachment = Attachment.jsonToAttachment(attachmentJSON);
        service.createAttachmentNoContent(returnedAsset.get_id(), "Mr Attachment", attachment, dummyUriInfo);
    }

    /**
     * Verifies that an exception is thrown when we attachment to create an attachment with no
     * content and no linkType.
     */
    @Test
    public void testAddAttachmentNoContentNoLinkType() throws InvalidJsonAssetException, AssetPersistenceException, NonExistentArtefactException {
        thrown.expect(InvalidJsonAssetException.class);
        thrown.expectMessage("The link type for the attachment was not set.");
        String attachmentJSON = "{\"url\":\"http://example.com\"}";
        Asset asset = Asset.deserializeAssetFromJson("{\"name\":\"Mr Asset\"}");
        Asset returnedAsset = service.createAsset(asset, TEST_USERNAME);
        Attachment attachment = Attachment.jsonToAttachment(attachmentJSON);
        service.createAttachmentNoContent(returnedAsset.get_id(), "Mr Attachment", attachment, dummyUriInfo);
    }

    /**
     * Verifies that an exception is thrown when we attempt to create an attachment with no content
     * and a linkType that is not valid.
     */
    @Test
    public void testAddAttachmentNoContentBadLinkType() throws InvalidJsonAssetException, AssetPersistenceException, NonExistentArtefactException {
        thrown.expect(InvalidJsonAssetException.class);
        thrown.expectMessage("The link type for the attachment was set to an invalid value: Foobar");
        String attachmentJSON = "{\"url\":\"http://example.com\"}";
        Asset asset = Asset.deserializeAssetFromJson("{\"name\":\"Mr Asset\"}");
        Asset returnedAsset = service.createAsset(asset, TEST_USERNAME);
        Attachment attachment = Attachment.jsonToAttachment(attachmentJSON);
        attachment.setLinkType("Foobar");
        service.createAttachmentNoContent(returnedAsset.get_id(), "Mr Attachment", attachment, dummyUriInfo);
    }

    /**
     * Tests creating and retrieving an attachment with content.
     */
    @Test
    public void testAddAttachmentWithContent() throws Exception {
        Asset assetToCreate = new Asset(simpleObject);
        Asset returnedAsset = service.createAsset(assetToCreate, TEST_USERNAME);

        Attachment attachmentToCreate = new Attachment(attachmentWithContent);

        Attachment createdAttachment = service.createAttachmentWithContent(returnedAsset.get_id(),
                                                                           "AttachmentWithContent.txt",
                                                                           attachmentToCreate,
                                                                           "text/plain",
                                                                           new ByteArrayInputStream(attachmentContent),
                                                                           dummyUriInfo);

        Attachment returnedAttachment = service.retrieveAttachmentMetadata(returnedAsset.get_id(), createdAttachment.get_id(), dummyUriInfo);

        assertEquals("Attachment with content should have correct URL", "http://localhost:9080/ma/v1/assets/" + returnedAsset.get_id() +
                                                                        "/attachments/" + returnedAttachment.get_id() + "/" +
                                                                        "AttachmentWithContent.txt",
                     returnedAttachment.getUrl());
        returnedAttachment.getProperties().remove(Attachment.URL);

        assertEquals("Attachment should have assetId set correctly", returnedAsset.get_id(), returnedAttachment.getAssetId());
        returnedAttachment.getProperties().remove(Attachment.ASSET_ID);

        assertNotNull("Attachment should have an id", returnedAttachment.get_id());
        returnedAttachment.getProperties().remove(RepositoryObject._ID);

        assertEquals("Attachment should have the correct name", "AttachmentWithContent.txt", returnedAttachment.getName());
        returnedAttachment.getProperties().remove(Attachment.NAME);

        assertNotNull("Attachment should have uploadOn", returnedAttachment.getUploadOn());
        returnedAttachment.getProperties().remove(Attachment.UPLOAD_ON);

        assertEquals("Attachment should have contentType set correctly", "text/plain", returnedAttachment.getContentType());
        returnedAttachment.getProperties().remove(Attachment.CONTENT_TYPE);

        assertNotNull("Attachment should have gridFSId", returnedAttachment.getGridFSId());
        returnedAttachment.getProperties().remove(Attachment.GRIDFS_ID);

        assertEquals("Attachment should have correct size", attachmentContent.length, returnedAttachment.getSize());
        returnedAttachment.getProperties().remove(Attachment.SIZE);

        assertEquals("Returned attachment should have same contents that were POSTed", attachmentWithContent, returnedAttachment);

        try (InputStream is = service.retrieveAttachmentContent(returnedAsset.get_id(), createdAttachment.get_id(), "AttachmentWithContent.txt", dummyUriInfo)
                .getContentStream()) {
            byte[] returnedContent = BasicChecks.slurp(is);
            assertTrue(Arrays.equals(attachmentContent, returnedContent));
        }

        Asset fetchedAsset = service.retrieveAsset(returnedAsset.get_id(), dummyUriInfo);
        assertEquals(fetchedAsset.getAttachments().get(0), createdAttachment);
    }

    /**
     * Verifies that an exceptino is thrown when we attempt to create an attachment that has both
     * content and a url.
     */
    @Test
    public void testAddAttachmentWithContentAndUrl() throws Exception {

        thrown.expect(InvalidJsonAssetException.class);
        thrown.expectMessage("An attachment should not have the URL set if it is created with content");

        Asset assetToCreate = new Asset(simpleObject);
        Asset returnedAsset = service.createAsset(assetToCreate, TEST_USERNAME);
        Attachment attachmentToCreate = new Attachment(attachmentWithContent);
        attachmentToCreate.setUrl("foobar");
        service.createAttachmentWithContent(returnedAsset.get_id(), "AttachmentWithContent.txt", attachmentToCreate, "text/plain",
                                            new ByteArrayInputStream(attachmentContent), dummyUriInfo);
    }

    /**
     * Verifies that an exception is thrown when we attempt to create an attachment with both
     * content and a linkType.
     */
    @Test
    public void testAddAttachmentWithContentAndLinkType() throws Exception {

        thrown.expect(InvalidJsonAssetException.class);
        thrown.expectMessage("The link type must not be set for an attachment with content");

        Asset assetToCreate = new Asset(simpleObject);
        Asset returnedAsset = service.createAsset(assetToCreate, TEST_USERNAME);
        Attachment attachmentToCreate = new Attachment(attachmentWithContent);
        attachmentToCreate.setLinkType("foobar");
        service.createAttachmentWithContent(returnedAsset.get_id(), "AttachmentWithContent.txt", attachmentToCreate, "text/plain",
                                            new ByteArrayInputStream(attachmentContent), dummyUriInfo);
    }

    /**
     * Tests that an exception is thrown if an attachment is created with an invalid parent asset
     * id.
     *
     * @throws InvalidJsonAssetException
     * @throws NonExistentArtefactException
     * @throws AssetPersistenceException
     */
    @Test
    public void testAddAttachmentInvalidAssetId() throws InvalidJsonAssetException, NonExistentArtefactException, AssetPersistenceException {
        thrown.expect(NonExistentArtefactException.class);
        thrown.expectMessage("The parent asset for this attachment (id=FFFFFFFFFFFFFFFF) does not exist in the repository.");
        String attachmentJSON = "{\"url\":\"http://example.com\", \"linkType\":\"direct\"}";
        Attachment attachment = Attachment.jsonToAttachment(attachmentJSON);
        service.createAttachmentNoContent("FFFFFFFFFFFFFFFF", "Mr Attachment", attachment, dummyUriInfo);
    }

    /**
     * Verifies that an exception is thrown when we attempt to retrieve an asset that does not
     * exist.
     */
    @Test(expected = NonExistentArtefactException.class)
    public void testRetrieveNonExistentAsset() throws NonExistentArtefactException {
        service.retrieveAsset("0123456789", dummyUriInfo);
    }

    /**
     * Verifies that an exception is thrown when we attempt to retrieve an attachment that does not
     * exist.
     */
    @Test(expected = NonExistentArtefactException.class)
    public void testRetrieveNonExistentAttachment() throws NonExistentArtefactException, InvalidIdException {
        service.retrieveAttachmentMetadata("01234", "67864", dummyUriInfo);
    }

    @Test(expected = NonExistentArtefactException.class)
    public void testRetrieveNonExistentAttachmentContent() throws NonExistentArtefactException, InvalidIdException {
        service.retrieveAttachmentContent("01234", "67864", "a name that does not exist.", dummyUriInfo);
    }
}
