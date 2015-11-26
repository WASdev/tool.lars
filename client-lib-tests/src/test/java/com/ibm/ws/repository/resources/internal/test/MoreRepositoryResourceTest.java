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

package com.ibm.ws.repository.resources.internal.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;

public class MoreRepositoryResourceTest {

    @Test
    public void testLocaleProcessing() {
        Collection<AttachmentSummary> attachments = new ArrayList<AttachmentSummary>();
        attachments.add(new FakeAttachmentResource(new FakeAttachment(AttachmentType.LICENSE), Locale.ENGLISH, "English license"));
        attachments.add(new FakeAttachmentResource(new FakeAttachment(AttachmentType.LICENSE), Locale.FRENCH, "French license"));

        AttachmentSummary a = RepositoryResourceImpl.matchByLocale(attachments, AttachmentType.LICENSE, Locale.UK);
        assertEquals("Failed to match UK", "English license", a.getName());

        a = RepositoryResourceImpl.matchByLocale(attachments, AttachmentType.LICENSE, Locale.US);
        assertEquals("Failed to match US", "English license", a.getName());

        a = RepositoryResourceImpl.matchByLocale(attachments, AttachmentType.LICENSE, Locale.CANADA);
        assertEquals("Failed to match Canada", "English license", a.getName());

        a = RepositoryResourceImpl.matchByLocale(attachments, AttachmentType.LICENSE, Locale.CANADA_FRENCH);
        assertEquals("Failed to match French Canada", "French license", a.getName());
    }

    @Test
    public void testWeAtLeastGetEnglishIfThatsAllThereIs() {

        Collection<AttachmentSummary> attachments = new ArrayList<AttachmentSummary>();
        attachments.add(new FakeAttachmentResource(new FakeAttachment(AttachmentType.LICENSE), Locale.ITALY, "Italian license"));
        attachments.add(new FakeAttachmentResource(new FakeAttachment(AttachmentType.LICENSE), Locale.ENGLISH, "English license"));

        AttachmentSummary a = RepositoryResourceImpl.matchByLocale(attachments, AttachmentType.LICENSE, Locale.GERMAN);
        assertEquals("Failed to match English last resort", "English license", a.getName());

    }

    @Test
    public void testWeGetAnyEnglishIfThatsAllThatsLeft() {
        Collection<AttachmentSummary> attachments = new ArrayList<AttachmentSummary>();
        attachments.add(new FakeAttachmentResource(new FakeAttachment(AttachmentType.LICENSE), Locale.US, "American license"));
        attachments.add(new FakeAttachmentResource(new FakeAttachment(AttachmentType.LICENSE), Locale.FRANCE, "License Francais"));

        AttachmentSummary a = RepositoryResourceImpl.matchByLocale(attachments, AttachmentType.LICENSE, Locale.GERMAN);
        assertEquals("Failed to match American as last resort", "American license", a.getName());

    }

    class FakeAttachmentResource implements AttachmentSummary {

        Attachment attachment;
        Locale locale;
        String name;

        public FakeAttachmentResource(Attachment a, Locale l, String n) {
            this.attachment = a;
            this.locale = l;
            this.name = n;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public File getFile() {
            return null;
        }

        @Override
        public String getURL() {
            return null;
        }

        @Override
        public Attachment getAttachment() {
            return this.attachment;
        }

        @Override
        public Locale getLocale() {
            return this.locale;
        }
    }

    class FakeAttachment extends Attachment {

        AttachmentType type;

        public FakeAttachment(AttachmentType t) {
            type = t;
        }

        @Override
        public AttachmentType getType() {
            return type;
        }
    }

}
