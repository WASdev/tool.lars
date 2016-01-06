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

import static com.ibm.ws.lars.testutils.ReflectionTricks.reflectiveCallAnyTypes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.transport.client.AbstractFileClient;
import com.ibm.ws.repository.transport.client.DirectoryClient;
import com.ibm.ws.repository.transport.client.ZipClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;

/**
 *
 */
@RunWith(Parameterized.class)
public class FileClientLicenseTest {

    protected AbstractFileClient _licenseClient;

    public FileClientLicenseTest(AbstractFileClient client, String name) {
        _licenseClient = client;
    }

    @Parameters(name = "{1}")
    public static Object[][] getParameters() {
        File resourcesDir = new File("resources");
        return new Object[][] {
                               { new DirectoryClient(new File(resourcesDir, "licenseTestRepo")), "Directory" },
                               { new ZipClient(new File(resourcesDir, "licenseTestRepo.zip")), "Zip" }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLicensesFromJarFileWithoutLicenses() throws Exception {
        String assetId = "sampleNoLicense.jar";

        assertTrue("The sample asset should not have licenses.",
                   !((Boolean) reflectiveCallAnyTypes(_licenseClient, "hasLicenses", new Class[] { String.class }, new Object[] { assetId })));
        Map<String, Long> licenses = (Map<String, Long>) reflectiveCallAnyTypes(_licenseClient, "getLicenses", new Class[] { String.class }, new Object[] { assetId });
        assertTrue("The licenses map should be empty", licenses.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLicensesFromEsaFileWithoutLicenses() throws Exception {
        String assetId = "esaNoLicense.esa";

        assertTrue("The sample asset should not have licenses.",
                   !((Boolean) reflectiveCallAnyTypes(_licenseClient, "hasLicenses", new Class[] { String.class }, new Object[] { assetId })));
        Map<String, Long> licenses = (Map<String, Long>) reflectiveCallAnyTypes(_licenseClient, "getLicenses", new Class[] { String.class }, new Object[] { assetId });
        assertTrue("The licenses map should be empty", licenses.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLicensesFromJarFile() throws Exception {
        String assetId = "sampleWithLicense.jar";

        assertTrue("The sample asset should have licenses.",
                   (Boolean) reflectiveCallAnyTypes(_licenseClient, "hasLicenses", new Class[] { String.class }, new Object[] { assetId }));
        Map<String, Long> licenses = (Map<String, Long>) reflectiveCallAnyTypes(_licenseClient, "getLicenses", new Class[] { String.class }, new Object[] { assetId });
        assertTrue("The licenses map should not be empty", !licenses.isEmpty());
        assertTrue("The licenses map should contain LA_ or LI_", licenses.keySet().iterator().next().contains("LA_")
                                                                 || licenses.keySet().iterator().next().contains("LI_"));
        String key = licenses.keySet().iterator().next();
        System.out.println("Key = " + key + " of size " + licenses.get(key));
        assertTrue("The licenses map should contain size for license file", licenses.get(key) != 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLicensesFromEsaFile() throws Exception {
        String assetId = "esaWithLicense.esa";

        assertTrue("The esa asset should have licenses.",
                   (Boolean) reflectiveCallAnyTypes(_licenseClient, "hasLicenses", new Class[] { String.class }, new Object[] { assetId }));
        Map<String, Long> licenses = (Map<String, Long>) reflectiveCallAnyTypes(_licenseClient, "getLicenses", new Class[] { String.class }, new Object[] { assetId });
        assertTrue("The licenses map should not be empty", !licenses.isEmpty());
        assertTrue("The licenses map should contain LA_", licenses.keySet().iterator().next().contains("LA_"));
        String key = licenses.keySet().iterator().next();
        assertTrue("The licenses map should contain size for LA_* file", licenses.get(key) != 0);
    }

    @Test
    public void testGetLAHeaderAndLIHeaderFromJarFile() throws Exception {
        String assetId = "sampleWithLicense.jar";

        String header = (String) reflectiveCallAnyTypes(_licenseClient, "getHeader", new Class[] { String.class, String.class }, new Object[] { assetId,
                                                                                                                                               DirectoryClient.LA_HEADER_PRODUCT });
        System.out.println("Got LA header value " + header);
        assertNotNull(header);
        header = (String) reflectiveCallAnyTypes(_licenseClient, "getHeader", new Class[] { String.class, String.class },
                                                 new Object[] { assetId, DirectoryClient.LI_HEADER_PRODUCT });
        System.out.println("Got LI header value " + header);
        assertNotNull(header);
    }

    @Test
    public void testGetLAHeaderFromEsaFile() throws Exception {
        String assetId = "esaWithLicense.esa";

        String header = (String) reflectiveCallAnyTypes(_licenseClient, "getHeader", new Class[] { String.class, String.class },
                                                        new Object[] { assetId, DirectoryClient.LA_HEADER_FEATURE });
        System.out.println("Got LA header value " + header);
        assertNotNull(header);
    }

    @Test
    public void testGetEsaAssetWithLicenses() throws IOException, BadVersionException, RequestFailureException {
        String assetId = "esaWithLicense.esa";
        Asset ass = _licenseClient.getAsset(assetId);
        assertNotNull("We should have got an asset back");
        assertEquals("Should be the asset we were after", assetId, ass.get_id());
        List<Attachment> attachments = ass.getAttachments();
        assertEquals("Should have 19 attachments, 18 licenses and 1 main attachment", 19, attachments.size());
        for (Attachment att : attachments) {
            if (att.getType() == AttachmentType.LICENSE_AGREEMENT)
                assertTrue("attachment id should contain #licenses", att.get_id().contains("#licenses" + File.separator + "LA"));
            assertNotNull(_licenseClient.getAttachment(ass, att));
        }
    }

    /**
     * This test loads grails asset, note that only the LA files are loaded as licenses, we don't load LI files
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     */
    @Test
    public void testGetJarAssetWithLicenses() throws IOException, BadVersionException, RequestFailureException {
        String assetId = "sampleWithLicense.jar";
        Asset ass = _licenseClient.getAsset(assetId);
        assertNotNull("We should have got an asset back");
        assertEquals("Should be the asset we were after", assetId, ass.get_id());
        List<Attachment> attachments = ass.getAttachments();
        assertEquals("Should have 3 attachments, 1 LA licenses 1 LI License and 1 main attachment", 3, attachments.size());
        for (Attachment att : attachments) {
            if (att.getType() == AttachmentType.LICENSE_AGREEMENT)
                assertTrue("attachment id should contain #licenses", att.get_id().contains("#licenses" + File.separator + "LA"));
        }
    }

    @Test
    public void testGetLicenseAttachmentFromArchive() throws IOException, BadVersionException, RequestFailureException {
        Asset asset = _licenseClient.getAsset("esaWithLicense.esa");

        Attachment licenseAttachment = null;
        for (Attachment att : asset.getAttachments()) {
            if (att.getType() == AttachmentType.LICENSE_AGREEMENT && att.getLocale().equals(Locale.ENGLISH)) {
                licenseAttachment = att;
            }
        }

        InputStream is = _licenseClient.getAttachment(asset, licenseAttachment);

        assertNotNull(is);
        assertTrue("InputStream should be opened", is.available() > 0);

        is.close();
    }
}
