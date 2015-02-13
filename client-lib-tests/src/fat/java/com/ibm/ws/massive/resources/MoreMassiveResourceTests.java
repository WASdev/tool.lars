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

package com.ibm.ws.massive.resources;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;

import com.ibm.ws.massive.sa.client.model.Attachment;
import com.ibm.ws.massive.sa.client.model.AttachmentSummary;

public class MoreMassiveResourceTests {

    @Test
    public void testLocaleProcessing() {
        Collection<AttachmentSummary> attachments = new ArrayList<AttachmentSummary>();
        attachments.add(new FakeAttachmentResource(new FakeAttachment(Attachment.Type.LICENSE), Locale.ENGLISH, "English license"));
        attachments.add(new FakeAttachmentResource(new FakeAttachment(Attachment.Type.LICENSE), Locale.FRENCH, "French license"));

        AttachmentSummary a = MassiveResource.matchByLocale(attachments, Attachment.Type.LICENSE, Locale.UK);
        assertEquals("Failed to match UK", "English license", a.getName());

        a = MassiveResource.matchByLocale(attachments, Attachment.Type.LICENSE, Locale.US);
        assertEquals("Failed to match US", "English license", a.getName());

        a = MassiveResource.matchByLocale(attachments, Attachment.Type.LICENSE, Locale.CANADA);
        assertEquals("Failed to match Canada", "English license", a.getName());

        a = MassiveResource.matchByLocale(attachments, Attachment.Type.LICENSE, Locale.CANADA_FRENCH);
        assertEquals("Failed to match French Canada", "French license", a.getName());
    }

    @Test
    public void testWeAtLeastGetEnglishIfThatsAllThereIs() {

        Collection<AttachmentSummary> attachments = new ArrayList<AttachmentSummary>();
        attachments.add(new FakeAttachmentResource(new FakeAttachment(Attachment.Type.LICENSE), Locale.ITALY, "Italian license"));
        attachments.add(new FakeAttachmentResource(new FakeAttachment(Attachment.Type.LICENSE), Locale.ENGLISH, "English license"));

        AttachmentSummary a = MassiveResource.matchByLocale(attachments, Attachment.Type.LICENSE, Locale.GERMAN);
        assertEquals("Failed to match English last resort", "English license", a.getName());

    }

    @Test
    public void testWeGetAnyEnglishIfThatsAllThatsLeft() {
        Collection<AttachmentSummary> attachments = new ArrayList<AttachmentSummary>();
        attachments.add(new FakeAttachmentResource(new FakeAttachment(Attachment.Type.LICENSE), Locale.US, "American license"));
        attachments.add(new FakeAttachmentResource(new FakeAttachment(Attachment.Type.LICENSE), Locale.FRANCE, "License Francais"));

        AttachmentSummary a = MassiveResource.matchByLocale(attachments, Attachment.Type.LICENSE, Locale.GERMAN);
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

        Attachment.Type type;

        public FakeAttachment(Attachment.Type t) {
            type = t;
        }

        @Override
        public Type getType() {
            return type;
        }
    }

    /*
     * attachement = get attachment by locale;
     * if (attachment != null)
     * return attachment;
     * targetLocale = new Locale(locale.getLanguage());
     * return get attachment by targetLocale;
     *
     * private Locale matchLocale (Locale matchThis) {
     * if (knownLocales.contains(matchThis)) {
     * return matchThis;
     * }
     * Locale alternate = new Locale(matchThis.getLanguage());
     * if (knownLocales.contains(alternate)) {
     * return alternate;
     * }
     * return null;
     * }
     */
}
