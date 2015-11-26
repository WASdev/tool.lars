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
package com.ibm.ws.repository.transport.client.test;

import java.io.File;
import java.util.Locale;

import com.ibm.ws.repository.common.enums.AttachmentLinkType;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;

/**
 *
 */
public class MockAttachmentSummary implements AttachmentSummary {

    private final Attachment attachment;
    private final File file;

    public MockAttachmentSummary(File file, String name, AttachmentType type, long crc, String url) {
        this.file = file;
        attachment = new Attachment();

        attachment.setName(name);

        if (type == null) {
            // if we explicitly passed a null Attachment.Type do not set the attachment
            // type ... this is only for testing purposes
        } else {
            attachment.setType(type); // must set a Type
        }

        attachment.getWlpInformation().setCRC(crc);

        if (url != null && !url.isEmpty()) {
            attachment.setLinkType(AttachmentLinkType.DIRECT);
            attachment.setUrl(url);
        }
    }

    @Override
    public String getName() {
        return attachment.getName();
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getURL() {
        return attachment.getUrl();
    }

    @Override
    public Attachment getAttachment() {
        return attachment;
    }

    @Override
    public Locale getLocale() {
        return attachment.getLocale();
    }

}
