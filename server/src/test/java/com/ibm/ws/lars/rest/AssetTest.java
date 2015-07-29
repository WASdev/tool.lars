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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.exceptions.RepositoryException;
import com.ibm.ws.lars.rest.injection.AssetServiceLayerInjection;
import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.RepositoryResourceLifecycleException;

/**
 *
 */
public class AssetTest {

    private static final String TEST_USERNAME = "testUser";
    private static final String jsonArray = "[\"one\", \"two\", \"three\"]";
    private static final String simpleObject = "{\"name\":\"foo\"}";

    private static final String complexObject = "{\"name\":\"foo\", \"arrayField\": " + jsonArray + "}";

    AssetServiceLayer service;
    Persistor memoryPersistor = new MemoryPersistor();

    Map<String, Object> simpleAssetMap = new HashMap<>();
    {
        simpleAssetMap.put("name", "foo");
    }

    @Before
    public void setup() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        service = new AssetServiceLayer();

        Principal testPrincipal = new Principal() {
            @Override
            public String getName() {
                return TEST_USERNAME;
            }
        };

        AssetServiceLayerInjection.setConfiguration(service, new Configuration());
        AssetServiceLayerInjection.setPersistenceBean(service, memoryPersistor);
        AssetServiceLayerInjection.setPrincipal(service, testPrincipal);
    }

    /**
     * deserializeAssetFromJson should return an asset that has not been modified in any way, so it
     * should not have any generated fields set (e.g. created/updated times), and should not have a
     * state.
     *
     * @throws Exception
     */
    @Test
    public void testDeserializeAssetFromJson() throws Exception {

        Asset asset = Asset.deserializeAssetFromJson(complexObject);
        assertNull(asset.getCreatedOn());
        assertNull(asset.getLastUpdatedOn());
        assertNull(asset.getCreatedBy());
        assertNull(asset.get_id());
        assertEquals("No attachments should have been created", 0, asset.getAttachments().size());
        assertEquals("Name property has been modified", "foo", asset.getProperty("name"));
        assertEquals("Extra properties have been added", 2, asset.getProperties().size());
        assertTrue("Expected arrayField to be a list", (asset.getProperty("arrayField") instanceof List));

        Asset asset2 = Asset.createAssetFromMap(simpleAssetMap);
        assertNull(asset2.getCreatedOn());
        assertNull(asset2.getLastUpdatedOn());
        assertNull(asset2.getCreatedBy());
        assertNull(asset2.get_id());
        assertEquals("No attachments should have been created", 0, asset2.getAttachments().size());
        assertEquals("Name property has been modified", "foo", asset2.getProperty("name"));
        assertEquals("Extra properties have been added", 1, asset2.getProperties().size());

    }

    @Test(expected = RepositoryException.class)
    public void testDeserializeAssetFromJson2() throws Exception {
        Asset asset = Asset.deserializeAssetFromJson(simpleObject);
        assertNull(asset.getState());
    }

    @Test(expected = RepositoryException.class)
    public void testDeserializeAssetFromJson3() throws Exception {
        Asset asset = Asset.createAssetFromMap(simpleAssetMap);
        assertNull(asset.getState());
    }

    /**
     * deserializeAssetFromJson should return an asset that has not been modified in anyway, so it
     * should not have created/updated times set, and should not have a state.
     *
     * @throws Exception
     */
    @Test
    public void testDeserializeAssetFromJson4() throws Exception {

        try {
            Asset.deserializeAssetFromJson(jsonArray);
        } catch (InvalidJsonAssetException e) {
            Throwable e2 = e.getCause();
            assertTrue("Wrong type of exception", e2 instanceof JsonMappingException);
            return;
        }
        fail("An exception should have been thrown");

    }

    @Test
    public void testStateChange() throws InvalidJsonAssetException {
        Asset asset = Asset.deserializeAssetFromJson(simpleObject);
        asset = service.createAsset(asset);
        try {
            Asset.StateAction.PUBLISH.performAction(asset);
            assertEquals("", Asset.State.AWAITING_APPROVAL, asset.getState());

            Asset.StateAction.APPROVE.performAction(asset);
            assertEquals("", Asset.State.PUBLISHED, asset.getState());

            Asset.StateAction.UNPUBLISH.performAction(asset);
            assertEquals("", Asset.State.DRAFT, asset.getState());

            Asset.StateAction.PUBLISH.performAction(asset);
            Asset.StateAction.CANCEL.performAction(asset);
            assertEquals("", Asset.State.DRAFT, asset.getState());

            Asset.StateAction.PUBLISH.performAction(asset);
            Asset.StateAction.NEED_MORE_INFO.performAction(asset);
            assertEquals("", Asset.State.NEED_MORE_INFO, asset.getState());

            Asset.StateAction.PUBLISH.performAction(asset);
            assertEquals("", Asset.State.AWAITING_APPROVAL, asset.getState());

        } catch (RepositoryResourceLifecycleException e) {
            fail("Unexpected exception" + e);
        }
    }

    @Test(expected = RepositoryResourceLifecycleException.class)
    public void testBadStateChange() throws InvalidJsonAssetException, RepositoryResourceLifecycleException {
        Asset asset = Asset.deserializeAssetFromJson(simpleObject);
        asset = service.createAsset(asset);
        Asset.StateAction.NEED_MORE_INFO.performAction(asset);
    }

    @Test(expected = RepositoryResourceLifecycleException.class)
    public void testBadStateChange2() throws InvalidJsonAssetException, RepositoryResourceLifecycleException {
        Asset asset = Asset.deserializeAssetFromJson(simpleObject);
        asset = service.createAsset(asset);
        Asset.StateAction.PUBLISH.performAction(asset);
        Asset.StateAction.PUBLISH.performAction(asset);
    }

    @Test(expected = RepositoryResourceLifecycleException.class)
    public void testBadStateChange3() throws InvalidJsonAssetException, RepositoryResourceLifecycleException {
        Asset asset = Asset.deserializeAssetFromJson(simpleObject);
        asset = service.createAsset(asset);
        Asset.StateAction.PUBLISH.performAction(asset);
        Asset.StateAction.UNPUBLISH.performAction(asset);
    }

}
