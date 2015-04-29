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

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.Attachment;
import com.mongodb.BasicDBObject;

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
    public void testQuery() {
        final BasicDBObject filter = new BasicDBObject("name", "value");
        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("query: Querying database with query object " + filter);

                logger.fine("query: found " + 0 + " assets.");
            }
        };
        Deencapsulation.invoke(createTestBean(), "query", filter, new BasicDBObject());
    }

    @Test
    public void testCreateAsset() throws InvalidJsonAssetException {
        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("createAsset: inserting object into the database: { }");
            }
        };
        createTestBean().createAsset(new Asset());
    }

    @Test
    public void testUpdateAsset() throws InvalidJsonAssetException, NonExistentArtefactException {
        final String expectedString = "updateAsset: query object: { \"_id\" : { \"$oid\" : \"ffffffffffffffffffffffff\"}}\n"
                                      + "updated asset:{ \"_id\" : { \"$oid\" : \"ffffffffffffffffffffffff\"}}";
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

                logger.fine("createAttachmentMetadata: inserting new attachment { \"_id\" : { \"$oid\" : \"ffffffffffffffffffffffff\"}}");
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

}
