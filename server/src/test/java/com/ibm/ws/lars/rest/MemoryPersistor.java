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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.AttachmentContentMetadata;
import com.ibm.ws.lars.rest.model.AttachmentContentResponse;
import com.ibm.ws.lars.rest.model.AttachmentList;

/**
 *
 */
public class MemoryPersistor implements Persistor {

    static private long lastId = 0;

    static synchronized String getNextId() {
        lastId++;
        return String.format("%024x", lastId);
    }

    private static final String ASSET_ID = "assetId";

    private final Map<String, Map<String, Object>> assets = new HashMap<>();

    private final Map<String, Map<String, Object>> attachments = new HashMap<>();

    private final Map<String, AttachmentContent> gridFS = new HashMap<>();

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#retrieveAllAssets()
     */
    @Override
    public AssetList retrieveAllAssets() {
        // Note: retrieveAllAssets does *not* set attachments
        return AssetList.createAssetListFromMaps(new ArrayList<Map<String, Object>>(assets.values()));
    }

    @Override
    public AssetList retrieveAllAssets(Map<String, List<Condition>> filters, String searchTerm) {
        throw new UnsupportedOperationException("Filtering is not supported in this test facade");
    }

    @Override
    public List<Object> getDistinctValues(String field, Map<String, List<Condition>> filters, String searchTerm) {
        throw new UnsupportedOperationException("Filtering is not supported in this test facade");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#retrieveAsset(java.lang.String)
     */
    @Override
    public Asset retrieveAsset(String assetId) throws NonExistentArtefactException {
        if (!assets.containsKey(assetId)) {
            throw new NonExistentArtefactException();
        }
        return Asset.createAssetFromMap(new HashMap<>(assets.get(assetId)));
    }

    @Override
    public Asset createAsset(Asset newAsset) throws InvalidJsonAssetException {
        Map<String, Object> props = newAsset.getProperties();
        String id = getNextId();
        props.put("_id", id);
        assets.put(id, props);
        return Asset.createAssetFromMap(props);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#deleteAsset(java.lang.String)
     */
    @Override
    public void deleteAsset(String assetId) {
        assets.remove(assetId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#updateAsset(java.lang.String,
     * com.ibm.ws.lars.rest.model.Asset)
     */
    @Override
    public Asset updateAsset(String assetId, Asset asset) throws InvalidJsonAssetException, NonExistentArtefactException {
        assets.put(assetId, asset.getProperties());
        return asset;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#findAttachmentsForAsset(java.lang.String)
     */
    @Override
    public AttachmentList findAttachmentsForAsset(String assetId) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Entry<String, Map<String, Object>> e : attachments.entrySet()) {
            Map<String, Object> attachmentState = e.getValue();
            if (Objects.equals(attachmentState.get(ASSET_ID), assetId)) {
                resultList.add(attachmentState);
            }
        }

        return AttachmentList.createAttachmentListFromMaps(resultList);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#createAttachmentContent(java.lang.String,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public AttachmentContentMetadata createAttachmentContent(String name, String contentType, InputStream attachmentContentStream)
            throws AssetPersistenceException {
        try {
            String id = getNextId();

            // Oh Java, I hate you for making me do this. Maybe if we start using
            // Apache Commons then wecan rip this out
            byte[] buffer = new byte[1024];
            int length;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((length = attachmentContentStream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            byte[] contentBytes = baos.toByteArray();

            AttachmentContent attachmentContent = new AttachmentContent(name, contentType, id, contentBytes);

            gridFS.put(id, attachmentContent);

            return new AttachmentContentMetadata(id, contentBytes.length);
        } catch (IOException e) {
            throw new AssetPersistenceException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibm.ws.lars.rest.Persistor#createAttachmentMetadata(com.ibm.ws.lars.rest.model.Attachment
     * )
     */
    @Override
    public Attachment createAttachmentMetadata(Attachment attachment) {
        Map<String, Object> props = new HashMap<>(attachment.getProperties());
        String id = attachment.get_id();

        if (id == null) {
            id = getNextId();
            attachment.set_id(id);
        }
        attachments.put(id, props);
        return Attachment.createAttachmentFromMap(props);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#retrieveAttachmentMetadata(java.lang.String)
     */
    @Override
    public Attachment retrieveAttachmentMetadata(String attachmentId) throws NonExistentArtefactException {
        if (!attachments.containsKey(attachmentId)) {
            throw new NonExistentArtefactException();
        }
        return Attachment.createAttachmentFromMap(new HashMap<>(attachments.get(attachmentId)));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#deleteAttachmentContent(java.lang.String)
     */
    @Override
    public void deleteAttachmentContent(String attachmentId) {
        throw new RuntimeException("not implemented");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#deleteAttachmentMetadata(java.lang.String)
     */
    @Override
    public void deleteAttachmentMetadata(String attachmentId) {
        throw new RuntimeException("not implemented");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#retrieveAttachmentContent(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public AttachmentContentResponse retrieveAttachmentContent(String gridFSId) {
        AttachmentContent content = gridFS.get(gridFSId);
        InputStream contentStream = new ByteArrayInputStream(content.content);
        String contentType = content.contentType;
        return new AttachmentContentResponse(contentStream, contentType);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.lars.rest.Persistor#allocateNewId()
     */
    @Override
    public String allocateNewId() {
        return getNextId();
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        // Nothing to be done
    }

}

class AttachmentContent {
    String name;
    String contentType;
    String id;
    byte[] content;

    public AttachmentContent(String name, String contentType, String id, byte[] content) {
        this.name = name;
        this.contentType = contentType;
        this.id = id;
        // Note we do not defensively copy the content so you must not alter
        // the contents of the array
        this.content = content;
    }
}
