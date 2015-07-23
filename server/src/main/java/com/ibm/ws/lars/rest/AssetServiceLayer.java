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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.AttachmentContentMetadata;
import com.ibm.ws.lars.rest.model.AttachmentContentResponse;
import com.ibm.ws.lars.rest.model.AttachmentList;
import com.ibm.ws.lars.rest.model.RepositoryResourceLifecycleException;

/**
 * This needs to enforce:<br>
 * - state transitions<br>
 * -- no updates allowed to 'published' assets<br>
 * -- time stamp updates<br>
 * -- partial updates (allowed or not??)
 */
@Singleton
public class AssetServiceLayer {

    @Inject
    private Persistor persistenceBean;

    @Inject
    private Configuration configuration;

    /**
     * Current user, can only guarantee this is available for admin actions.
     */
    @Inject
    private Principal principal;

    /**
     * @see Persistor#retrieveAllAssets()
     */
    public AssetList retrieveAllAssets() {
        return persistenceBean.retrieveAllAssets();
    }

    /**
     * @see Persistor#retrieveAllAssets(Map,String)
     */
    public AssetList retrieveAllAssets(Map<String, List<Condition>> filters, String searchTerm) {
        return persistenceBean.retrieveAllAssets(filters, searchTerm);
    }

    /**
     * Summarizes a list of fields from the assets matched by the given filters and search term.
     * <p>
     * For each field, the result is the list of unique values that are stored in that field, across
     * all of the assets matched by the filters and searchTerm.
     * <p>
     * This result is put into a map of the following form:
     *
     * <pre>
     * {
     *   "filterName": fieldName
     *   "filterValue": listOfDistinctValues
     * }
     * </pre>
     * <p>
     * Filters and searchTerm are treated the same as they are in
     * {@link #retrieveAllAssets(Map, String)}.
     *
     * @param fields a list of fields to summarize
     * @param filters a map of filters, which may be empty
     * @param searchTerm a term to search for, which may be null
     * @return a list of result maps, one for each field
     */
    public List<Map<String, Object>> summarizeAssets(List<String> fields, Map<String, List<Condition>> filters, String searchTerm) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (String field : fields) {
            List<Object> values = persistenceBean.getDistinctValues(field, filters, searchTerm);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("filterName", field);
            resultMap.put("filterValue", values);
            result.add(resultMap);
        }

