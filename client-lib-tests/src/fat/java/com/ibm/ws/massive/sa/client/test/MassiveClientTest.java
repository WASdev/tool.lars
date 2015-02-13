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

package com.ibm.ws.massive.sa.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.RepositoryFixture;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.sa.client.BadVersionException;
import com.ibm.ws.massive.sa.client.ClientLoginInfo;
import com.ibm.ws.massive.sa.client.MassiveClient;
import com.ibm.ws.massive.sa.client.MassiveClient.FilterableAttribute;
import com.ibm.ws.massive.sa.client.RequestFailureException;
import com.ibm.ws.massive.sa.client.model.AppliesToFilterInfo;
import com.ibm.ws.massive.sa.client.model.Asset;
import com.ibm.ws.massive.sa.client.model.Asset.Privacy;
import com.ibm.ws.massive.sa.client.model.Asset.State;
import com.ibm.ws.massive.sa.client.model.Asset.Type;
import com.ibm.ws.massive.sa.client.model.Attachment;
import com.ibm.ws.massive.sa.client.model.Attachment.LinkType;
import com.ibm.ws.massive.sa.client.model.AttachmentInfo;
import com.ibm.ws.massive.sa.client.model.AttachmentSummary;
import com.ibm.ws.massive.sa.client.model.FilterVersion;
import com.ibm.ws.massive.sa.client.model.StateAction.Action;
import com.ibm.ws.massive.sa.client.model.WlpInformation;
import com.ibm.ws.massive.sa.client.model.WlpInformation.Visibility;

/**
 * Tests for the {@link MassiveClient} object. To run it requires having a
 * testLogin.properties folder at the root of the com.ibm.ws.massive.testsuite
 * project
 */
public class MassiveClientTest {

    private final static File resourcesDir = new File("resources");
    private static MassiveClient massiveClient;
    private static MassiveClient userRoleMassiveClient;
    /**
     * collection of asset IDs to delete when a test is finished
     */
    private final Collection<String> toDelete = new HashSet<String>();

    @Rule
    public RepositoryFixture repo = FatUtils.FAT_REPO;

    private LoginInfoEntry _loginInfoEntry;

    private LoginInfoEntry _userRoleLoginInfoEntry;

    @Before
    public void createClients() {
        _loginInfoEntry = repo.getLoginInfoEntry();
        _userRoleLoginInfoEntry = repo.getUserLoginInfoEntry();

        // These must be re-created before every test because the loginInfo object will be refreshed if the server goes down
        massiveClient = new MassiveClient(_loginInfoEntry.getClientLoginInfo());

        // ...and a user-role client
        userRoleMassiveClient = new MassiveClient(_userRoleLoginInfoEntry.getClientLoginInfo());
    }

    /**
     * Deletes all the assets in {@link #toDelete}.
     *
     * @throws IOException
     * @throws RequestFailureException
     */
    @After
    public void deleteAssets() throws IOException, RequestFailureException {
        for (String assetId : this.toDelete) {
            massiveClient.deleteAssetAndAttachments(assetId);
        }
        this.toDelete.clear();
    }

    /**
     * Test for {@link MassiveClient#getAllAssetsMetadata()}
     *
     * @throws RequestFailureException
     * @throws IOException
     */
    @Test
    public void testGetAllAssetsMetadata() throws IOException, RequestFailureException {
        Map<String, List<String>> headers = massiveClient.getAllAssetsMetadata();

        // We currently don't require any specific headers so just check we have some headers
        assertFalse("There should be some headers returned", headers.isEmpty());
    }

    /**
     * This test makes sure that we can get assets from Massive
     *
     * @throws IOException
     */
    @Test
    public void testGetAllAssets() throws IOException, RequestFailureException {
        @SuppressWarnings("unused")
        List<Asset> assets = massiveClient.getAllAssets();

        /*
         * We don't have any consistent test data in there at the moment, if we
         * get this far call it a pass!
         */
    }

    /**
     * This method will test adding an asset and then deleting it
     *
     * @throws IOException
     */
    @Test
    public void testAddAndDeleteAsset() throws Exception {
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        assertNotNull("The ID should of been set by massive",
                      createdAsset.get_id());
        assertEquals(
                     "The returned asset name should match the supplied one",
                     newAsset.getName(), createdAsset.getName());
    }

    /**
     * This method will test adding an asset and then getting it
     *
     * @throws IOException
     */
    @Test
    public void testGetAsset() throws Exception {
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        Asset gotAsset = massiveClient.getAsset(createdAsset.get_id());
        assertEquals(
                     "The ID should be the same of the got asset as the one created originally",
                     createdAsset.get_id(), gotAsset.get_id());
        assertEquals(
                     "The returned asset name should match the supplied one",
                     newAsset.getName(), gotAsset.getName());
    }

    /**
     * This tests that custom attributes are created in Massive
     */
    @Test
    public void testCustomAttributes() throws Exception {
        Asset newAsset = createTestAsset();

        // Add custom attributes
        WlpInformation wlpInfo = new WlpInformation();
        String appliesTo = "Test applies to";
        wlpInfo.setAppliesTo(appliesTo);
        newAsset.setWlpInformation(wlpInfo);
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        WlpInformation createdWlpInfo = createdAsset.getWlpInformation();
        assertNotNull("The created asset should contain wlp infromation",
                      createdWlpInfo);
        assertEquals(
                     "Fields set on the wlp information should match in the returned object",
                     appliesTo, createdWlpInfo.getAppliesTo());
    }

    /**
     * Test to make sure that if you don't have permission to upload then you get a {@link RequestFailureException} with the right response code and message in it.
     *
     * @throws BadVersionException
     * @throws IOException
     */
    @Test
    @Ignore
    // needs auth
    public void testFailToUpload() throws IOException, RequestFailureException, BadVersionException {
        Asset newAsset = createTestAsset();
        MassiveClient client = new MassiveClient(new ClientLoginInfo("does.not.exists@nothere.com", "doesn't matter", _loginInfoEntry.getApiKey(), _loginInfoEntry.getRepositoryUrl(), _loginInfoEntry.getSoftlayerUserId(), _loginInfoEntry.getSoftlayerPassword()));
        try {
            client.addAsset(newAsset);
            fail("Should have thrown a RequestFailureException");
        } catch (RequestFailureException e) {
            assertEquals("The error message should match", "Invalid credentials", e.getErrorMessage());
            assertEquals("The error stream contents should match", "{\"statusCode\":401,\"message\":\"Invalid credentials\",\"stack\":\"\"}", e.getErrorStreamContents());
            assertEquals("The message should match",
                         "Server returned HTTP response code: 401 for URL: " + _loginInfoEntry.getRepositoryUrl() + "/assets?apiKey=" + _loginInfoEntry.getApiKey()
                                         + " error message: \"Invalid credentials\"", e.getMessage());
            assertEquals("The response code should match", 401, e.getResponseCode());
        }
    }

    private static Attachment addAttachment(String id, final String name,
                                            final File f, int crc) throws IOException, BadVersionException,
                    RequestFailureException {

        // default to adding CONTENT attachment and not supplying a URL
        return addAttachment(id, name, f, crc, Attachment.Type.CONTENT, null);
    }

    private static Attachment addAttachment(String id, final String name, final File f, int crc, String url)
                    throws IOException, BadVersionException, RequestFailureException {

        // default to adding CONTENT attachment
        return addAttachment(id, name, f, crc, Attachment.Type.CONTENT, url);
    }

