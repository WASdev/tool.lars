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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.AttachmentContentMetadata;
import com.ibm.ws.lars.rest.model.AttachmentContentResponse;
import com.ibm.ws.lars.rest.model.AttachmentList;

/**
 *
 */
public interface Persistor {

    public AssetList retrieveAllAssets();

    /**
     * Retrieve a list of assets, filtered based on the supplied set of filters. The keys in the the
     * filter map should be a field specification as per the mongo query syntax. So a key of
     * 'foo.bar' would refer to a document containing a field named 'foo' where 'foo' is an object
     * containing a field name 'bar'
     *
     * This method will return each asset where <b>all</b> of the the fields indicated by a key is a
     * match for <b>any</b> of the conditions in the corresponding list.
     *
     * If the filter map is empty, all entries in the store will be returned.
     */
    public AssetList retrieveAllAssets(Map<String, List<Condition>> filters);

    /**
     * Retrieve a single asset by its id.
     *
     * @throws AssetNotFoundException
     */
    public Asset retrieveAsset(String assetId) throws NonExistentArtefactException;

    /**
     * Create a single asset.
     *
     * The id need not be set on the asset that is passed in.
     *
     * @throws AssetNotFoundException
     */
    public Asset createAsset(Asset newAsset) throws InvalidJsonAssetException;

    /**
     * Update an existing asset.
     *
     * @return the updated asset, or if the id didn't previously exist, returns null
     * @throws AssetNotFoundException
     */
    public Asset updateAsset(String assetId, Asset asset) throws InvalidJsonAssetException, NonExistentArtefactException;

    /**
     * Delete the asset with the specified id.
     */
    public void deleteAsset(String assetId);

    /**
     * Returns the list of all the attachments on the object with the specified assetId.
     */
    public AttachmentList findAttachmentsForAsset(String assetId);

    /**
     * Creates attachment content (ie file contents) in the persistence store.
     *
     * @throws AssetPersistenceException
     */
    public AttachmentContentMetadata createAttachmentContent(String name, String contentType, InputStream attachmentContentStream) throws AssetPersistenceException;

    /**
     * Creates the (JSON) metadata for an attachment in the persistence store.
     *
     * @throws AssetNotFoundException
     */
    public Attachment createAttachmentMetadata(Attachment attachment);

    /**
     * Retrieves metadata for the specified attachment.
     *
     * @throws AssetNotFoundException
     */
    public Attachment retrieveAttachmentMetadata(String attachmentId) throws NonExistentArtefactException;

    /**
     * Deletes the content of the specified attachment. Caller should also delete the attachment
     * metadata.
     */
    public void deleteAttachmentContent(String attachmentId);

    /**
     * Deletes the metadata for the specified attachment. Callers should have already deleted
     * attachment content (if it exists).
     */
    public void deleteAttachmentMetadata(String attachmentId);

    /**
     * Returns an input stream of the content of the specified attachment.
     *
     * @throws NonExistentArtefactException
     */
    public AttachmentContentResponse retrieveAttachmentContent(String gridFSId) throws NonExistentArtefactException;

    /**
     * Allocates and returns a new unique id. This is useful if the id of an object has to be set
     * before creating it in the persistence store.
     */
    public String allocateNewId();
}
