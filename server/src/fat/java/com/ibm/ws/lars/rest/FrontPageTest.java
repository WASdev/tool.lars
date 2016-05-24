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
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.apache.http.client.methods.HttpGet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.lars.rest.RepositoryContext.Protocol;
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
        HttpGet req = new HttpGet(baseUrl + "/");
        String text = repository.doRequest(req, 200);
        assertThat(text, containsString("The repository is running with 0 assets"));
    }

    @Test
    public void testCorrectCount() throws Exception {
        repository.addAssetNoAttachments(getTestAsset());
        repository.addAssetNoAttachments(getTestAsset());
        HttpGet req = new HttpGet(baseUrl + "/");
        String text = repository.doRequest(req, 200);
        assertThat(text, containsString("The repository is running with 2 assets"));
    }

}
