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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Singleton;

import org.bson.types.ObjectId;

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.AttachmentContentMetadata;
import com.ibm.ws.lars.rest.model.AttachmentContentResponse;
import com.ibm.ws.lars.rest.model.AttachmentList;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

/**
 * Bean through which supports CRUD operations. All accesses to the database should go through this
 * class.
 *
 * Current limitations:<br>
 * - No logging<br>
 * - Little or no error handling<br>
 * - Currently only supports JSON Strings.<br>
 *
 * Perhaps we want to be communicating using a facade over the top of DBObjects to make the
 * interface a bit nicer.
 *
 */
@Singleton
public class PersistenceBean implements Persistor {

    private static final Logger logger = Logger.getLogger(PersistenceBean.class.getCanonicalName());

    private static final String ASSETS_COLLECTION = "assets";

    private static final String ATTACHMENTS_COLLECTION = "attachments";

    /** The _id field of a MongoDB object */
    private static String ID = "_id";

    private static final String DB_NAME = "mongo/larsDB";

    @Resource(lookup = DB_NAME)
    private com.mongodb.DB db;

    private GridFS gridFS;

    @PostConstruct
    public void createGridFS() {
        gridFS = new GridFS(db);
    }

    private DBCollection getAssetCollection() {
        return db.getCollection(ASSETS_COLLECTION);
    }

    private DBCollection getAttachmentCollection() {
        return db.getCollection(ATTACHMENTS_COLLECTION);
    }

    private DBObject makeQueryById(ObjectId id) {
        return new BasicDBObject(ID, id);
    }

    private static void convertObjectIdToHexString(DBObject obj) {
        Object objectIdObject = obj.get(ID);
        if ((objectIdObject != null) && (objectIdObject instanceof ObjectId)) {
            ObjectId objId = (ObjectId) objectIdObject;
            String hex = objId.toStringMongod();
            obj.put(ID, hex);
        }
    }

    private static void convertHexIdToObjectId(DBObject obj) {
        Object idObject = obj.get(ID);
        if ((idObject != null) && (idObject instanceof String)) {
            String hex = (String) idObject;
            obj.put(ID, new ObjectId(hex));
        }
    }

    @Override
    public AssetList retrieveAllAssets() {
        List<Map<String, Object>> mapList = new ArrayList<>();

        try (DBCursor cursor = getAssetCollection().find()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("retrieveAllAssets: found " + cursor.count() + " assets.");
            }
            for (DBObject obj : cursor) {
                convertObjectIdToHexString(obj);
                // BSON spec says that all keys have to be strings
                // so this should be safe.
                mapList.add(obj.toMap());
            }
        }

