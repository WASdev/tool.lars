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

import org.apache.http.entity.ContentType;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.testutils.FatUtils;

/**
 * Tests to ensure that the BaseUrl configuration property is respected
 */
public class BaseUrlTest {

    @Rule
    public RepositoryContext baseUrlRepo = new RepositoryContext(FatUtils.BASEURL_SERVER_URL, "admin", "passw0rd", true);

    @Test
    public void testBaseUrl() throws Exception {
        Asset testAsset = AssetUtils.getTestAsset();
        Asset returnedAsset = baseUrlRepo.addAssetNoAttachments(testAsset);

        Attachment attachmentWithContent = AssetUtils.getTestAttachmentWithContent();

        String attachmentName = "attachment.txt";
        byte[] content = "This is the content.\nIt is quite short.".getBytes("UTF-8");

        Attachment createdAttachment = baseUrlRepo.doPostAttachmentWithContent(returnedAsset.get_id(),
                                                                               attachmentName,
                                                                               attachmentWithContent,
                                                                               content,
                                                                               ContentType.APPLICATION_OCTET_STREAM);

        String expectedUrl = "http://example.com/test" + FatUtils.LARS_APPLICATION_ROOT
                             + "/assets/" + returnedAsset.get_id()
                             + "/attachments/" + createdAttachment.get_id()
                             + "/" + attachmentName;

        assertEquals("Created attachment URL is not correct", expectedUrl, createdAttachment.getUrl());

        Attachment returnedAttachment = baseUrlRepo.doGetOnlyAttachment(returnedAsset.get_id(), createdAttachment.get_id());

        assertEquals("Fetched attachment URL is not correct", expectedUrl, returnedAttachment.getUrl());

    }
}
