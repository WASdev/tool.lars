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

import java.util.logging.Level;
import java.util.logging.Logger;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import com.ibm.ws.lars.testutils.ReflectionTricks;

import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.exceptions.NonExistentArtefactException;
import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.mongo.PersistenceBean;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;

public class PersistenceBeanLoggingTest {
    @Mocked
    Logger logger;

    @Mocked
    com.mongodb.DB db;

    /** ID for an asset which should never exist */
    public static final String NON_EXISTENT_ID = "ffffffffffffffffffffffff";

    /**
     * There isn't an easy way to magically inject the database into the persistence bean, as it is
     * normally injected by the container, so this is a utility method to inject it
     */
    private PersistenceBean createTestBean() {
        PersistenceBean bean = new PersistenceBean();
        Deencapsulation.setField(bean, "db", db);
        return bean;
    }

    @Test
    public void testRetrieveAllAssets() {
        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("retrieveAllAssets: found " + 0 + " assets.");
            }
        };
        createTestBean().retrieveAllAssets();
    }

    @Test
    public void testQuery() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        final BasicDBObject filter = new BasicDBObject("name", "value");
        final BasicDBObject sort = new BasicDBObject("foo", "bar");
        final BasicDBObject projection = new BasicDBObject("wibble", "foop");
        final PaginationOptions pagination = new PaginationOptions(0, 2);
        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("query: Querying database with query object " + filter);
                logger.fine("query: sort object " + sort);
                logger.fine("query: projection object " + projection);
                logger.fine("query: pagination object " + pagination);

                logger.fine("query: found " + 0 + " assets.");
            }
        };
        ReflectionTricks.reflectiveCallWithDefaultClass(createTestBean(), "query", DBObject.class, filter, sort, projection, pagination);
    }

    @Test
    public void testCreateAsset() throws InvalidJsonAssetException {
        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("createAsset: inserting object into the database: {}");
            }
        };
        createTestBean().createAsset(new Asset());
    }

    @Test
    public void testUpdateAsset() throws InvalidJsonAssetException, NonExistentArtefactException {
        final String expectedString = "updateAsset: query object: {\"_id\": {\"$oid\": \"ffffffffffffffffffffffff\"}}\n"
                                      + "updated asset:{\"_id\": {\"$oid\": \"ffffffffffffffffffffffff\"}}";
        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine(expectedString);
            }
        };
        Asset asset = new Asset();
        asset.set_id(NON_EXISTENT_ID);
        createTestBean().updateAsset(NON_EXISTENT_ID, asset);
    }

    @Test
    public void testCreateAttachmentMetadata() {
        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("createAttachmentMetadata: inserting new attachment {\"_id\": {\"$oid\": \"ffffffffffffffffffffffff\"}}");
            }
        };
        Attachment a2 = new Attachment();
        a2.set_id(NON_EXISTENT_ID);
        createTestBean().createAttachmentMetadata(a2);
    }

    @Test
    public void testFindAttachmentsForAsset() {
        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("findAttachmentsForAsset: found " + 0 + " attachments for asset " + NON_EXISTENT_ID);
            }
        };
        createTestBean().findAttachmentsForAsset(NON_EXISTENT_ID);
    }

    @Test
    public void testQueryCount(@Mocked final DBCursor cursor) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        final BasicDBObject filterObject = new BasicDBObject("key1", "value1");
        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("queryCount: Querying database with query object " + filterObject);

                cursor.count();
                result = 3;

                logger.fine("queryCount: found 3 assets.");
            }
        };
        ReflectionTricks.reflectiveCallWithDefaultClass(createTestBean(), "queryCount", DBObject.class, filterObject);
    }
}