        return AssetList.createAssetListFromMaps(mapList);
    }

    /** {@inheritDoc} */
    @Override
    public AssetList retrieveAllAssets(Map<String, List<Condition>> filters) {

        if (filters.size() == 0) {
            return retrieveAllAssets();
        }

        // Need to use a filterList and an $and operator because we may add multiple $or sections
        // which would overwrite each other if we just appended them to the filterObject
        BasicDBList filterList = new BasicDBList();
        BasicDBObject filterObject = new BasicDBObject("$and", filterList);

        for (Entry<String, List<Condition>> filter : filters.entrySet()) {
            List<Condition> conditions = filter.getValue();
            if (conditions.size() == 1) {
                filterList.add(createFilterObject(filter.getKey(), conditions.get(0)));
            } else {
                BasicDBList list = new BasicDBList();
                for (Condition condition : conditions) {
                    list.add(createFilterObject(filter.getKey(), condition));
                }
                filterList.add(new BasicDBObject("$or", list));
            }
        }

        return query(filterObject);
    }

    private BasicDBObject createFilterObject(String field, Condition condition) {
        Object value = null;
        switch (condition.getOperation()) {
            case EQUALS:
                value = condition.getValue();
                break;
            case NOT_EQUALS:
                value = new BasicDBObject("$ne", condition.getValue());
                break;
        }

        return new BasicDBObject(field, value);
    }

    private AssetList query(DBObject filterObject) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("query: Querying database with query object " + filterObject);
        }

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (DBCursor cursor = getAssetCollection().find(filterObject)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("query: found " + cursor.count() + " assets.");
            }
            for (DBObject obj : cursor) {
                convertObjectIdToHexString(obj);
                // BSON spec says that all keys have to be strings
                // so this should be safe.
                results.add(obj.toMap());
            }
        }

        return AssetList.createAssetListFromMaps(results);

    }

    @Override
    public Asset retrieveAsset(String assetId) throws NonExistentArtefactException {
        return retrieveAsset(new ObjectId(assetId));
    }

    /**
     * Retrieve a single asset by its id.
     *
     * @return A json string, or null if the asset doesn't exist
     * @throws AssetNotFoundException
     */
    private Asset retrieveAsset(ObjectId assetId) throws NonExistentArtefactException {
        BasicDBObject query = new BasicDBObject(ID, assetId);
        DBObject resultObj = getAssetCollection().findOne(query);
        if (resultObj == null) {
            throw new NonExistentArtefactException(assetId.toString(), "asset");
        }
        convertObjectIdToHexString(resultObj);
        // All entries in a Mongo document have string keys, this is part of
        // the BSON spec, so this should be safe. Not very nice though.
        return Asset.createAssetFromMap(resultObj.toMap());
    }

    @Override
    public Asset createAsset(Asset newAsset) throws InvalidJsonAssetException {

        DBObject obj = new BasicDBObject(newAsset.getProperties());
        convertHexIdToObjectId(obj);

        DBCollection coll = getAssetCollection();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("createAsset: inserting object into the database: " + obj);
        }

        coll.insert(obj);

        Asset createdAsset = null;
        try {
            createdAsset = retrieveAsset((ObjectId) obj.get(ID));
        } catch (NonExistentArtefactException e) {
            // This should not happen. If it does it is a repository bug
            throw new RepositoryException("Created asset could not be retrieved from the database.", e);
        }

        return createdAsset;
    }

    @Override
    public Asset updateAsset(String assetId, Asset asset) throws InvalidJsonAssetException, NonExistentArtefactException {
        if (!Objects.equals(assetId, asset.get_id())) {
            throw new InvalidJsonAssetException("The specified asset id does not match the specified asset.");
        }

        DBCollection coll = getAssetCollection();

        ObjectId objId = new ObjectId(assetId);
        DBObject query = makeQueryById(objId);

        DBObject obj = new BasicDBObject(asset.getProperties());
        convertHexIdToObjectId(obj);

        if (logger.isLoggable(Level.FINE)) {
            String msg = "updateAsset: query object: " + query + "\nupdated asset:" + obj;
            logger.fine(msg);
        }

        coll.update(query, obj);

        return retrieveAsset(objId);
    }

    /**
     * Delete the asset with the specified id.
     */
    @Override
    public void deleteAsset(String assetId) {
        DBCollection coll = getAssetCollection();
        DBObject query = new BasicDBObject(ID, new ObjectId(assetId));
        coll.remove(query);
    }

    /**
     * @param attachmentContentStream
     * @return
     * @throws AssetPersistenceException
     */
    @Override
    public AttachmentContentMetadata createAttachmentContent(String name, String contentType, InputStream attachmentContentStream) {
        // Do not specify a bucket (so the data will be stored in fs.files and fs.chunks)
        GridFSInputFile gfsFile = gridFS.createFile(attachmentContentStream);
        ObjectId id = new ObjectId();
        gfsFile.setContentType(contentType);
        gfsFile.setId(id);
        String filename = id.toString();
        gfsFile.setFilename(filename);
        gfsFile.save();

        return new AttachmentContentMetadata(gfsFile.getFilename(), gfsFile.getLength());
    }

    /**
     * @param attachment
     * @return
     * @throws AssetNotFoundException
     */
    @Override
    public Attachment createAttachmentMetadata(Attachment attachment) {
        BasicDBObject state = new BasicDBObject(attachment.getProperties());
        convertHexIdToObjectId(state);

        DBCollection coll = getAttachmentCollection();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("createAttachmentMetadata: inserting new attachment " + state);
        }
        coll.insert(state);
        Object idObject = state.get(ID);
        String id;
        if (idObject instanceof String) {
            id = (String) idObject;
        } else if (idObject instanceof ObjectId) {
            id = ((ObjectId) idObject).toStringMongod();
        } else {
            throw new AssertionError("_id should be either String of ObjectId");
        }
        try {
            return retrieveAttachmentMetadata(id);
        } catch (NonExistentArtefactException e) {
            throw new RepositoryException("Created attachment could not be retrieved from the persistence store", e);
        }
    }

    @Override
    public Attachment retrieveAttachmentMetadata(String attachmentId) throws NonExistentArtefactException {
        BasicDBObject query = new BasicDBObject(ID, new ObjectId(attachmentId));
        DBObject resultObj = getAttachmentCollection().findOne(query);
        if (resultObj == null) {
            throw new NonExistentArtefactException(attachmentId, "attachment");
        }
        convertObjectIdToHexString(resultObj);

        // All entries in a Mongo document have string keys, this is part of
        // the BSON spec, so this should be safe. Not very nice though.
        @SuppressWarnings("unchecked")
        Map<String, Object> map = resultObj.toMap();
        return Attachment.createAttachmentFromMap(map);
    }

    @Override
    public void deleteAttachmentContent(String attachmentId) {
        gridFS.remove(attachmentId);
    }

    @Override
    public void deleteAttachmentMetadata(String attachmentId) {
        DBObject query = new BasicDBObject(ID, new ObjectId(attachmentId));
        getAttachmentCollection().remove(query);
    }

    @Override
    public AttachmentList findAttachmentsForAsset(String assetId) {
        BasicDBObject query = new BasicDBObject("assetId", assetId);
        ArrayList<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (DBCursor cursor = getAttachmentCollection().find(query)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("findAttachmentsForAsset: found " + cursor.count() + " attachments for asset " + assetId);
            }

            for (DBObject attachment : cursor) {
                convertObjectIdToHexString(attachment);
                @SuppressWarnings("unchecked")
                Map<String, Object> oneResult = attachment.toMap();
                results.add(oneResult);
            }
        }

        return AttachmentList.createAttachmentListFromMaps(results);
    }

    /**
     * Returns an InputStream of the content of the attachment or null if the attachment does not
     * exist.
     *
     * @throws NonExistentArtefactException
     */
    @Override
    public AttachmentContentResponse retrieveAttachmentContent(String gridFSId) throws NonExistentArtefactException {
        GridFSDBFile file = gridFS.findOne(gridFSId);

        if (file != null) {
            InputStream contentStream = file.getInputStream();
            String contentType = file.getContentType();
            return new AttachmentContentResponse(contentStream, contentType);
        } else {
            throw new NonExistentArtefactException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String allocateNewId() {
        return new ObjectId().toStringMongod();
    }
}