        return result;
    }

    /**
     * @param asset
     * @return
     * @throws InvalidJsonAssetException
     * @throws AssetNotFoundException
     */
    public Asset createAsset(Asset asset) throws InvalidJsonAssetException {
        Asset newAsset = new Asset(asset);

        verifyNewAsset(newAsset);
        String now = IsoDate.format(new Date());
        newAsset.setCreatedOn(now);
        newAsset.setLastUpdatedOn(now);
        newAsset.setCreatedBy(principal.getName());
        newAsset.getProperties().put("state", Asset.State.DRAFT.getValue());

        return persistenceBean.createAsset(newAsset);
    }

    /**
     * @param assetId
     * @return
     * @throws AssetNotFoundException
     */
    public Asset retrieveAsset(String assetId, UriInfo uriInfo) throws NonExistentArtefactException {
        Asset asset = persistenceBean.retrieveAsset(assetId);

        AttachmentList attachments = persistenceBean.findAttachmentsForAsset(assetId);
        for (Attachment attachment : attachments) {
            computeAttachmentURL(attachment, uriInfo);
        }

        asset.setAttachments(attachments);

        return asset;
    }

    /**
     * @param assetId
     * @param assetJSON
     * @return
     * @throws InvalidJsonAssetException
     * @throws AssetNotFoundException
     */
    public Asset updateAsset(String assetId, Asset asset) throws InvalidJsonAssetException, NonExistentArtefactException {
        Asset existingAsset = persistenceBean.retrieveAsset(assetId);
        if (existingAsset == null) {
            throw new NonExistentArtefactException(assetId, "asset");
        }
        return persistenceBean.updateAsset(assetId, asset);
    }

    /**
     * Throws an exception if the state transition is invalid.
     *
     * @param action
     * @param id
     * @return
     * @throws RepositoryResourceLifecycleException
     */
    public void updateAssetState(Asset.StateAction action, String id) throws RepositoryResourceLifecycleException, NonExistentArtefactException {
        Asset existingAsset = persistenceBean.retrieveAsset(id);

        action.performAction(existingAsset);
        existingAsset.setLastUpdatedOn(IsoDate.format(new Date()));

        try {
            persistenceBean.updateAsset(id, existingAsset);
        } catch (InvalidJsonAssetException e) {
            // This should never happen, as the asset was retrieved from the persistence layer,
            // and the only changes were by us. Don't percolate the json exception, as that would
            // make it look like user error.
            throw new RepositoryException("JSON retrieved from asset store could not be save back again", e);
        }

    }

    /**
     * @param assetId
     * @throws AssetNotFoundException
     */
    public void deleteAsset(String assetId) throws NonExistentArtefactException {

        // Delete all attachments belonging to the asset
        Asset asset = persistenceBean.retrieveAsset(assetId);

        for (Attachment attachment : asset.getAttachments()) {
            deleteAttachment(attachment.get_id());
        }

        // Delete the asset itself
        persistenceBean.deleteAsset(assetId);
    }

    public AttachmentList retrieveAttachmentsForAsset(String assetId, UriInfo uriInfo) {
        AttachmentList list = persistenceBean.findAttachmentsForAsset(assetId);
        for (Attachment attachment : list) {
            computeAttachmentURL(attachment, uriInfo);
        }
        return list;
    }

    private Attachment createAttachment(String assetId, String name, Attachment originalAttachmentMetadata, String contentType,
                                        InputStream attachmentContentStream, UriInfo uriInfo) throws InvalidJsonAssetException, AssetPersistenceException, NonExistentArtefactException {

        // Check that the parent exists
        try {
            persistenceBean.retrieveAsset(assetId);
        } catch (NonExistentArtefactException e) {
            // The message from the PersistenceLayer is unhelpful in this context, so send back a better one
            throw new NonExistentArtefactException("The parent asset for this attachment (id="
                                                   + assetId + ") does not exist in the repository.");
        }

        Attachment attachmentMetadata = new Attachment(originalAttachmentMetadata);

        // Add necessary fields to the attachment (JSON) metadata
        if (attachmentMetadata.get_id() == null) {
            attachmentMetadata.set_id(persistenceBean.allocateNewId());
        }
        attachmentMetadata.setAssetId(assetId);
        if (contentType != null) {
            attachmentMetadata.setContentType(contentType);
        }
        attachmentMetadata.setName(name);
        attachmentMetadata.setUploadOn(IsoDate.format(new Date()));

        // Create the attachment content
        if (attachmentContentStream != null) {
            AttachmentContentMetadata contentMetadata = persistenceBean.createAttachmentContent(name, contentType, attachmentContentStream);

            // TODO perhaps we should try to clean up after ourselves and delete the attachmentMetadata
            // TODO seriously, this is one of the places where we reaslise that using a DB that doesn't
            // support transactions means we don't get some of the guarantees that we might be used to.

            attachmentMetadata.setGridFSId(contentMetadata.filename);
            attachmentMetadata.setSize(contentMetadata.length);
        }

        Attachment returnedAttachment = persistenceBean.createAttachmentMetadata(attachmentMetadata);

        computeAttachmentURL(returnedAttachment, uriInfo);

        return returnedAttachment;
    }

    public Attachment createAttachmentWithContent(String assetId, String name, Attachment attachmentMetadata, String contentType,
                                                  InputStream attachmentContentStream, UriInfo uriInfo) throws InvalidJsonAssetException, AssetPersistenceException, NonExistentArtefactException {

        // The attachment has content, so the URL must not be set, and the
        // linkType must not be set (i.e. it must be null).

        String url = attachmentMetadata.getUrl();
        if (url != null) {
            throw new InvalidJsonAssetException("An attachment should not have the URL set if it is created with content");
        }

        String stringType = attachmentMetadata.getLinkType();
        if (stringType != null) {
            throw new InvalidJsonAssetException("The link type must not be set for an attachment with content");
        }

        return createAttachment(assetId, name, attachmentMetadata, contentType, attachmentContentStream, uriInfo);
    }

    /**
     * Create an attachment where the binary is stored elsewhere. This type of asset must supply the
     * URL of the binary in the URL field of the supplied Attachment object. If this is not set,
     * then an InvalidJsonAssetException will be thrown
     *
     * @param assetId
     * @param name
     * @param attachmentMetadata
     * @return
     * @throws InvalidJsonAssetException
     * @throws AssetPersistenceException
     * @throws NonExistentArtefactException
     */
    public Attachment createAttachmentNoContent(String assetId, String name, Attachment attachmentMetadata, UriInfo uriInfo) throws InvalidJsonAssetException,
            AssetPersistenceException, NonExistentArtefactException {

        // There is no content, so an external URL must be set, and the link
        // type must be DIRECT or WEB_PAGE
        String url = attachmentMetadata.getUrl();
        if (url == null) {
            throw new InvalidJsonAssetException("The URL of the supplied attachment was null");
        }

        String stringType = attachmentMetadata.getLinkType();
        if (stringType == null) {
            throw new InvalidJsonAssetException("The link type for the attachment was not set.");
        }

        Attachment.LinkType linkType = Attachment.LinkType.forValue(stringType);
        if (linkType == null || (linkType != Attachment.LinkType.DIRECT && linkType != Attachment.LinkType.WEB_PAGE)) {
            throw new InvalidJsonAssetException("The link type for the attachment was set to an invalid value: " + stringType);
        }

        return createAttachment(assetId, name, attachmentMetadata, null, null, uriInfo);

    }

    public void deleteAttachment(String attachmentId) {
        persistenceBean.deleteAttachmentMetadata(attachmentId);
        persistenceBean.deleteAttachmentContent(attachmentId);
    }

    public Attachment retrieveAttachmentMetadata(String assetId, String attachmentId, UriInfo uriInfo) throws InvalidIdException, NonExistentArtefactException {
        Attachment attachment = persistenceBean.retrieveAttachmentMetadata(attachmentId);
        if (!Objects.equals(attachment.getAssetId(), assetId)) {
            throw new InvalidIdException("Attachment " + attachmentId + " does not have assetId " + assetId);
        }
        computeAttachmentURL(attachment, uriInfo);
        return attachment;
    }

    public AttachmentContentResponse retrieveAttachmentContent(String assetId, String attachmentId, String name, UriInfo uriInfo) throws InvalidIdException,
            NonExistentArtefactException {
        Attachment attachmentMetadata = retrieveAttachmentMetadata(assetId, attachmentId, uriInfo);

        if (!Objects.equals(assetId, attachmentMetadata.getAssetId())) {
            throw new InvalidIdException("Attachment " + attachmentId + " does not have assetId " + assetId);
        }

        if (!Objects.equals(name, attachmentMetadata.getName())) {
            throw new InvalidIdException("Attachment " + attachmentId + " does not have name " + name);
        }

        String gridFSId = attachmentMetadata.getGridFSId();

        return persistenceBean.retrieveAttachmentContent(gridFSId);
    }

    /**
     * There are no required fields for an asset, all that needs to be checked is that there is no
     * _id field. It is not allowed to specify an id in the JSON when an asset is being created.
     * Allowing a user to do so is exposing a mongo implementation.
     *
     * @param assetJSON
     */
    void verifyNewAsset(Asset newAsset) throws InvalidJsonAssetException {
        String id = newAsset.get_id();
        if (id != null) {
            throw new InvalidJsonAssetException("When creating a new asset, the _id field must be blank");
        }
    }

    /**
     * Computes and sets the URL for an attachment if the attachment's content is stored in lars.
     * <p>
     * If the attachment is stored externally, the URL is not changed.
     * <p>
     * The start of the URL is computed from the base URL of the request, unless it's overridden in
     * the server.xml.
     *
     * @param attachment the attachment for which to update and set the URL
     * @param uriInfo the UriInfo from the current request
     */
    private void computeAttachmentURL(Attachment attachment, UriInfo uriInfo) {
        // LinkType != null -> asset is not stored in LARS
        // Therefore there should be an external URL in the attachment
        if (attachment.getLinkType() != null) {
            return;
        }

        // For assets stored in LARS, we need to compute the URL and store it in the attachment
        String encodedName;
        try {
            encodedName = URLEncoder.encode(attachment.getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("This should never happen.", e);
        }

        String url = configuration.getURLBase(uriInfo) + "ma/v1/assets/" + attachment.getAssetId() + "/attachments/" + attachment.get_id() + "/" + encodedName;
        attachment.setUrl(url);
    }

}
