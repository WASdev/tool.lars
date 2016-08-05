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

import static com.ibm.ws.lars.rest.AssetUtils.getTestAsset;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.http.client.methods.HttpGet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.ibm.ws.lars.rest.RepositoryContext.Protocol;
import com.ibm.ws.lars.rest.model.Asset.State;
import com.ibm.ws.lars.testutils.FatUtils;

/**
 * Tests for the LARS front page
 */
@RunWith(Parameterized.class)
public class FrontPageTest {

    @Rule
    public final RepositoryContext repository;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { Protocol.HTTP, "http://localhost:" + FatUtils.LIBERTY_PORT_HTTP },
                                             { Protocol.HTTPS, "https://localhost:" + FatUtils.LIBERTY_PORT_HTTPS },
                                             { Protocol.HTTP, "http://localhost:" + FatUtils.LIBERTY_PORT_HTTP + "/bluemix" },
                                             { Protocol.HTTPS, "https://localhost:" + FatUtils.LIBERTY_PORT_HTTPS + "/bluemix" } });
    }

    private final String baseUrl;

    public FrontPageTest(Protocol protocol, String url) {
        this.repository = RepositoryContext.createAsAdmin(protocol);
        this.baseUrl = url;
    }

    @Test
    public void testFrontPage() throws Exception {
        JsonParser frontPageJsonParser = getJsonParser();

        String serverName = null;
        int assetCount = -1;

        while (frontPageJsonParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = frontPageJsonParser.getCurrentName();
            if (("serverName").equals(fieldName)) {
                frontPageJsonParser.nextToken();
                serverName = frontPageJsonParser.getValueAsString();
            }
            else if (("assetCount").equals(fieldName)) {
                frontPageJsonParser.nextToken();
                assetCount = frontPageJsonParser.getIntValue();
            }
        }
        assertEquals("LARS front page JSON server name", "LARS", serverName);
        assertEquals("LARS front page asset count", 0, assetCount);
    }

    @Test
    public void testCorrectCount() throws Exception {
        repository.addAssetNoAttachmentsWithState(getTestAsset(), State.PUBLISHED);
        repository.addAssetNoAttachmentsWithState(getTestAsset(), State.AWAITING_APPROVAL);
        repository.addAssetNoAttachmentsWithState(getTestAsset(), State.DRAFT);
        repository.addAssetNoAttachmentsWithState(getTestAsset(), State.NEED_MORE_INFO);

        JsonParser frontPageJsonParser = getJsonParser();

        int assetCount = -1;

        while (frontPageJsonParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = frontPageJsonParser.getCurrentName();
            if (("assetCount").equals(fieldName)) {
                frontPageJsonParser.nextToken();
                assetCount = frontPageJsonParser.getIntValue();
            }
        }
        assertEquals("LARS front page asset count", 1, assetCount);
    }

    private JsonParser getJsonParser() throws JsonParseException, IOException {
        HttpGet req = new HttpGet(baseUrl + "/");
        String fpText = repository.doRequest(req, 200);
        JsonParser jsonParser = new JsonFactory().createParser(fpText);
        return jsonParser;
    }
}
