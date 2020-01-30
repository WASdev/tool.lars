/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
package com.ibm.ws.repository.transport.client.test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.ws.lars.testutils.clients.DirectoryWriteableClient;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.transport.client.DirectoryClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.ClientFailureException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;

public class AbstractFileClientTest {

    @Test
    public void testEmptyFiltersAreIgnored() throws IOException, RequestFailureException, SecurityException, BadVersionException, ClientFailureException {

        File repoDir = getTempDir();

        Asset asset = new Asset();
        asset.setName("a silly asset");
        asset.setType(ResourceType.FEATURE);

        DirectoryWriteableClient writeableClient = new DirectoryWriteableClient(repoDir);
        writeableClient.addAsset(asset);

        Map<FilterableAttribute, Collection<String>> filters =
                        new HashMap<FilterableAttribute, Collection<String>>();
        filters.put(FilterableAttribute.TYPE, Collections.singleton(ResourceType.FEATURE.getValue()));
        filters.put(FilterableAttribute.LOWER_CASE_SHORT_NAME, Collections.<String> emptySet());

        DirectoryClient client = new DirectoryClient(repoDir);
        Collection<Asset> filtered = client.getFilteredAssets(filters);
        assertThat("One asset should have been returned by the filtering operation", filtered, hasSize(1));
        assertEquals("An asset with the wrong name came back", "a silly asset", filtered.iterator().next().getName());

    }

    public static File getTempDir() throws IOException {
        File tmpRepoRoot = File.createTempFile("tempRepoDir", null);
        tmpRepoRoot.delete();
        File tmpRepoDir = new File(tmpRepoRoot.getPath());
        if (!tmpRepoDir.mkdir()) {
            throw new IOException("Couldn't create directory for temp repo directory: " + tmpRepoDir);
        }
        tmpRepoDir.deleteOnExit();
        return tmpRepoDir;
    }

}
