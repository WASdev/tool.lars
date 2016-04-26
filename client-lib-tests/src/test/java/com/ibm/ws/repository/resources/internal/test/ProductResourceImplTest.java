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
package com.ibm.ws.repository.resources.internal.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.WlpInformation;

public class ProductResourceImplTest {

    @Test
    public void testProductVanityUrlNoAttachment() throws RepositoryResourceCreationException {
        Asset asset = new Asset();
        asset.setType(ResourceType.INSTALL);
        WlpInformation wlp = new WlpInformation();
        asset.setWlpInformation(wlp);
        asset.setName("fooName");
        ProductResourceImpl product = new ProductResourceImpl(null, asset);
        product.updateGeneratedFields(true);
        String vanity = product.getVanityURL();
        assertEquals("The vanity url wasn't what was expected", "runtimes-fooName", vanity);
    }

    @Test
    public void testProductVanityUrlWithAttachment() throws Exception {
        Asset asset = new Asset();
        asset.setType(ResourceType.INSTALL);
        WlpInformation wlp = new WlpInformation();
        asset.setWlpInformation(wlp);
        asset.setName("fooName");
        ProductResourceImpl product = new ProductResourceImpl(null, asset);

        // Need to have an attachment file, but content is unimportant
        File dummy = File.createTempFile("dummy", null);
        FileOutputStream foo = new FileOutputStream(dummy);
        foo.write(12345);
        foo.close();
        dummy.deleteOnExit();

        product.addAttachment(dummy, AttachmentType.CONTENT, "what a good-name-not this bit");
        product.updateGeneratedFields(true);

        String vanity = product.getVanityURL();
        assertEquals("The vanity url wasn't what was expected", "runtimes-what_a_good-name", vanity);
    }

}