    private static Attachment addAttachment(String id, final String name, final File f, int crc, Attachment.Type type, final String url)
                    throws IOException, BadVersionException, RequestFailureException {
        System.out.println("Adding attachment:");
        System.out.println("Asset id: " + id);
        System.out.println("File: " + f.getAbsolutePath());
        System.out.println("Type: " + type);
        System.out.println("URL: " + url);
        // Add the attachment
        final Attachment at = new Attachment();
        if (type == null) {
            // if we explicitly passed a null Attachment.Type do not set the attachment
            // type ... this is only for testing purposes
        } else {
            at.setType(type); // must set a Type
        }
        at.getWlpInformation().setCRC(crc);
        if (url != null && !url.isEmpty()) {
            at.setLinkType(LinkType.DIRECT);
            at.setUrl(url);
        }
        Attachment newAt = massiveClient.addAttachment(id,
                                                       new AttachmentSummary() {
                                                           @Override
                                                           public String getName() {
                                                               return name;
                                                           }

                                                           @Override
                                                           public File getFile() {
                                                               return f;
                                                           }

                                                           @Override
                                                           public String getURL() {
                                                               return url;
                                                           }

                                                           @Override
                                                           public Attachment getAttachment() {
                                                               return at;
                                                           }

                                                           @Override
                                                           public Locale getLocale() {
                                                               return null;
                                                           }
                                                       });

        System.out.println("Attachment added:");
        System.out.println(newAt.toString());
        return newAt;
    }

    @Test
    public void testAttachmentEquivalent() throws Exception {
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        Attachment createdAttachment1 = null;
        Attachment createdAttachment2 = null;
        Attachment createdAttachment3 = null;
        Attachment createdAttachment4 = null;
        Attachment createdAttachment5 = null;
        Attachment createdAttachment6 = null;
        Attachment createdAttachment7 = null;
        Attachment createdAttachment8 = null;
        Attachment createdAttachment9 = null;
        Attachment createdAttachment10 = null;

        try {
            // Add the attachment
            createdAttachment1 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0);
            createdAttachment2 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0);
            assertTrue(createdAttachment1.equivalent(createdAttachment2));
            assertFalse(createdAttachment1.equals(createdAttachment2));

