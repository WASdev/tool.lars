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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;

import org.apache.wink.common.model.multipart.BufferedInMultiPart;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.ws.lars.rest.model.RepositoryResourceLifecycleException;

public class RepositoryRESTResourceLoggingTest {

    @Mocked
    AssetServiceLayer assetService;

    /** ID for an asset which should never exist */
    public static final String NON_EXISTENT_ID = "ffffffffffffffffffffffff";

    @Test
    public void testGetAsset(@Mocked final Logger logger) throws InvalidIdException, NonExistentArtefactException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;
                logger.fine("getAsset called with id of 'ffffffffffffffffffffffff'");
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        Deencapsulation.setField(tested, "assetService", new AssetServiceLayer());
        tested.getAsset(NON_EXISTENT_ID);
    }

    @Test
    public void testGetAssets(@Mocked final Logger logger, @Mocked final UriInfo info, @Mocked final MultivaluedMap mvMap) throws URISyntaxException, JsonProcessingException {

        new Expectations() {
            {
                info.getQueryParameters();
                result = mvMap;

                logger.isLoggable(Level.FINE);
                result = true;

                info.getRequestUri();
                result = new URI("http://localhost:9085/ma/v1/assets?foo=bar");

                logger.fine("getAssets called with query parameters: foo=bar");
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.getAssets(info);
    }

    @Test
    public void testPostAssets(@Mocked final Logger logger) {

        final String json = "{\"name\":\"myname\"}";

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("postAssets called with json content:\n" + json);
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.postAssets(json);
    }

    @Test
    public void testDeleteAssets(@Mocked final Logger logger) throws InvalidIdException, NonExistentArtefactException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("deleteAsset called with id of " + NON_EXISTENT_ID);
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.deleteAsset(NON_EXISTENT_ID);
    }

    @Test
    public void testCreateAttachmentWithContent(@Mocked final Logger logger, @Mocked final BufferedInMultiPart inMultiPart) throws InvalidJsonAssetException, InvalidIdException, AssetPersistenceException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("createAttachmentWithContent called, name: " + "name" + " assetId: " + NON_EXISTENT_ID);
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.createAttachmentWithContent("name", NON_EXISTENT_ID, null, inMultiPart);
    }

    @Test
    public void testCreateAttachmentNoContent(@Mocked final Logger logger) throws InvalidJsonAssetException, InvalidIdException, AssetPersistenceException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("createAttachmentNoContent called, name: " + "name" + " assetId: " + NON_EXISTENT_ID
                            + " json content:\n" + "{}");
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.createAttachmentNoContent("name", NON_EXISTENT_ID, null, "{}");
    }

    @Test
    public void testGetAttachments(@Mocked final Logger logger) throws InvalidJsonAssetException, InvalidIdException, IOException, ServletException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("getAttachments called for assetId: " + NON_EXISTENT_ID);
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.getAttachments(NON_EXISTENT_ID);
    }

    @Test
    public void testDeleteAttachment(@Mocked final Logger logger) throws InvalidIdException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("deleteAttachment called for assetId: " + NON_EXISTENT_ID + " and attachmentId: " + NON_EXISTENT_ID);
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.deleteAttachment(NON_EXISTENT_ID, NON_EXISTENT_ID);
    }

    @Test
    public void testGetAttachmentContent(@Mocked final Logger logger) throws InvalidIdException, NonExistentArtefactException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("getAttachmentContent called for assetId: " + NON_EXISTENT_ID
                            + " attachmentId: " + NON_EXISTENT_ID + " name: " + "no_name");
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.getAttachmentContent(NON_EXISTENT_ID, NON_EXISTENT_ID, "no_name");
    }

    @Test
    public void testUpdateAssetState(@Mocked final Logger logger) throws NonExistentArtefactException, RepositoryResourceLifecycleException {

        final String updateJson = "{\"action\":\"publish\"}";

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("updateAssetState called for assetId: " + NON_EXISTENT_ID + " action: " + updateJson);
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.updateAssetState(NON_EXISTENT_ID, updateJson);
    }

    @Test
    public void testGetFakeImConfig(@Mocked final Logger logger) {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("getFakeImConfig called");
            }
        };

        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        Deencapsulation.setField(tested, "assetService", assetService);
        tested.getFakeImConfig();
    }

}
