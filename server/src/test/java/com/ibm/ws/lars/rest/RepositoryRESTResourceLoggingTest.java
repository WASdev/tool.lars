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

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import mockit.Expectations;
import mockit.Mocked;

import org.apache.wink.common.model.multipart.BufferedInMultiPart;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.ws.lars.rest.exceptions.AssetPersistenceException;
import com.ibm.ws.lars.rest.exceptions.InvalidIdException;
import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.exceptions.InvalidParameterException;
import com.ibm.ws.lars.rest.exceptions.NonExistentArtefactException;
import com.ibm.ws.lars.rest.model.RepositoryResourceLifecycleException;

public class RepositoryRESTResourceLoggingTest {

    private RepositoryRESTResource getRestResource() {
        RepositoryRESTResource tested = new RepositoryRESTResource();
        // The asset service should be injected by jax-rs, so I think it
        // needs to be set explicitly here, don't think jmockit can do this automagically
        try {
            Field field = tested.getClass().getDeclaredField("assetService");
            field.setAccessible(true);
            field.set(tested, assetService);
            field.setAccessible(false);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException ia) {
            throw new RuntimeException(ia);
        }
        return tested;
    }

    private UriInfo dummyUriInfo;

    @Mocked
    AssetServiceLayer assetService;

    /** ID for an asset which should never exist */
    public static final String NON_EXISTENT_ID = "ffffffffffffffffffffffff";

    @Before
    public void setUp() throws URISyntaxException {
        dummyUriInfo = new DummyUriInfo(new URI("http://localhost:9080/ma/v1/"));
    }

    @Test
    public void testGetAsset(@Mocked final Logger logger, @Mocked final SecurityContext sc) throws InvalidIdException, NonExistentArtefactException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;
                logger.fine("getAsset called with id of 'ffffffffffffffffffffffff'");
                sc.isUserInRole("Administrator");
                result = true;
            }
        };

        getRestResource().getAsset(NON_EXISTENT_ID, dummyUriInfo, sc);
    }

    @Test
    public void testGetAssets(@Mocked final Logger logger, @Mocked final UriInfo info, @Mocked SecurityContext context) throws URISyntaxException, JsonProcessingException, InvalidParameterException {

        new Expectations() {
            {
                info.getQueryParameters(false);

                logger.isLoggable(Level.FINE);
                result = true;

                info.getRequestUri();
                result = new URI("http://localhost:9085/ma/v1/assets?foo=bar");

                logger.fine("getAssets called with query parameters: foo=bar");
            }
        };

        getRestResource().getAssets(info, context);
    }

    @Test
    public void testCountAssets(@Mocked final Logger logger, @Mocked final UriInfo info, @Mocked SecurityContext sc) throws URISyntaxException, InvalidParameterException {

        new Expectations() {
            {
                info.getQueryParameters(false);

                logger.isLoggable(Level.FINE);
                result = true;

                info.getRequestUri();
                result = new URI("http://localhost:9085/ma/v1/assets?foo=bar");

                logger.fine("countAssets called with query parameters: foo=bar");
            }
        };

        getRestResource().countAssets(info, sc);
    }

    @Test
    public void testPostAssets(@Mocked final Logger logger, @Mocked SecurityContext context) {

        final String json = "{\"name\":\"myname\"}";

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("postAssets called with json content:\n" + json);
            }
        };

        getRestResource().postAssets(json, context);
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

        getRestResource().deleteAsset(NON_EXISTENT_ID);
    }

    @Test
    public void testCreateAttachmentWithContent(@Mocked final Logger logger, @Mocked final BufferedInMultiPart inMultiPart) throws InvalidJsonAssetException, InvalidIdException, AssetPersistenceException, NonExistentArtefactException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("createAttachmentWithContent called, name: " + "name" + " assetId: " + NON_EXISTENT_ID);
            }
        };

        getRestResource().createAttachmentWithContent("name", NON_EXISTENT_ID, null, inMultiPart, dummyUriInfo);
    }

    @Test
    public void testCreateAttachmentNoContent(@Mocked final Logger logger) throws InvalidJsonAssetException, InvalidIdException, AssetPersistenceException, NonExistentArtefactException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("createAttachmentNoContent called, name: " + "name" + " assetId: " + NON_EXISTENT_ID
                            + " json content:\n" + "{}");
            }
        };

        getRestResource().createAttachmentNoContent("name", NON_EXISTENT_ID, null, "{}", dummyUriInfo);
    }

    @Test
    public void testGetAttachments(@Mocked final Logger logger, @Mocked final SecurityContext sc) throws Exception {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;
                logger.fine("getAttachments called for assetId: " + NON_EXISTENT_ID);
                sc.isUserInRole("Administrator");
                result = true;
            }
        };

        getRestResource().getAttachments(NON_EXISTENT_ID, dummyUriInfo, sc);
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

        getRestResource().deleteAttachment(NON_EXISTENT_ID, NON_EXISTENT_ID);
    }

    @Test
    public void testGetAttachmentContent(@Mocked final Logger logger, @Mocked final SecurityContext sc) throws InvalidIdException, NonExistentArtefactException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;

                logger.fine("getAttachmentContent called for assetId: " + NON_EXISTENT_ID
                            + " attachmentId: " + NON_EXISTENT_ID + " name: " + "no_name");
                sc.isUserInRole("Administrator");
                result = true;
            }
        };

        getRestResource().getAttachmentContent(NON_EXISTENT_ID, NON_EXISTENT_ID, "no_name", dummyUriInfo, sc);
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

        getRestResource().updateAssetState(NON_EXISTENT_ID, updateJson);
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

        getRestResource().getFakeImConfig();
    }

    @Test
    public void testGetAssetFieldSummary(@Mocked final Logger logger, @Mocked final UriInfo info, @Mocked SecurityContext sc) throws URISyntaxException, JsonProcessingException, InvalidParameterException {

        final MultivaluedMapImpl<String, String> parameters = new MultivaluedMapImpl<>();
        parameters.add("fields", "xyz");

        new Expectations() {
            {
                info.getQueryParameters(false);
                result = parameters;

                logger.isLoggable(Level.FINE);
                result = true;

                info.getRequestUri();
                result = new URI("http://localhost:9085/ma/v1/assets/summary?fields=xyz");

                logger.fine("getAssetFieldSummary called with query parameters: fields=xyz");
            }
        };

        getRestResource().getAssetFieldSummary(info, sc);

    }

    @Test
    public void testGetAssetReviews(@Mocked final Logger logger, @Mocked final SecurityContext sc) throws InvalidIdException, NonExistentArtefactException {

        new Expectations() {
            {
                logger.isLoggable(Level.FINE);
                result = true;
                logger.fine("getAssetReviews called with id of 'ffffffffffffffffffffffff'");
                sc.isUserInRole("Administrator");
                result = true;
            }
        };

        getRestResource().getAssetReviews(NON_EXISTENT_ID, dummyUriInfo, sc);
    }
}