            // When we supply a URL (same one each time) then the attachments should be equivalent
            createdAttachment3 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "htpp://url");
            createdAttachment4 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "htpp://url");
            assertTrue(createdAttachment3.equivalent(createdAttachment4));
            assertFalse(createdAttachment3.equals(createdAttachment4));

            // The URLs are different but if link type is null then it's treated as equivalent (as we may copy attachments
            // that are stored in massive and get different URL's for attachments we want to this of as equivalent)
            createdAttachment5 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "apple");
            createdAttachment5.setLinkType(null);
            createdAttachment6 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "pear");
            createdAttachment6.setLinkType(null);
            assertTrue(createdAttachment5.equivalent(createdAttachment6));
            assertFalse(createdAttachment5.equals(createdAttachment6));

            // This time one of the link types is non null so equiv should fail
            createdAttachment7 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "apple");
            createdAttachment8 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "pear");
            createdAttachment8.setLinkType(null);
            assertFalse(createdAttachment7.equivalent(createdAttachment8));
            assertFalse(createdAttachment7.equals(createdAttachment8));

            // Check with other link type being non null
            createdAttachment9 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "apple");
            createdAttachment9.setLinkType(null);
            createdAttachment10 = addAttachment(createdAsset.get_id(),
                                                "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "pear");
            assertFalse(createdAttachment9.equivalent(createdAttachment10));
            assertFalse(createdAttachment9.equals(createdAttachment10));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testAddAttachmentWithInfo() throws Exception {
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        Attachment createdAttachment = null;

        try {
            // Add the attachment
            final Attachment at = new Attachment();
            at.setWlpInformation(new AttachmentInfo());
            at.getWlpInformation().setCRC(2345);
            createdAttachment = addAttachment(createdAsset.get_id(),
                                              "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 2345);
            assertEquals("The attachment asset ID should match the asset ID",
                         createdAsset.get_id(), createdAttachment.getAssetId());
            assertNotNull("The URL should of been set by massive",
                          createdAttachment.getUrl());
            assertEquals("The CRC value in the AttachmentInfo didn't match", 2345,
                         createdAttachment.getWlpInformation().getCRC());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Tests equivalence between two assets with the same attachment
     *
     * @throws Exception
     */
    @Test
    public void testAssetEquivalent() throws Exception {
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset1 = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset1.get_id());
        Asset createdAsset2 = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset2.get_id());
        Attachment createdAttachment1 = null;
        Attachment createdAttachment2 = null;
        Attachment createdAttachment3 = null;
        Attachment createdAttachment4 = null;
        Attachment createdAttachment5 = null;
        Attachment createdAttachment6 = null;

        try {
            createdAttachment1 = addAttachment(createdAsset1.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "http://url");
            createdAttachment2 = addAttachment(createdAsset2.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "http://url");

            // add attachment to asset
            createdAsset1.addAttachement(createdAttachment1);
            createdAsset2.addAttachement(createdAttachment2);
            // Attachments are equivalent as they have the same url
            assertTrue(createdAsset1.equivalent(createdAsset2));
            assertFalse(createdAsset1.equals(createdAsset2));

            // Add the attachment to massive
            createdAttachment3 = addAttachment(createdAsset1.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0);
            createdAttachment4 = addAttachment(createdAsset2.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0);

            // add attachment to asset
            createdAsset1.addAttachement(createdAttachment3);
            createdAsset2.addAttachement(createdAttachment4);
            // Attachments are equivalent as both attachments have generated URLs, and when they are generated (linkType
            // is set to null) then we don't check the URLs
            assertTrue(createdAsset1.equivalent(createdAsset2));
            assertFalse(createdAsset1.equals(createdAsset2));

            // Add the attachment to massive
            createdAttachment5 = addAttachment(createdAsset1.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, "apple");
            createdAttachment6 = addAttachment(createdAsset2.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0);
            // add attachment to asset
            createdAsset1.addAttachement(createdAttachment5);
            createdAsset2.addAttachement(createdAttachment6);
            // Attachments are no longer equivalent as as the URLs are different now (one is using a generated one and
            // other is hardcoded to use one)
            assertFalse(createdAsset1.equivalent(createdAsset2));
            assertFalse(createdAsset1.equals(createdAsset2));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Tests inequivalence between two attachments with the same content but
     * of different attachment types.
     *
     * @throws Exception
     */
    @Test
    public void testAttachmentNotEquivalent() throws Exception {
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        Attachment createdAttachment1 = null;
        Attachment createdAttachment2 = null;

        try {
            // Add the attachments to massive using different attachment types
            createdAttachment1 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, Attachment.Type.CONTENT, null);
            createdAttachment2 = addAttachment(createdAsset.get_id(),
                                               "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0, Attachment.Type.DOCUMENTATION, null);

            // add attachment to asset
            createdAsset.addAttachement(createdAttachment1);
            createdAsset.addAttachement(createdAttachment2);
            assertFalse(createdAttachment1.equivalent(createdAttachment2)); // Type is different
            assertFalse(createdAttachment1.equals(createdAttachment2));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Test that adding an attachment without an attachment type specified
     * causes an exception
     *
     * @throws Exception
     */
    @Test
    public void testAttachmentNoTypeSpecified() throws Exception {
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());

        try {
            // Add the attachments to massive without specifying an Attachment.Type
            addAttachment(createdAsset.get_id(), "TestAttachment.txt",
                          new File(resourcesDir, "TestAttachment.txt"), 0, null);
        } catch (IllegalArgumentException iae) {
            // Expected
        } catch (Exception e) {
            fail("Unexpected Exception caught " + e);
        }
    }

    /**
     * Try adding an attachment after creating an asset
     *
     * @throws IOException
     */
    @Test
    public void testAddAttachment() throws Exception {
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        Attachment createdAttachment = null;

        File attachmentFile = new File(resourcesDir, "TestAttachment.txt");
        System.out.println("Adding attachment: " + attachmentFile);
        // Add the attachment
        createdAttachment = addAttachment(createdAsset.get_id(),
                                          "TestAttachment.txt", attachmentFile, 0);
        assertEquals("The attachment asset ID should match the asset ID",
                     createdAsset.get_id(), createdAttachment.getAssetId());
        assertNotNull("The URL should of been set by massive",
                      createdAttachment.getUrl());

        // Get the attachment content to make sure it was added ok
        InputStream attachmentContentStream = massiveClient.getAttachment(
                                                                          createdAsset.get_id(), createdAttachment.get_id());
        BufferedReader attachmentContentReader = new BufferedReader(
                        new InputStreamReader(attachmentContentStream));
        assertEquals(
                     "The content in the attachment should be the same as what was uploaded",
                     "This is a test attachment",
                     attachmentContentReader.readLine());
    }

    @Test
    public void testGetAllAssetsDoesntGetAttachments() throws Exception {
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());

        // Add the attachment
        addAttachment(createdAsset.get_id(), "TestAttachment.txt", new File(
                        resourcesDir, "TestAttachment.txt"), 0);

        List<Asset> allAssets = massiveClient.getAllAssets();
        for (Asset theAsset : allAssets) {
            List<Attachment> noAttachments = theAsset.getAttachments();
            assertNull("There should not be any attachments from a getAllAssets call", noAttachments);
        }
    }

    @Test
    public void testGetAssetGetsAttachments() throws Exception {
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        Attachment createdAttachment = null;

        try {
            // Add the attachment
            createdAttachment = addAttachment(createdAsset.get_id(),
                                              "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0);

            Asset ass = massiveClient.getAsset(createdAsset.get_id());
            assertEquals("There should be 1 attachments from a getAsset call", 1,
                         ass.getAttachments().size());
            assertEquals("The attachment asset ID should match the asset ID",
                         createdAsset.get_id(), createdAttachment.getAssetId());

            Attachment readAttachment = ass.getAttachments().get(0);

            assertNotNull("The URL should of been set by massive",
                          readAttachment.getUrl());

            // Get the attachment content to make sure it was added ok
            InputStream attachmentContentStream = massiveClient.getAttachment(
                                                                              createdAsset.get_id(), readAttachment.get_id());
            BufferedReader attachmentContentReader = new BufferedReader(
                            new InputStreamReader(attachmentContentStream));
            assertEquals(
                         "The content in the attachment should be the same as what was uploaded",
                         "This is a test attachment",
                         attachmentContentReader.readLine());

            System.out.println("asset = " + ass.get_id());
            massiveClient.updateState(ass.get_id(), Action.PUBLISH);
            massiveClient.updateState(ass.get_id(), Action.APPROVE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Make sure we can delete an asset and all its attachments
     *
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     */
    @Test
    public void testDeleteAssetAndAttachments() throws Exception {
        // Add an asset that we'll add the attachments to
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);

        // Add the attachments
        Attachment createdAttachment1 = addAttachment(createdAsset.get_id(), "TestAttachment.txt",
                                                      new File(resourcesDir, "TestAttachment.txt"), 0);
        Attachment createdAttachment2 = addAttachment(createdAsset.get_id(), "TestAttachment2.txt",
                                                      new File(resourcesDir, "TestAttachment.txt"), 0);

        // Now delete the lot and make sure we can't access them any more
        massiveClient.deleteAssetAndAttachments(createdAsset.get_id());

        try {
            massiveClient.getAttachment(createdAsset.get_id(),
                                        createdAttachment1.get_id());
            fail("Attachment 1 should've been deleted");
        } catch (Exception e) {
            // pass
        }

        try {
            massiveClient.getAttachment(createdAsset.get_id(),
                                        createdAttachment2.get_id());
            fail("Attachment 2 should've been deleted");
        } catch (Exception e) {
            // pass
        }

        // And make sure the asset was deleted
        try {
            massiveClient.getAsset(createdAsset.get_id());
            fail("Asset should've been deleted");
        } catch (Exception e) {
            // pass
        }
    }

    /**
     * Test to make sure you can update an asset
     *
     * @throws IOException
     */
    @Test
    @Ignore
    // needs update
    public void testUpdate() throws Exception {
        Asset asset = createTestAsset();
        asset = massiveClient.addAsset(asset);
        toDelete.add(asset.get_id());
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        String updatedName = "updatedName";
        asset.setName(updatedName);
        Asset updatedAsset = massiveClient.updateAsset(asset);
        assertEquals("The asset name should've been updated", updatedName,
                     updatedAsset.getName());
    }

    /**
     * Test to make sure that the state can be updated on an asset
     *
     * @throws IOException
     */
    @Test
    public void testStateUpdate() throws Exception {
        Asset asset = createTestAsset();
        asset = massiveClient.addAsset(asset);
        toDelete.add(asset.get_id());
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails by putting it in a try block
        massiveClient.updateState(asset.get_id(), Action.PUBLISH);
        Asset updatedAsset = massiveClient.getAsset(asset.get_id());
        assertEquals("The asset state should've been updated",
                     State.AWAITING_APPROVAL, updatedAsset.getState());
    }

    /**
     * Test to make sure that the state can be updated on an asset to the
     * published state
     *
     * @throws IOException
     */
    @Test
    @Ignore
    // needs reviews
    public void testPublishAsset() throws Exception {
        Asset asset = createTestAsset();
        asset = massiveClient.addAsset(asset);
        toDelete.add(asset.get_id());
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails by putting it in a try block

        // You need two calls to update the state to get to published
        massiveClient.updateState(asset.get_id(), Action.PUBLISH);
        massiveClient.updateState(asset.get_id(), Action.APPROVE);
        Asset updatedAsset = massiveClient.getAsset(asset.get_id());
        assertEquals("The asset state should've been updated",
                     State.PUBLISHED, updatedAsset.getState());
        assertNotNull("The asset should now have a review object",
                      updatedAsset.getReviewed());
    }

    /**
     * Test behaviour discovered Jan 13th, that PRIVATE assets skip the awaiting_approval state
     * Jan 13th 2014, Zeenat Lainwala writes
     * "If the asset is private, the asset goes directly to publish state when submitted.
     * We were planning to remove this private asset concept since we thought it was not
     * being used. Are you folks planning to use this attribute? What context are you using
     * the privacy attribute?"
     * Graham Charters replied,
     * "I don't believe we need this concept, we just had a test that used it."
     * this test will break if the underlying state model is changed.
     */
    @Test
    @Ignore
    //needs private assets
    public void testPrivateAssetsSkipAwaitingApprovalOnPublish() throws Exception {
        Asset rawAsset = createTestAsset();
        rawAsset.setPrivacy(Privacy.PRIVATE);
        Asset asset = massiveClient.addAsset(rawAsset);
        assertEquals("Wrong initial state", State.DRAFT, asset.getState());
        String assetId = asset.get_id();
        massiveClient.updateState(assetId, Action.PUBLISH);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.PUBLISHED, asset.getState());
    }

    @Test
    public void testLifecycleFromDraft() throws Exception {
        Asset asset = createTestAsset();
        asset = massiveClient.addAsset(asset);

        assertEquals("Wrong initial state", State.DRAFT, asset.getState());
        String assetId = asset.get_id();

        try {
            massiveClient.updateState(assetId, Action.CANCEL);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());

        try {
            massiveClient.updateState(assetId, Action.NEED_MORE_INFO);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());

        try {
            massiveClient.updateState(assetId, Action.APPROVE);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());

        massiveClient.updateState(assetId, Action.PUBLISH);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.AWAITING_APPROVAL, asset.getState());

    }

    @Test
    public void testLifecycleFromAwaitingApproval() throws Exception {
        Asset asset = createTestAsset();
        asset = massiveClient.addAsset(asset);
        String assetId = asset.get_id();
        massiveClient.updateState(assetId, Action.PUBLISH);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Wrong initial state", State.AWAITING_APPROVAL, asset.getState());

        massiveClient.updateState(assetId, Action.CANCEL);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());

        massiveClient.updateState(assetId, Action.PUBLISH);
        massiveClient.updateState(assetId, Action.NEED_MORE_INFO);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.NEED_MORE_INFO, asset.getState());

        massiveClient.updateState(assetId, Action.PUBLISH);
        massiveClient.updateState(assetId, Action.APPROVE);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.PUBLISHED, asset.getState());
    }

    @Test
    public void testLifecycleFromNeedMoreInfo() throws Exception {
        Asset asset = createTestAsset();
        asset = massiveClient.addAsset(asset);
        String assetId = asset.get_id();
        massiveClient.updateState(assetId, Action.PUBLISH);
        massiveClient.updateState(assetId, Action.NEED_MORE_INFO);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Wrong initial state", State.NEED_MORE_INFO, asset.getState());

        try {
            massiveClient.updateState(assetId, Action.APPROVE);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.NEED_MORE_INFO, asset.getState());

        try {
            massiveClient.updateState(assetId, Action.CANCEL);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.NEED_MORE_INFO, asset.getState());

        try {
            massiveClient.updateState(assetId, Action.NEED_MORE_INFO);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.NEED_MORE_INFO, asset.getState());

        massiveClient.updateState(assetId, Action.PUBLISH);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.AWAITING_APPROVAL, asset.getState());
    }

    @Test
    public void testLifecycleFromPublished() throws Exception {
        Asset asset = createTestAsset();
        asset = massiveClient.addAsset(asset);
        String assetId = asset.get_id();
        massiveClient.updateState(assetId, Action.PUBLISH);
        massiveClient.updateState(assetId, Action.APPROVE);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Wrong initial state", State.PUBLISHED, asset.getState());

        // Only the unpublish action can be done from the published state
        for (Action action : Action.values()) {
            try {
                if (action != Action.UNPUBLISH) {
                    massiveClient.updateState(assetId, action);
                    fail("Expected a request failure");
                }
            } catch (RequestFailureException e) {
                // expected
                assertEquals("Expected an exception saying this was invalid but had: " + e.getErrorMessage(), e.getErrorMessage(), "Invalid action " + action.getValue()
                                                                                                                                   + " performed on the asset with state published");
            }
            asset = massiveClient.getAsset(assetId);
            assertEquals("Unexpected state transition", State.PUBLISHED, asset.getState());
        }

        // Upublish back to draft state
        massiveClient.updateState(assetId, Action.UNPUBLISH);
        asset = massiveClient.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());
    }

    /**
     * Test to make sure you can update an attachment on an asset
     *
     * @throws IOException
     */
    @Test
    @Ignore
    // needs ignore URL when uploading a direct attachment
    public void testUpdateAttachment() throws Exception {
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        final Attachment createdAttachment = addAttachment(createdAsset.get_id(),
                                                           "TestAttachment.txt", new File(resourcesDir, "TestAttachment.txt"), 0);;

        // Get the attachment content to make sure it was added ok
        InputStream attachmentContentStream = massiveClient.getAttachment(
                                                                          createdAsset.get_id(), createdAttachment.get_id());
        BufferedReader attachmentContentReader = new BufferedReader(
                        new InputStreamReader(attachmentContentStream));
        assertEquals(
                     "The content in the attachment should be the same as what was uploaded",
                     "This is a test attachment",
                     attachmentContentReader.readLine());

        // Now update

        AttachmentSummary attachSummary = new AttachmentSummary() {

            @Override
            public String getName() {
                return "TestAttachment.txt";
            }

            @Override
            public File getFile() {
                return new File(resourcesDir, "TestAttachment2.txt");
            }

            @Override
            public String getURL() {
                return null;
            }

            @Override
            public Attachment getAttachment() {
                return createdAttachment;
            }

            @Override
            public Locale getLocale() {
                return null;
            }

        };
        Attachment updatedAttachment = massiveClient.updateAttachment(createdAsset.get_id(), attachSummary);
        attachmentContentStream = massiveClient.getAttachment(
                                                              createdAsset.get_id(), updatedAttachment.get_id());
        attachmentContentReader = new BufferedReader(
                        new InputStreamReader(attachmentContentStream));
        assertEquals(
                     "The content in the attachment should be the same as the udpated file",
                     "This is a different test attachment",
                     attachmentContentReader.readLine());
    }

    /**
     * This test will make sure you can get an asset by it's {@link WlpInformation.Type}.
     *
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     */
    @Test
    public void testGetByType() throws Exception {
        int initialEsaCount = massiveClient.getAssets(Type.FEATURE).size();
        int initialAddonCount = massiveClient.getAssets(Type.ADDON).size();
        int initialTotalCount = massiveClient.getAllAssets().size();

        // Add 3 assets with different types (including null)
        String esaAssetName = "ESA Asset";
        String addonAssetName = "Addon Asset";
        Asset asset = new Asset();
        asset.setName(esaAssetName);
        WlpInformation wlpInformation = new WlpInformation();
        asset.setType(Type.FEATURE);
        asset.setWlpInformation(wlpInformation);
        this.toDelete.add(massiveClient.addAsset(asset).get_id());

        asset = new Asset();
        asset.setName(addonAssetName);
        wlpInformation = new WlpInformation();
        asset.setType(Type.ADDON);
        asset.setWlpInformation(wlpInformation);
        this.toDelete.add(massiveClient.addAsset(asset).get_id());

        asset = new Asset();
        asset.setName(addonAssetName);
        this.toDelete.add(massiveClient.addAsset(asset).get_id());

        // Now make sure we get the right ones back when we call the method
        Collection<Asset> esas = massiveClient.getAssets(Type.FEATURE);
        assertEquals("Only one esa was added", initialEsaCount + 1, esas.size());
        boolean found = false;
        for (Asset esa : esas) {
            if (esaAssetName.equals(esa.getName())) {
                found = true;
                break;
            }
        }
        assertTrue("The name of the esa should match the supplied value", found);

        Collection<Asset> addons = massiveClient.getAssets(Type.ADDON);
        assertEquals("Only one addon was added", initialAddonCount + 1, addons.size());
        found = false;
        for (Asset addon : addons) {
            if (addonAssetName.equals(addon.getName())) {
                found = true;
                break;
            }
        }
        assertTrue("The name of the addon should match the supplied value", found);

        Collection<Asset> allAssets = massiveClient.getAllAssets();
        assertEquals("Three assets were added in total", initialTotalCount + 3, allAssets.size());

        assertEquals("Passing in null should get all assets", initialTotalCount + 3, massiveClient.getAssets(null).size());
    }

    /**
     * Creates a test asset with a timestamp in its name
     *
     * @return The asset
     */
    private Asset createTestAsset() {
        Asset newAsset = new Asset();
        String assetName = "Test Asset" + createDateString();
        newAsset.setName(assetName);
        return newAsset;
    }

    /**
     * Creates a string saying when something is created.
     *
     * @return
     */
    private String createDateString() {
        SimpleDateFormat dateFormater = new SimpleDateFormat(
                        "yyyy/MM/dd HH:mm:ss.SSS");
        String createdString = " created at "
                               + dateFormater.format(Calendar.getInstance().getTime());
        return createdString;
    }

    /**
     * Add an asset and ensure it can be read back without being authenticated
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void testUnauthReadOne() throws Exception {

        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        massiveClient.updateState(createdAsset.get_id(), Action.PUBLISH);
        massiveClient.updateState(createdAsset.get_id(), Action.APPROVE);

        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        Asset gotAsset = userRoleMassiveClient.getAsset(createdAsset.get_id());
        assertEquals("The ID should be the same of the got asset as the one created originally",
                     createdAsset.get_id(), gotAsset.get_id());
        assertEquals("The returned asset name should match the supplied one",
                     newAsset.getName(), gotAsset.getName());
    }

    /**
     * Add an asset and ensure it can be read back without being authenticated
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void testUnauthReadAttachment() throws Exception {

        Asset newAsset = createTestAsset();
        Asset createdAsset = massiveClient.addAsset(newAsset);
        toDelete.add(createdAsset.get_id());
        final Attachment createdAttachment = new Attachment();
        createdAttachment.setType(Attachment.Type.CONTENT);
        createdAttachment.setWlpInformation(new AttachmentInfo());
        createdAttachment.getWlpInformation().setCRC(0);

        try {
            // Add the attachment
            massiveClient.addAttachment(createdAsset.get_id(),
                                        new AttachmentSummary() {

                                            @Override
                                            public String getName() {
                                                return "TestAttachment.txt";
                                            }

                                            @Override
                                            public File getFile() {
                                                return new File(resourcesDir, "TestAttachment.txt");
                                            }

                                            @Override
                                            public String getURL() {
                                                return null;
                                            }

                                            @Override
                                            public Attachment getAttachment() {
                                                return createdAttachment;
                                            }

                                            @Override
                                            public Locale getLocale() {
                                                return null;
                                            }
                                        });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        massiveClient.updateState(createdAsset.get_id(), Action.PUBLISH);
        massiveClient.updateState(createdAsset.get_id(), Action.APPROVE);

        Asset gotAsset = userRoleMassiveClient.getAsset(createdAsset.get_id());
        assertEquals("The ID should be the same of the got asset as the one created originally",
                     createdAsset.get_id(), gotAsset.get_id());
        assertEquals("The returned asset name should match the supplied one",
                     newAsset.getName(), gotAsset.getName());

        List<Attachment> attachments = gotAsset.getAttachments();
        for (Attachment att : attachments) {
            InputStream is = userRoleMassiveClient.getAttachment(gotAsset.get_id(), att.get_id());
            assertNotNull("Unexpected null InputStream returned", is);
        }
    }

    /**
     * Add an asset and ensure it can be read back without being authenticated
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    @Ignore
    // needs auth and private assets (and hiding unpublished assets from unauth?)
    public void testUserGetAll() throws Exception {

        // define counters
        int publishedAssetsFound = 0;
        int nonPublishedAssetsFound = 0;

        // Check the number of assets left lying around when we start
        List<Asset> startingAssetList = massiveClient.getAllAssets();

        // count existing chaff in repository
        System.out.println("testUnauthGetAll: Existing assets in repository");
        for (Asset asset : startingAssetList) {
            // only concerned with non-private assets as unauthenticated does not return them
            if (asset.getPrivacy() != Privacy.PRIVATE) {
                if (asset.getState() == State.PUBLISHED) {
                    System.out.println("PUBLISHED   asset id=" + asset.get_id());
                    publishedAssetsFound--;
                } else {
                    System.out.println("UNPUBLISHED asset id=" + asset.get_id());
                    nonPublishedAssetsFound--;
                }
            }
        }
        System.out.println("testUnauthGetAll initialState: published assets=" + publishedAssetsFound + ", non published assets=" + nonPublishedAssetsFound);
        int originalPublishedAssetCount = publishedAssetsFound;

        // create 2 published and 1 draft asset
        Asset newAsset1 = createTestAsset();
        Asset createdAsset1 = massiveClient.addAsset(newAsset1);
        toDelete.add(createdAsset1.get_id());
        massiveClient.updateState(createdAsset1.get_id(), Action.PUBLISH);
        massiveClient.updateState(createdAsset1.get_id(), Action.APPROVE);
        System.out.println("Created PUBLISHED   asset id=" + createdAsset1.get_id());

        Asset newAsset2 = createTestAsset();
        Asset createdAsset2 = massiveClient.addAsset(newAsset2);
        toDelete.add(createdAsset2.get_id());
        massiveClient.updateState(createdAsset2.get_id(), Action.PUBLISH);
        massiveClient.updateState(createdAsset2.get_id(), Action.APPROVE);
        System.out.println("Created PUBLISHED   asset id=" + createdAsset2.get_id());

        Asset newAsset3 = createTestAsset();
        Asset createdAsset3 = massiveClient.addAsset(newAsset3);
        toDelete.add(createdAsset3.get_id());
        System.out.println("Created UNPUBLISHED asset id=" + createdAsset3.get_id());

        // testing the getAllAssets() method we EXPECT there to be just our
        // 2 published ones but could be others from tests that failed to
        // cleanup.
        List<Asset> assetList = userRoleMassiveClient.getAllAssets();
        for (Asset asset : assetList) {
            if (asset.getState() == State.PUBLISHED) {
                userRoleMassiveClient.getAsset(asset.get_id());
                System.out.println("Found PUBLISHED   asset, id=" + asset.get_id());
                publishedAssetsFound++;
            } else {
                fail("Managed to load an asset that was not published using get all on an unathenticated client: " + asset);
            }
        }
        // Check we found the correct number of published assets.
        // We've already accounted for any existing stuff in the repository so this should be only the two assets we published.
        if (publishedAssetsFound != 2) {
            System.out.println("testUnauthGetAll failureState: published assets=" + publishedAssetsFound + ", non published assets=" + nonPublishedAssetsFound);
            fail("NEW publishedAssetsFound=" + publishedAssetsFound + ", NEW nonPublishedAssetsFound=" + nonPublishedAssetsFound);
        }

        // Try again with the authenticated client, should be able to get everything
        assetList = massiveClient.getAllAssets();
        publishedAssetsFound = originalPublishedAssetCount;
        for (Asset asset : assetList) {
            // Restrict to non-private assets to match the counting at the start of the test
            if (asset.getPrivacy() != Privacy.PRIVATE) {
                if (asset.getState() == State.PUBLISHED) {
                    massiveClient.getAsset(asset.get_id());
                    System.out.println("Found PUBLISHED   asset, id="
                                       + asset.get_id());
                    publishedAssetsFound++;
                } else {
                    // Attempting to get non-published asset with
                    // authentication should be fine
                    massiveClient.getAsset(asset.get_id());
                    nonPublishedAssetsFound++;
                }
            }
        }

        // Check we found the correct number of published and unpublished assets
        // We've already accounted for any existing stuff in the repository so this should be only the two assets we published and the one we left in draft state.
        if (publishedAssetsFound != 2 || nonPublishedAssetsFound != 1) {
            System.out
                            .println("testUnauthGetAll failureState: published assets="
                                     + publishedAssetsFound
                                     + ", non published assets="
                                     + nonPublishedAssetsFound);
            fail("NEW publishedAssetsFound=" + publishedAssetsFound
                 + ", NEW nonPublishedAssetsFound="
                 + nonPublishedAssetsFound);
        }
    }

    /**
     * Test to make sure if you have a valid charset in the content type then it is returned.
     */
    @Test
    public void testGetCharsetValidCharset() {
        String utf16 = "utf-16";
        String contentType = "text/html; charset=" + utf16;
        String charset = MassiveClient.getCharset(contentType);
        assertEquals("We should have got the charset we entered", utf16, charset);
    }

    /**
     * Test to make sure if you have a valid charset in the content type then it is returned even if there are more properties
     */
    @Test
    public void testGetCharsetValidCharsetAndMultipleProperties() {
        String utf16 = "utf-16";
        String contentType = "text/html; charset=" + utf16 + " ; foo=bar";
        String charset = MassiveClient.getCharset(contentType);
        assertEquals("We should have got the charset we entered", utf16, charset);
    }

    /**
     * Test to make sure if you don't have a charset the default is used
     */
    @Test
    public void testGetCharsetDefault() {
        String contentType = "text/html";
        String charset = MassiveClient.getCharset(contentType);
        assertEquals("We should have got the default charset", "UTF-8", charset);
    }

    /**
     * Test to make sure if you have a invalid charset in the content type then the default is returned.
     */
    @Test
    public void testGetCharsetInvalidCharset() {
        String contentType = "text/html; charset=wibble";
        String charset = MassiveClient.getCharset(contentType);
        assertEquals("We should have got the default charset", "UTF-8", charset);
    }

    /**
     * Test that if we don't recognize the content then we throw an exception for a single get by ID
     *
     * @throws RequestFailureException
     * @throws IOException
     */
    @Test
    public void testVerificationSingleObject() throws IOException, RequestFailureException {
        Asset asset = new Asset();
        WlpInformation wlpInformation = new WlpInformation();
        String version = Float.toString(WlpInformation.MAX_VERSION + 1);
        wlpInformation.setWlpInformationVersion(version);
        asset.setWlpInformation(wlpInformation);
        try {
            Asset returnedAsset = massiveClient.addAsset(asset);
            String assetId = returnedAsset.get_id();
            massiveClient.getAsset(assetId);
            fail("Should have thrown a bad version exception");
        } catch (BadVersionException e) {
            // This is correct
            assertEquals("The version in the exception should match that set on the wlpInformation", version, e.getBadVersion());
            assertEquals("The min version in the exception should match that set on the wlpInformation", Float.toString(WlpInformation.MIN_VERSION), e.getMinVersion());
            assertEquals("The max version in the exception should match that set on the wlpInformation", Float.toString(WlpInformation.MAX_VERSION), e.getMaxVersion());
        }
    }

    /**
     * Test that if we don't recognize the content then we ignore it when doing a get all
     *
     * @throws RequestFailureException
     * @throws IOException
     * @throws BadVersionException
     */
    @Test
    public void testVerificationMultipleObjects() throws IOException, RequestFailureException, BadVersionException {
        Asset invalidAsset = new Asset();
        invalidAsset.setName("Invalid");
        WlpInformation invalidWlpInformation = new WlpInformation();
        invalidWlpInformation.setWlpInformationVersion(Float.toString(WlpInformation.MAX_VERSION + 1));
        invalidAsset.setWlpInformation(invalidWlpInformation);

        Asset validAsset = new Asset();
        validAsset.setName("Valid");
        WlpInformation validWlpInformation = new WlpInformation();
        validWlpInformation.setWlpInformationVersion(Float.toString(WlpInformation.MIN_VERSION));
        validAsset.setWlpInformation(validWlpInformation);
        try {
            massiveClient.addAsset(invalidAsset);
        } catch (BadVersionException e) {
            // This is expected as add tries to reload the asset and it is invalid
        }
        massiveClient.addAsset(validAsset);
        List<Asset> allAssets = massiveClient.getAllAssets();
        assertEquals("Expected to get just the single valid asset back", 1, allAssets.size());
        assertEquals("Expected to get just the valid asset back", "Valid", allAssets.get(0).getName());
    }

    @Test
    public void testSettingVisibilityToInstaller() throws IOException, BadVersionException, RequestFailureException {
        Asset asset = new Asset();
        WlpInformation wlpInformation = new WlpInformation();
        wlpInformation.setVisibility(Visibility.INSTALL);
        asset.setWlpInformation(wlpInformation);
        Asset returnedAsset = massiveClient.addAsset(asset);
        toDelete.add(returnedAsset.get_id());
        assertEquals("Expected the visibility to be read back correctly", Visibility.INSTALL, returnedAsset.getWlpInformation().getVisibility());
    }

    /**
     * Test you can filter by multiple types
     *
     * @throws RequestFailureException
     * @throws BadVersionException
     * @throws IOException
     */
    @Test
    public void testFilteringByTypes() throws IOException, BadVersionException, RequestFailureException {
        Asset feature = createTestAsset();
        feature.setType(Type.FEATURE);
        feature = massiveClient.addAsset(feature);
        toDelete.add(feature.get_id());

        Asset sample = createTestAsset();
        sample.setType(Type.PRODUCTSAMPLE);
        sample = massiveClient.addAsset(sample);
        toDelete.add(sample.get_id());

        Asset install = createTestAsset();
        install.setType(Type.INSTALL);
        install = massiveClient.addAsset(install);
        toDelete.add(install.get_id());

        Collection<Type> types = new HashSet<Asset.Type>();
        types.add(Type.FEATURE);
        types.add(Type.PRODUCTSAMPLE);
        Collection<Asset> assets = massiveClient.getAssets(types, null, null, null);
        assertEquals("Two assets should be obtained", 2, assets.size());
        assertTrue("Should get back the feature", assets.contains(feature));
        assertTrue("Should get back the feature", assets.contains(sample));
    }

    /**
     * Tests you can filter by min version
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     */
    @Test
    public void testFilteringByMinVersion() throws IOException, BadVersionException, RequestFailureException {
        Asset assetWithRightVersion = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        FilterVersion minVersion = new FilterVersion();
        minVersion.setValue("8.5.5.4");
        filterInfo.setMinVersion(minVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithRightVersion.setWlpInformation(wlpInfo);
        assetWithRightVersion = massiveClient.addAsset(assetWithRightVersion);
        toDelete.add(assetWithRightVersion.get_id());

        Asset assetWithNoVersion = createTestAsset();
        assetWithNoVersion = massiveClient.addAsset(assetWithNoVersion);
        toDelete.add(assetWithNoVersion.get_id());

        Collection<Asset> assets = massiveClient.getAssets(null, null, null, Collections.singleton("8.5.5.4"));
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(assetWithRightVersion));
    }

    /**
     * Tests you can filter by having no visibility
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     */
    @Test
    public void testFilteringByVisibility() throws IOException, BadVersionException, RequestFailureException {
        Asset assetWithRightVisibility = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        wlpInfo.setVisibility(Visibility.PUBLIC);
        assetWithRightVisibility.setWlpInformation(wlpInfo);
        assetWithRightVisibility = massiveClient.addAsset(assetWithRightVisibility);
        toDelete.add(assetWithRightVisibility.get_id());

        Asset assetWithWrongVisiblity = createTestAsset();
        WlpInformation wlpInfoWrongVisibility = new WlpInformation();
        wlpInfoWrongVisibility.setVisibility(Visibility.PRIVATE);
        assetWithWrongVisiblity.setWlpInformation(wlpInfoWrongVisibility);
        assetWithWrongVisiblity = massiveClient.addAsset(assetWithWrongVisiblity);
        toDelete.add(assetWithWrongVisiblity.get_id());

        Collection<Asset> assets = massiveClient.getAssets(null, null, Visibility.PUBLIC, null);
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(assetWithRightVisibility));
    }

    /**
     * Tests you can filter by having two product IDs
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     */
    @Test
    public void testFilteringByMultipleProductIds() throws IOException, BadVersionException, RequestFailureException {
        Asset assetWithRightProduct1 = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct1");
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithRightProduct1.setWlpInformation(wlpInfo);
        assetWithRightProduct1 = massiveClient.addAsset(assetWithRightProduct1);
        toDelete.add(assetWithRightProduct1.get_id());

        Asset assetWithRightProduct2 = createTestAsset();
        filterInfo.setProductId("correct2");
        assetWithRightProduct2.setWlpInformation(wlpInfo);
        assetWithRightProduct2 = massiveClient.addAsset(assetWithRightProduct2);
        toDelete.add(assetWithRightProduct2.get_id());

        Asset assetWithIncorrectId = createTestAsset();
        filterInfo.setProductId("incorrect");
        assetWithIncorrectId.setWlpInformation(wlpInfo);
        assetWithIncorrectId = massiveClient.addAsset(assetWithIncorrectId);
        toDelete.add(assetWithIncorrectId.get_id());

        Collection<String> rightProductIds = new HashSet<String>();
        rightProductIds.add("correct1");
        rightProductIds.add("correct2");
        Collection<Asset> assets = massiveClient.getAssets(null, rightProductIds, null, null);
        assertEquals("Two assets should be obtained", 2, assets.size());
        assertTrue("Should get back the feature with ID correct1", assets.contains(assetWithRightProduct1));
        assertTrue("Should get back the feature with ID correct2", assets.contains(assetWithRightProduct2));
    }

    /**
     * Tests you can filter by having two product IDs with different product versions
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     */
    @Test
    public void testFilteringByMultipleProductIdsAndVersions() throws IOException, BadVersionException, RequestFailureException {
        Asset assetWithRightProduct1 = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct1");
        FilterVersion minVersion = new FilterVersion();
        minVersion.setValue("8.5.5.4");
        filterInfo.setMinVersion(minVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithRightProduct1.setWlpInformation(wlpInfo);
        assetWithRightProduct1 = massiveClient.addAsset(assetWithRightProduct1);
        toDelete.add(assetWithRightProduct1.get_id());

        Asset assetWithRightProduct2 = createTestAsset();
        filterInfo.setProductId("correct2");
        minVersion.setValue("1.0.0.0");
        assetWithRightProduct2.setWlpInformation(wlpInfo);
        assetWithRightProduct2 = massiveClient.addAsset(assetWithRightProduct2);
        toDelete.add(assetWithRightProduct2.get_id());

        Asset assetWithRightProduct2ButWrongVersion = createTestAsset();
        filterInfo.setProductId("correct2");
        minVersion.setValue("2.0.0.0");
        assetWithRightProduct2ButWrongVersion.setWlpInformation(wlpInfo);
        assetWithRightProduct2ButWrongVersion = massiveClient.addAsset(assetWithRightProduct2ButWrongVersion);
        toDelete.add(assetWithRightProduct2ButWrongVersion.get_id());
        minVersion.setValue("1.0.0.0");

        Asset assetWithIncorrectId = createTestAsset();
        filterInfo.setProductId("incorrect");
        assetWithIncorrectId.setWlpInformation(wlpInfo);
        assetWithIncorrectId = massiveClient.addAsset(assetWithIncorrectId);
        toDelete.add(assetWithIncorrectId.get_id());

        Collection<String> rightProductIds = new HashSet<String>();
        rightProductIds.add("correct1");
        rightProductIds.add("correct2");
        Collection<String> rightVersions = new HashSet<String>();
        rightVersions.add("1.0.0.0");
        rightVersions.add("8.5.5.4");
        Collection<Asset> assets = massiveClient.getAssets(null, rightProductIds, null, rightVersions);
        assertEquals("Two assets should be obtained", 2, assets.size());
        assertTrue("Should get back the feature with ID correct1", assets.contains(assetWithRightProduct1));
        assertTrue("Should get back the feature with ID correct2", assets.contains(assetWithRightProduct2));
    }

    /**
     * Tests you can filter by having different fields set
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     */
    @Test
    public void testFilteringByMultipleFields() throws IOException, BadVersionException, RequestFailureException {
        Asset featureWithRightVisibilityAndVersion = createTestAsset();
        featureWithRightVisibilityAndVersion.setType(Type.FEATURE);
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct");
        FilterVersion minVersion = new FilterVersion();
        minVersion.setValue("8.5.5.4");
        filterInfo.setMinVersion(minVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        wlpInfo.setVisibility(Visibility.PUBLIC);
        featureWithRightVisibilityAndVersion.setWlpInformation(wlpInfo);
        featureWithRightVisibilityAndVersion = massiveClient.addAsset(featureWithRightVisibilityAndVersion);
        toDelete.add(featureWithRightVisibilityAndVersion.get_id());

        Asset featureWithWrongVisiblity = createTestAsset();
        featureWithWrongVisiblity.setType(Type.FEATURE);
        wlpInfo.setVisibility(Visibility.PRIVATE);
        featureWithWrongVisiblity.setWlpInformation(wlpInfo);
        featureWithWrongVisiblity = massiveClient.addAsset(featureWithWrongVisiblity);
        toDelete.add(featureWithWrongVisiblity.get_id());
        wlpInfo.setVisibility(Visibility.PUBLIC);

        Asset featureWithWrongVersion = createTestAsset();
        featureWithWrongVersion.setType(Type.FEATURE);
        minVersion.setValue("8.5.5.0");
        featureWithWrongVersion.setWlpInformation(wlpInfo);
        featureWithWrongVersion = massiveClient.addAsset(featureWithWrongVersion);
        toDelete.add(featureWithWrongVersion.get_id());
        minVersion.setValue("8.5.5.4");

        Asset featureWithWrongProduct = createTestAsset();
        featureWithWrongVersion.setType(Type.FEATURE);
        filterInfo.setProductId("incorrect");
        featureWithWrongProduct.setWlpInformation(wlpInfo);
        featureWithWrongProduct = massiveClient.addAsset(featureWithWrongProduct);
        toDelete.add(featureWithWrongProduct.get_id());

        Asset install = createTestAsset();
        install.setType(Type.INSTALL);
        install = massiveClient.addAsset(install);
        toDelete.add(install.get_id());

        Collection<Type> types = new HashSet<Asset.Type>();
        types.add(Type.FEATURE);
        Collection<Asset> assets = massiveClient.getAssets(types, Collections.singleton("correct"), Visibility.PUBLIC, Collections.singleton("8.5.5.4"));
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(featureWithRightVisibilityAndVersion));
    }

    /**
     * Tests that you can filter for an asset with no max version
     *
     * @throws RequestFailureException
     * @throws BadVersionException
     * @throws IOException
     */
    @Test
    public void testFilteringForNoMaxVersion() throws IOException, BadVersionException, RequestFailureException {
        Asset assetWithMaxVersion = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        FilterVersion maxVersion = new FilterVersion();
        maxVersion.setValue("8.5.5.4");
        filterInfo.setMaxVersion(maxVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithMaxVersion.setWlpInformation(wlpInfo);
        assetWithMaxVersion = massiveClient.addAsset(assetWithMaxVersion);
        toDelete.add(assetWithMaxVersion.get_id());

        Asset assetWithNoMaxVersion = createTestAsset();
        filterInfo.setMaxVersion(null);
        assetWithNoMaxVersion.setWlpInformation(wlpInfo);
        assetWithNoMaxVersion = massiveClient.addAsset(assetWithNoMaxVersion);
        toDelete.add(assetWithNoMaxVersion.get_id());

        Collection<Asset> assets = massiveClient.getAssetsWithUnboundedMaxVersion(null, null, null);
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(assetWithNoMaxVersion));
    }

    /**
     * Tests that you can filter for an asset with no max version
     *
     * @throws RequestFailureException
     * @throws BadVersionException
     * @throws IOException
     */
    @Test
    public void testFilteringForNoMaxVersionWithProductId() throws IOException, BadVersionException, RequestFailureException {
        Asset assetWithMaxVersion = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct");
        FilterVersion maxVersion = new FilterVersion();
        maxVersion.setValue("8.5.5.4");
        filterInfo.setMaxVersion(maxVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithMaxVersion.setWlpInformation(wlpInfo);
        assetWithMaxVersion = massiveClient.addAsset(assetWithMaxVersion);
        toDelete.add(assetWithMaxVersion.get_id());

        Asset assetWithNoMaxVersion = createTestAsset();
        filterInfo.setMaxVersion(null);
        assetWithNoMaxVersion.setWlpInformation(wlpInfo);
        assetWithNoMaxVersion = massiveClient.addAsset(assetWithNoMaxVersion);
        toDelete.add(assetWithNoMaxVersion.get_id());

        Asset assetWithNoMaxVersionWrongProduct = createTestAsset();
        filterInfo.setMaxVersion(null);
        filterInfo.setProductId("incorrect");
        assetWithNoMaxVersionWrongProduct.setWlpInformation(wlpInfo);
        assetWithNoMaxVersionWrongProduct = massiveClient.addAsset(assetWithNoMaxVersionWrongProduct);
        toDelete.add(assetWithNoMaxVersionWrongProduct.get_id());

        Collection<Asset> assets = massiveClient.getAssetsWithUnboundedMaxVersion(null, Collections.singleton("correct"), null);
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(assetWithNoMaxVersion));
    }

    /**
     * Tests you can filter by having two product IDs
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     */
    @Test
    public void testFilteringByMultipleProductIdsAndNoMaxVersion() throws IOException, BadVersionException, RequestFailureException {
        Asset assetWithRightProduct1 = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct1");
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithRightProduct1.setWlpInformation(wlpInfo);
        assetWithRightProduct1 = massiveClient.addAsset(assetWithRightProduct1);
        toDelete.add(assetWithRightProduct1.get_id());

        Asset assetWithRightProduct2 = createTestAsset();
        filterInfo.setProductId("correct2");
        assetWithRightProduct2.setWlpInformation(wlpInfo);
        assetWithRightProduct2 = massiveClient.addAsset(assetWithRightProduct2);
        toDelete.add(assetWithRightProduct2.get_id());

        Asset assetWithIncorrectId = createTestAsset();
        filterInfo.setProductId("incorrect");
        assetWithIncorrectId.setWlpInformation(wlpInfo);
        assetWithIncorrectId = massiveClient.addAsset(assetWithIncorrectId);
        toDelete.add(assetWithIncorrectId.get_id());

        Collection<String> rightProductIds = new HashSet<String>();
        rightProductIds.add("correct1");
        rightProductIds.add("correct2");
        Collection<Asset> assets = massiveClient.getAssetsWithUnboundedMaxVersion(null, rightProductIds, null);
        assertEquals("Two assets should be obtained", 2, assets.size());
        assertTrue("Should get back the feature with ID correct1", assets.contains(assetWithRightProduct1));
        assertTrue("Should get back the feature with ID correct2", assets.contains(assetWithRightProduct2));
    }

    /**
     * Tests that you can filter for an asset with no max version, type and visibility
     *
     * @throws RequestFailureException
     * @throws BadVersionException
     * @throws IOException
     */
    @Test
    public void testFilteringForNoMaxVersionTypeAndVisibility() throws IOException, BadVersionException, RequestFailureException {
        Asset featureWithMaxVersion = createTestAsset();
        featureWithMaxVersion.setType(Type.FEATURE);
        WlpInformation wlpInfo = new WlpInformation();
        wlpInfo.setVisibility(Visibility.PUBLIC);
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct");
        FilterVersion maxVersion = new FilterVersion();
        maxVersion.setValue("8.5.5.4");
        filterInfo.setMaxVersion(maxVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        featureWithMaxVersion.setWlpInformation(wlpInfo);
        featureWithMaxVersion = massiveClient.addAsset(featureWithMaxVersion);
        toDelete.add(featureWithMaxVersion.get_id());

        Asset featureWithNoMaxVersion = createTestAsset();
        featureWithNoMaxVersion.setType(Type.FEATURE);
        filterInfo.setMaxVersion(null);
        featureWithNoMaxVersion.setWlpInformation(wlpInfo);
        featureWithNoMaxVersion = massiveClient.addAsset(featureWithNoMaxVersion);
        toDelete.add(featureWithNoMaxVersion.get_id());

        Asset featureWithNoMaxVersionWrongVisibility = createTestAsset();
        featureWithNoMaxVersionWrongVisibility.setType(Type.FEATURE);
        wlpInfo.setVisibility(Visibility.PRIVATE);
        featureWithNoMaxVersionWrongVisibility.setWlpInformation(wlpInfo);
        featureWithNoMaxVersionWrongVisibility = massiveClient.addAsset(featureWithNoMaxVersionWrongVisibility);
        toDelete.add(featureWithNoMaxVersionWrongVisibility.get_id());

        Asset assetWithNoMaxVersionWrongProduct = createTestAsset();
        filterInfo.setMaxVersion(null);
        filterInfo.setProductId("incorrect");
        assetWithNoMaxVersionWrongProduct.setWlpInformation(wlpInfo);
        assetWithNoMaxVersionWrongProduct = massiveClient.addAsset(assetWithNoMaxVersionWrongProduct);
        toDelete.add(assetWithNoMaxVersionWrongProduct.get_id());

        Asset install = createTestAsset();
        install.setType(Type.INSTALL);
        install = massiveClient.addAsset(install);
        toDelete.add(install.get_id());

        Collection<Type> types = new HashSet<Asset.Type>();
        types.add(Type.FEATURE);
        Collection<Asset> assets = massiveClient.getAssetsWithUnboundedMaxVersion(types, Collections.singleton("correct"), Visibility.PUBLIC);
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(featureWithNoMaxVersion));
    }

    /**
     * Test for the {@link MassiveClient#getFilteredAssets(Map)} method.
     *
     * @throws RequestFailureException
     * @throws IOException
     * @throws BadVersionException
     */
    @Test
    public void testGetFilteredAssets() throws IOException, RequestFailureException, BadVersionException {
        // Create an assets that has all of the filterable attributes set
        Asset filterableAsset = createTestAsset();
        filterableAsset.setType(Type.FEATURE);
        WlpInformation wlpInfo = new WlpInformation();
        filterableAsset.setWlpInformation(wlpInfo);
        wlpInfo.setVisibility(Visibility.PUBLIC);
        wlpInfo.addProvideFeature("feature1");
        wlpInfo.setShortName("IsThisShort?");
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("a.product");
        FilterVersion filterVersion = new FilterVersion();
        filterVersion.setValue("1");
        filterInfo.setMinVersion(filterVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        filterableAsset = massiveClient.addAsset(filterableAsset);
        String assetId = filterableAsset.get_id();
        toDelete.add(assetId);

        // Now try to filter on each attribute both in positive and negative way
        // TYPE
        Map<FilterableAttribute, Collection<String>> filters = new HashMap<MassiveClient.FilterableAttribute, Collection<String>>();
        filters.put(FilterableAttribute.TYPE, Collections.singleton(Type.FEATURE.getValue()));
        Collection<Asset> filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.TYPE, Collections.singleton(Type.ADDON.getValue()));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // PRODUCT ID
        filters.clear();
        // Try a collection of strings
        Collection<String> productIdValues = new HashSet<String>();
        productIdValues.add("a.product");
        productIdValues.add("another.product");
        filters.put(FilterableAttribute.PRODUCT_ID, productIdValues);
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.PRODUCT_ID, Collections.singleton("Wibble"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // VISIBILITY
        filters.clear();
        filters.put(FilterableAttribute.VISIBILITY, Collections.singleton(Visibility.PUBLIC.toString()));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.VISIBILITY, Collections.singleton(Visibility.INSTALL.toString()));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // PRODUCT MIN VERSION
        filters.clear();
        filters.put(FilterableAttribute.PRODUCT_MIN_VERSION, Collections.singleton("1"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.PRODUCT_MIN_VERSION, Collections.singleton("2"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // PRODUCT HAS MAX VERSION
        filters.clear();
        filters.put(FilterableAttribute.PRODUCT_HAS_MAX_VERSION, Collections.singleton("false"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.PRODUCT_HAS_MAX_VERSION, Collections.singleton("true"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // SYMBOLIC NAME
        filters.clear();
        filters.put(FilterableAttribute.SYMBOLIC_NAME, Collections.singleton("feature1"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.SYMBOLIC_NAME, Collections.singleton("wibble"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // SHORT NAME
        filters.clear();
        filters.put(FilterableAttribute.SHORT_NAME, Collections.singleton("IsThisShort?"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.SHORT_NAME, Collections.singleton("Wibble"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // LOWER CASE SHORT NAME
        filters.clear();
        filters.put(FilterableAttribute.LOWER_CASE_SHORT_NAME, Collections.singleton("isthisshort?"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.LOWER_CASE_SHORT_NAME, Collections.singleton("Wibble"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // Finally make sure passing in null is ok
        filters.clear();
        filters.put(FilterableAttribute.TYPE, null);
        assertFalse("Should get back some assets", massiveClient.getFilteredAssets(filters).isEmpty());
        filters.put(FilterableAttribute.TYPE, Collections.<String> emptySet());
        assertFalse("Should get back some assets", massiveClient.getFilteredAssets(filters).isEmpty());
    }

    /**
     * Try to filter on visibility private and install, this isn't valid as the values are stored in two different objects.
     *
     * @throws RequestFailureException
     * @throws IOException
     */
    @Test
    public void testInvalidFilter() throws IOException, RequestFailureException {
        Map<FilterableAttribute, Collection<String>> filters = new HashMap<MassiveClient.FilterableAttribute, Collection<String>>();
        Collection<String> visibilityValues = new HashSet<String>();
        visibilityValues.add(Visibility.PRIVATE.toString());
        visibilityValues.add(Visibility.INSTALL.toString());
        filters.put(FilterableAttribute.VISIBILITY, visibilityValues);
        try {
            massiveClient.getFilteredAssets(filters);
            fail("Should not be able to filter on private and install visibilities at the same time");
        } catch (IllegalArgumentException e) {

        }
    }

    /**
     * Test to make sure a filter value can include an &
     *
     * @throws RequestFailureException
     * @throws IOException
     * @throws BadVersionException
     */
    @Test
    public void testFilterWithAmpersand() throws IOException, RequestFailureException, BadVersionException {
        Asset filterableAsset = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        filterableAsset.setWlpInformation(wlpInfo);
        wlpInfo.addProvideFeature("feature&1");
        filterableAsset = massiveClient.addAsset(filterableAsset);
        String assetId = filterableAsset.get_id();
        toDelete.add(assetId);

        Map<FilterableAttribute, Collection<String>> filters = new HashMap<MassiveClient.FilterableAttribute, Collection<String>>();
        filters.put(FilterableAttribute.SYMBOLIC_NAME, Collections.singleton("feature&1"));
        Collection<Asset> filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.SYMBOLIC_NAME, Collections.singleton("wibble"));
        filteredAssets = massiveClient.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

    }
}
