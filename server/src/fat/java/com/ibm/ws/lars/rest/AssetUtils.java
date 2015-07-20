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
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.Attachment;

/**
 *
 */
public class AssetUtils {
    private AssetUtils() {
        throw new AssertionError("Please do not instantiate this class.");
    }

    /**
     * Creates a minimum valid asset ie an asset with only compulsory fields and no attachments.
     *
     * @return the asset.
     */
    protected static Asset getTestAsset() {
        Asset testAsset = new Asset();
        testAsset.setProperty("name", "Test Asset created at " + createDateString());
        return testAsset;
    }

    /**
     * Creates a minimum valid attachment that will be uploaded to LARS. It is intended that content
     * will be supplied when we upload this attachment and therefore the linkType is not set.
     *
     * @return the attachment.
     */
    protected static Attachment getTestAttachmentWithContent() {
        Attachment testAttachment = new Attachment();
        testAttachment.setType("Test asset");
        // don't set the URL because we will be uploading content
        // for this attachment
        return testAttachment;
    }

    /**
     * Creates a minimum valid attachment metadata that will be uploaded to LARS. It is intended
     * that content will *not* be supplied when we upload this attachment and therefore the linkType
     * is set to "direct".
     *
     * @return the attachment.
     */
    protected static Attachment getTestAttachmentNoContent() {
        Attachment testAttachment = new Attachment();
        testAttachment.setType("Test attachment");
        testAttachment.setUrl("http://example.com/test-asset");
        testAttachment.setLinkType(Attachment.LinkType.DIRECT.getValue());
        return testAttachment;
    }

    /**
     * Creates a string that represents the current time.
     *
     * @return a string containing the date.
     */
    protected static String createDateString() {
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        return " created at " + dateFormater.format(Calendar.getInstance().getTime());
    }

    /**
     * Creates a clone of an asset in canonical form that can be compared with another asset for
     * equality.
     *
     * @return the clone of the asset.
     */
    private static Asset normaliseAsset(Asset asset) {
        Asset clone = new Asset(asset); // create a clone

        // An Asset with an empty attachments list is equivalent to an asset with
        // no attachments list at all so remove attachments if empty.
        if (clone.getAttachments().isEmpty()) {
            clone.getProperties().remove(Asset.ATTACHMENTS);
        }

        return clone;
    }

    /**
     * Asserts that two assets are logically equal to each other after being normalised ( @see
     * #normaliseAsset() ).
     */
    protected static void assertAssetsEqual(String message, Asset lhs, Asset rhs) {
        assertEquals(message, normaliseAsset(lhs), normaliseAsset(rhs));
    }

    /**
     * Compares an uploaded asset to the original assets.
     *
     * @param message the failure message.
     * @param expected the expected asset.
     * @param actual the actual asset.
     */
    protected static void assertUploadedAssetEquivalentToOriginal(String message, Asset expected, Asset actual) {
        Asset clone = new Asset(actual);

        clone.getProperties().remove(Asset._ID);
        clone.getProperties().remove(Asset.CREATED_ON);
        clone.getProperties().remove(Asset.CREATED_BY);
        clone.getProperties().remove(Asset.STATE);
        clone.getProperties().remove(Asset.LAST_UPDATED_ON);

        assertAssetsEqual(message, expected, clone);
    }

    /**
     * Asserts that two Attachment objects, actual and expect, are equivalent. In addition, the
     * fields id, assetId, name and contentType on the actual Attachment are compared with the
     * supplied parameters. This should be used similarly to JUnit's assertEquals() methods.
     */
    protected static void assertAttachmentMetadataEquivalent(RepositoryContext repository,
                                                             String message,
                                                             Attachment actual,
                                                             String assetId,
                                                             String id,
                                                             String name,
                                                             String contentType,
                                                             long size,
                                                             Attachment expected) {
        Attachment clone = new Attachment(actual);
        assertEquals("attachment assetId", assetId, actual.getAssetId());
        clone.getProperties().remove(Attachment.ASSET_ID);
        assertEquals("attachment id", id, actual.get_id());
        clone.getProperties().remove(Attachment._ID);
        assertEquals("attachment name", name, actual.getName());
        clone.getProperties().remove(Attachment.NAME);
        assertEquals("attachment contentType", contentType, actual.getContentType());
        clone.getProperties().remove(Attachment.CONTENT_TYPE);
        assertEquals("attachment URL", repository.getFullURL() + "/assets/" + assetId + "/attachments/" + id + "/" + name, actual.getUrl());
        clone.getProperties().remove(Attachment.URL);
        assertNotNull("attachment should have a gridFS id", actual.getGridFSId());
        clone.getProperties().remove(Attachment.GRIDFS_ID);
        assertEquals("attachment size should match size of uploaded content", size, actual.getSize());
        clone.getProperties().remove(Attachment.SIZE);
        clone.getProperties().remove(Attachment.UPLOAD_ON);

        assertEquals(message, expected, clone);
    }

    /**
     * Asserts that an Attachment metadata object that has been downloaded from the server is
     * equivalent to the original object that was uploaded. Certain fields will be set by the server
     * when the Attachment metadata object is uploaded and we supply the expected values of these
     * fields as parameters to the method.
     *
     * @param downloadedAttachment the attachment that we wish to check, which is assumed to have
     *            been downloaded from the server and which will therefore have fields set by the
     *            server.
     * @param assetId the expected assetId
     * @param id the expected id
     * @param name the expected name
     * @param reference the attachment that we wish to compare attachment to. It is assumed that
     *            reference has *not* been uploaded to the server and therefore will not have fields
     *            set by the server.
     */
    protected static void assertAttachmentNoContentMetadataEquivalent(Attachment downloadedAttachment,
                                                                      String assetId,
                                                                      String id,
                                                                      String name,
                                                                      Attachment reference) {
        Attachment clone = new Attachment(downloadedAttachment);
        assertEquals("attachment assetId", assetId, downloadedAttachment.getAssetId());
        clone.getProperties().remove(Attachment.ASSET_ID);
        assertEquals("attachment id", id, downloadedAttachment.get_id());
        clone.getProperties().remove(Attachment._ID);
        assertEquals("attachment name", name, downloadedAttachment.getName());
        clone.getProperties().remove(Attachment.NAME);
        clone.getProperties().remove(Attachment.SIZE);
        clone.getProperties().remove(Attachment.UPLOAD_ON);

        assertEquals(reference, clone);
    }
}
