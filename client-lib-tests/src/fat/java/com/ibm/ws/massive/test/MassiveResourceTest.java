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

package com.ibm.ws.massive.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.BasicChecks;
import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.RepositoryFixture;
import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.RepositoryBackendRequestFailureException;
import com.ibm.ws.massive.RepositoryException;
import com.ibm.ws.massive.resources.EsaResource;
import com.ibm.ws.massive.resources.EsaResource.InstallPolicy;
import com.ibm.ws.massive.resources.IfixResource;
import com.ibm.ws.massive.resources.MassiveResource;
import com.ibm.ws.massive.resources.MassiveResource.AttachmentLinkType;
import com.ibm.ws.massive.resources.MassiveResource.AttachmentResource;
import com.ibm.ws.massive.resources.MassiveResource.AttachmentType;
import com.ibm.ws.massive.resources.MassiveResource.DisplayPolicy;
import com.ibm.ws.massive.resources.MassiveResource.DownloadPolicy;
import com.ibm.ws.massive.resources.MassiveResource.State;
import com.ibm.ws.massive.resources.MassiveResource.Type;
import com.ibm.ws.massive.resources.MassiveResource.TypeLabel;
import com.ibm.ws.massive.resources.MassiveResource.Visibility;
import com.ibm.ws.massive.resources.ProductDefinition;
import com.ibm.ws.massive.resources.ProductRelatedResource;
import com.ibm.ws.massive.resources.ProductResource;
import com.ibm.ws.massive.resources.Provider;
import com.ibm.ws.massive.resources.RepositoryResourceException;
import com.ibm.ws.massive.resources.SampleResource;
import com.ibm.ws.massive.resources.SimpleProductDefinition;
import com.ibm.ws.massive.resources.UpdateInPlaceStrategy;
import com.ibm.ws.massive.resources.UpdateType;
import com.ibm.ws.massive.sa.client.model.Asset;
import com.ibm.ws.massive.sa.client.model.WlpInformation;

public class MassiveResourceTest {

    private static final String PROVIDER_NAME = "Test Provider";

    private final static File resourceDir = new File("resources");

    @Rule
    public RepositoryFixture repo = FatUtils.FAT_REPO;
    private final LoginInfoEntry _loginInfoEntry = repo.getLoginInfoEntry();
    private final TestResource _testRes = new TestResource(repo.getLoginInfoEntry());

    public MassiveResourceTest() throws FileNotFoundException, IOException {
        super();
    }

    private void populateResource(MassiveResource res) {
        BasicChecks.populateResource(res);
        res.getProvider().setName(PROVIDER_NAME);
    }

    private void uploadResource(MassiveResource res) throws RepositoryBackendException, RepositoryResourceException {
        res.uploadToMassive(new UpdateInPlaceStrategy());
    }

    private void updateResource(MassiveResource res) throws RepositoryBackendException, RepositoryResourceException {
        res.uploadToMassive(new UpdateInPlaceStrategy());
    }

    private SampleResource createSampleResource() {
        SampleResource sampleRes = new SampleResource(_loginInfoEntry);
        populateResource(sampleRes);
        sampleRes.setType(Type.PRODUCTSAMPLE);
        return sampleRes;
    }

    /**
     * This test checks that a resource can be added to massive and that
     * the asset inside the resource is equivalent before and after the
     * upload (equivalent doesn't compare things like ID, uploadedOn etc).
     * It also makes sure that the number of resources in massive inceases
     * by one once we have done the add.
     */
    @Test
    public void testCreateResource() throws SecurityException,
                    NoSuchFieldException, RepositoryException, URISyntaxException {

        populateResource(_testRes);
        Asset beforeAss = _testRes.getAsset();
        int numAssets = MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry)).size();
        uploadResource(_testRes);
        Asset afterAss = _testRes.getAsset();
        assertTrue("The asset was changed after uploading before=<" + beforeAss
                   + "> after=<" + afterAss + ">", beforeAss.equivalent(afterAss));
        assertNull(
                   "The original asset was updated with an ID when only the returned one should have an id ",
                   beforeAss.get_id());
        assertNotNull("An ID was not assigned to the asset after uploading ",
                      afterAss.get_id());
        int newNumAssets = MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry)).size();
        assertEquals("Unexpected number of assets found after adding an asset",
                     numAssets + 1, newNumAssets);
    }

    /**
     * This test will check that the creation of resources from the
     * MassiveResource.Type enum creates a resource of the correct type
     */
    @Test
    public void testCreateType() {
        LoginInfoEntry loginInfo = null;
        // Null would fail an instanceof check so this checks for null as well
        assertTrue(MassiveResource.Type.ADDON.createResource(loginInfo) instanceof ProductResource);
        assertTrue(MassiveResource.Type.FEATURE.createResource(loginInfo) instanceof EsaResource);
        assertTrue(MassiveResource.Type.IFIX.createResource(loginInfo) instanceof IfixResource);
        assertTrue(MassiveResource.Type.INSTALL.createResource(loginInfo) instanceof ProductResource);
        assertTrue(MassiveResource.Type.OPENSOURCE.createResource(loginInfo) instanceof SampleResource);
        assertTrue(MassiveResource.Type.PRODUCTSAMPLE.createResource(loginInfo) instanceof SampleResource);
    }

    /**
     * Tests the Visibility enum maps onto the correct base enum. See {@link #runEnumTest} for how
     */
    @Test
    public void testVisibilityEnum() throws SecurityException,
                    NoSuchMethodException, IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException {
        Method m1 = Visibility.class.getDeclaredMethod("lookup",
                                                       WlpInformation.Visibility.class);
        Method m2 = Visibility.class.getDeclaredMethod("getWlpVisibility");
        runEnumTest(m1, m2, Visibility.values(), WlpInformation.Visibility
                        .values());
    }

    /**
     * Tests the State enum maps onto the correct base enum. See {@link #runEnumTest} for how
     */
    @Test
    public void testStateEnum() throws SecurityException,
                    NoSuchMethodException, IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException {
        Method m1 = State.class.getDeclaredMethod("lookup", Asset.State.class);
        Method m2 = State.class.getDeclaredMethod("getAssetState");
        runEnumTest(m1, m2, State.values(), Asset.State.values());
    }

    @Test
    public void testDisplayPolicyEnum() throws SecurityException,
                    NoSuchMethodException, IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException {
        Method m1 = DisplayPolicy.class.getDeclaredMethod("lookup", WlpInformation.DisplayPolicy.class);
        Method m2 = DisplayPolicy.class.getDeclaredMethod("getWlpDisplayPolicy");
        runEnumTest(m1, m2, DisplayPolicy.values(), WlpInformation.DisplayPolicy.values());
    }

    @Test
    public void testTypeLableEnum() throws SecurityException,
                    NoSuchMethodException, IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException {
        Method m1 = TypeLabel.class.getDeclaredMethod("lookup", WlpInformation.TypeLabel.class);
        Method m2 = TypeLabel.class.getDeclaredMethod("getWlpTypeLabel");
        runEnumTest(m1, m2, TypeLabel.values(), WlpInformation.TypeLabel.values());
    }

    @Test
    public void testDownloadPolicyEnum() throws SecurityException,
                    NoSuchMethodException, IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException {
        Method m1 = DownloadPolicy.class.getDeclaredMethod("lookup", WlpInformation.DownloadPolicy.class);
        Method m2 = DownloadPolicy.class.getDeclaredMethod("getWlpDownloadPolicy");
        runEnumTest(m1, m2, DownloadPolicy.values(), WlpInformation.DownloadPolicy.values());
    }

    @Test
    public void testInstallPolicyEnum() throws SecurityException,
                    NoSuchMethodException, IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException {
        Method m1 = InstallPolicy.class.getDeclaredMethod("lookup", WlpInformation.InstallPolicy.class);
        Method m2 = InstallPolicy.class.getDeclaredMethod("getWlpInstallPolicy");
        runEnumTest(m1, m2, InstallPolicy.values(), WlpInformation.InstallPolicy.values());
    }

    /**
     * Tests the Type enum maps onto the correct base enum. See {@link #runEnumTest} for how
     */
    @Test
    public void testTypeEnum() throws SecurityException, NoSuchMethodException,
                    IllegalArgumentException, IllegalAccessException,
                    InvocationTargetException {
        Method m1 = Type.class.getDeclaredMethod("lookup",
                                                 Asset.Type.class);
        Method m2 = Type.class.getDeclaredMethod("getAssetType");
        runEnumTest(m1, m2, Type.values(), Asset.Type.values());
    }

    /**
     * This method checks that we can upload then read back an asset from massive and
     * that the resource read from massive is equivalent to the one we uploaded
     */
    @Test
    public void testGetAssetBackFromMassive() throws
                    RepositoryException, SecurityException, IllegalArgumentException, NoSuchMethodException,
                    IllegalAccessException, InvocationTargetException, URISyntaxException {
        SampleResource sampleRes = createSampleResource();
        uploadResource(sampleRes);
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, sampleRes.get_id());
        assertTrue("Resource was different from the one uploaded", sampleRes.equivalent(readBack));
    }

    /**
     * Checks that we equivalent method says that resources are equivalent that
     * have different fields that we are not interested in. This is achieved by creating
     * a resource (which has fields we care about in) then uploading the resource.
     * We then get the resource back from massive which will now contain fields
     * that we don't care about and should not effect the equivalent method.
     *
     * We then make an update to a field we do care about. We then ensure that this
     * resource is not equivalent to the one we read back from massive. We then
     * upload this resource and re-check this hasn't changed the fact that this resource
     * should not be equivalent to the one we read back from massive.
     *
     * We then read back the resource from massive again and at this point the two
     * resources should be equivalent again.
     */
    @Test
    @Ignore
    // needs update assets
    public void testResourceEquivalent() throws URISyntaxException, RepositoryException {
        SampleResource sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addLicense(attachment2, Locale.US);
        uploadResource(sampleRes);

        String id = sampleRes.get_id();

        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertTrue("The resource uploaded is not equivalent to the one extracted from massive",
                   sampleRes.equivalent(readBack));

        sampleRes.setDescription("new description");
        assertFalse("The resource has been updated and shouldn't be equivalent to the one obtained from massive",
                    sampleRes.equivalent(readBack));

        updateResource(sampleRes);

        assertFalse("The resource has been updated and shouldn't be equivalent to the one obtained from massive",
                    sampleRes.equivalent(readBack));

        readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertTrue("The resource uploaded is not equivalent to the one extracted from massive",
                   sampleRes.equivalent(readBack));
    }

    /**
     * Tests that we can upload a resource into massive and that the find matching resource returns a
     * matching one when it should
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testFindMatchingResource() throws SecurityException, IllegalArgumentException,
                    NoSuchMethodException, IllegalAccessException, InvocationTargetException, RepositoryException,
                    IOException, URISyntaxException {
        // Add a resource
        SampleResource sampleRes = createSampleResource();

        List<MassiveResource> resFromMassive = sampleRes.findMatchingResource();
        assertTrue("A matching resource was found when there should not have been one",
                   resFromMassive.isEmpty());

        uploadResource(sampleRes);

        resFromMassive = sampleRes.findMatchingResource();

        assertEquals("No matching resource was found when we expected to find one",
                     1, resFromMassive.size());
        assertTrue("The matching resource was not as expected", sampleRes.equivalent(resFromMassive.get(0)));
    }

    /**
     * Tests that we can upload a resource, then using the same resource object
     * we can update the fields in the resource and update it in massive.
     * Check that the resource we get back is equivalent to the updated resource we
     * uploaded.
     */
    @Test
    @Ignore
    // needs update assets
    public void testUpdateResource() throws IOException, RepositoryException, SecurityException,
                    IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException, URISyntaxException {
        SampleResource sampleRes = createSampleResource();
        uploadResource(sampleRes);

        sampleRes.setDescription("Updated description");
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        // Read the resource back from massive
        String id = sampleRes.get_id();
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);

        assertTrue("The updated resource was not the same as the one found in massive",
                   sampleRes.equivalent(readBack));

    }

    /**
     * This tests is similiar to testUpdateResource but this time we update fields
     * inside the wlpinfo inner fields.
     */
    @Test
    @Ignore
    // needs update assets
    public void testUpdateWlpInfoInResource() throws IOException, RepositoryException, SecurityException,
                    IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException, URISyntaxException {
        SampleResource sampleRes = createSampleResource();
        sampleRes.setAppliesTo("old applies to");
        uploadResource(sampleRes);

        sampleRes.setAppliesTo("new applies to");
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        // Read the resource back from massive
        String id = sampleRes.get_id();
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);

        assertTrue("The updated resource was not the same as the one found in massive",
                   sampleRes.equivalent(readBack));

    }

    /**
     * This test is like testUpdateWlpInfoInResource except that we create a brand new
     * resource object to update with instead of re-using the resource object we used for
     * doing the initial add. This checks that we don't hit any partial-update problems
     */
    @Test
    @Ignore
    // needs update assets
    public void testUpdateWlpInfo() throws IOException, RepositoryException, SecurityException,
                    IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException, URISyntaxException {
        SampleResource res1 = new SampleResource(_loginInfoEntry);
        res1.setType(Type.OPENSOURCE);
        res1.setAppliesTo("testAppliesTo1");
        populateResource(res1);

        uploadResource(res1);
        String id1 = res1.get_id();

        SampleResource res2 = new SampleResource(_loginInfoEntry);
        populateResource(res2);
        res2.setType(Type.OPENSOURCE);
        res2.setDescription("desc changed");
        res2.setAppliesTo("testAppliesTo1");

        res2.uploadToMassive(new UpdateInPlaceStrategy());

        // Read the resource back from massive
        String id2 = res2.get_id();

        assertEquals("This was an update, the ids should be the same", id1, id2);
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id1);

        assertFalse("The updated resource was the same as the one found in massive",
                    res1.equivalent(readBack));
        assertTrue("The updated resource was not the same as the one found in massive",
                   res2.equivalent(readBack));

    }

    /**
     * This test tests the refreshFromMassive call. A resource is uploaded and read back.
     * The resource is then updated, we check that the updated resource is not equivalent to
     * the updated resource. We then refresh the resource we read back - this should make
     * the resource equivalent to the updated one.
     */
    @Test
    @Ignore
    // needs update assets
    public void testRefreshData() throws IOException, RepositoryException, SecurityException,
                    IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException, URISyntaxException {
        // Add a resource
        SampleResource sampleRes = createSampleResource();
        uploadResource(sampleRes);

        // Read the resource back from massive
        String id = sampleRes.get_id();
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);

        // Update resource
        sampleRes.setDescription("Updated description");
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        // Check that the original resource has not been updated
        assertFalse("The resource found in massive was equal to the pre-updated resource " +
                    " when it should have been updated",
                    sampleRes.equivalent(readBack));

        // refresh to get latest data
        readBack.refreshFromMassive();

        // Now confirm the 2 resources match
        assertTrue("The resource found in massive was not the same as the updated resource " +
                   " when it should have been updated",
                   sampleRes.equivalent(readBack));
    }

    /**
     * Tests
     * - What happens when we delete something that doesn't exist
     * - Add resource
     * - delete resource
     * - ensure the resource is no longer in massive
     *
     * @throws URISyntaxException
     */
    @Test
    public void testDelete() throws IOException, RepositoryException, URISyntaxException {
        populateResource(_testRes);
        uploadResource(_testRes);
        String id = _testRes.get_id();
        _testRes.delete();
        try {
            MassiveResource.getResource(_loginInfoEntry, id);
            fail("Should not of been able to get an asset that did not exist");
        } catch (RepositoryException e) {
            // pass
        }
    }

    /**
     * Add an attachment and ensure we can get an input stream to it. The attachment
     * is added to the resource before the resource is uploaded
     *
     * @throws URISyntaxException
     */
    @Test
    public void testAddAttachmentBeforeUploading() throws IOException, RepositoryException, URISyntaxException {
        SampleResource sampleRes = createSampleResource();
        File attachment = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment);
        uploadResource(sampleRes);
        String id = sampleRes.get_id();
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertEquals("We only expected 1 attachment", 1, readBack.getAttachmentCount());
        assertNotNull("No attachment was found", readBack.getAttachment(attachment.getName()));
    }

    /**
     * Add an attachment and ensure we can get an input stream to it. The resource is created
     * is created then the attachment is added to the resource and re-uploaded to massive, this
     * should update the asset in massive to include the attachment. We make sure we can get
     * an input stream to the attachment and that there is exactly one attachment associated
     * with the resource.
     *
     * @throws URISyntaxException
     */
    @Test
    @Ignore
    // needs update assets
    public void testAddAttachmentAfterUploading() throws IOException, RepositoryException, URISyntaxException {
        SampleResource sampleRes = createSampleResource();
        uploadResource(sampleRes);
        File attachment = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment);
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());
        String id = sampleRes.get_id();
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertEquals("We only expected 1 attachment", 1, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment was found", readBack.getAttachment(attachment.getName()));
    }

    @Test
    @Ignore
    // http hosting
    public void testWeCanHostAttachmentsOutsideMassive() throws Exception
    {
        SampleResource sampleRes = createSampleResource();
        uploadResource(sampleRes);
        String sampleURL = "http://external.url"; //TODO: get this working in fat tests
        sampleRes.addAttachment(new File(resourceDir, "NotAZip.txt"),
                                AttachmentType.CONTENT, "NotAZip.txt", sampleURL, null);
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());
        String id = sampleRes.get_id();
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertEquals("We expected 1 attachment", 1, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment was found", readBack.getAttachment("NotAZip.txt"));
        AttachmentResource att = readBack.getAttachment("NotAZip.txt");
        InputStream is = att.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        char[] buffer = new char[1024];
        StringBuilder sb = new StringBuilder();
        while ((isr.read(buffer)) != -1) {
            sb.append(buffer);
        }
        assertTrue(sb.toString().contains("Just in case you weren't sure, this is not a zip file."));
        assertFalse(sb.toString().contains("IF YOU SEE THIS TEST FAILED"));
        assertEquals("The link type should default to direct", AttachmentLinkType.DIRECT, att.getLinkType());
    }

    /**
     * This test checks that we the updateRequired methods return the correct values.
     * A resource is created, we confirm that updateRequired returns ADD at this point.
     * We then upload the resource into massive and add an attachment. At this point
     * the asset itself should not need updating (returns NOTHING), but the attachment
     * itself should return ADD when asked if an update is required.
     * Next we modify the resource to use a different attachment (using the same attachment
     * name as the original name). This should cause the asset to say that NOTHING needs upading
     * but the attachment should say that an UPDATE is required.
     * Lastly we upload the resource and ensure that, with no further changes to the resource,
     * that both the asset and the attachment say that nothing needs updating
     */
    @Test
    @Ignore
    // needs update assets
    public void testUpdateAttachmentRequired() throws RepositoryException, IOException, URISyntaxException {
        SampleResource sampleRes = createSampleResource();
        List<MassiveResource> matching = sampleRes.findMatchingResource();
        assertEquals("Attachment had been uploaded, isUpdateRequired should have returned ADD",
                     UpdateType.ADD, sampleRes.updateRequired(getFirst(matching)));
        uploadResource(sampleRes);
        File attachment = new File(resourceDir, "TestAttachment.txt");
        matching = sampleRes.findMatchingResource();
        assertEquals("Nothing has changed on the resource so isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));

        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "documentationAttachment");
        matching = sampleRes.findMatchingResource();

        // The resource should not need updating after an attachment is added
        assertEquals("Attachment was added, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        // ... but the attachment does need adding
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleRes.getAttachment("documentationAttachment").updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        sampleRes.addContent(attachment);
        matching = sampleRes.findMatchingResource();

        // The resource will need updating when content is added
        assertEquals("Content was added so the file size should have changed and isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleRes.updateRequired(getFirst(matching)));
        // ... and the attachment does need adding
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleRes.getAttachment("TestAttachment.txt").updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        attachment = new File(resourceDir, "TestAttachment2.txt");
        sampleRes.addAttachment(attachment, AttachmentType.CONTENT, "TestAttachment.txt");
        matching = sampleRes.findMatchingResource();
        // The resource should not need updating
        assertEquals("Attachment was changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        // ... but the attachment does need updating
        assertEquals("Attachment was added, isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleRes.getAttachment("TestAttachment.txt").updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        matching = sampleRes.findMatchingResource();
        assertEquals("nothing has changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        assertEquals("nothing has changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.getAttachment("TestAttachment.txt").updateRequired(getFirst(matching)));
    }

    private MassiveResource getFirst(List<MassiveResource> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    @Test
    public void testUpdateURLInAttachment() throws RepositoryException, IOException, URISyntaxException {
        SampleResource sampleRes = createSampleResource();
        File attachment = new File(resourceDir, "TestAttachment.txt");

        // No url specified yet
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "useDefaultURL");
        List<MassiveResource> matching = sampleRes.findMatchingResource();
        assertEquals("Asset had been uploaded, isUpdateRequired should have returned ADD",
                     UpdateType.ADD, sampleRes.updateRequired(getFirst(matching)));
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleRes.getAttachment("useDefaultURL").updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        // Still no url specified yet - so this should match - we aren't changing the attachment at all
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "useDefaultURL");
        matching = sampleRes.findMatchingResource();
        assertEquals("Asset has not changed, isUpdateRequired should have returned NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        assertEquals("Attachment has not changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.getAttachment("useDefaultURL").updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        // Now go back to using the massive provided URLs - we can't do this....yet....
//		sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "useDefaultURL");
//		matching = sampleRes.findMatchingResource();
//		assertEquals ("Attachment had been uploaded, isUpdateRequired should have returned NOTHING",
//				UpdateType.NOTHING, sampleRes.updateRequired(matching));
//		assertEquals ("Attachment was added, isUpdateRequired should have returned type UPDATE",
//				UpdateType.UPDATE, sampleRes.getAttachment("useDefaultURL").updateRequired(matching));
//		uploadResource(sampleRes);

        // Now create a new attachment and use the internal URL, should add a new attachment
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "specifyURL");
        matching = sampleRes.findMatchingResource();
        assertEquals("Asset not changed, isUpdateRequired should have returned NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleRes.getAttachment("specifyURL").updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        // Now use an internal URL, should add a new attachment
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "specifyURL", "http://blah");
        matching = sampleRes.findMatchingResource();
        assertEquals("Asset not changed, isUpdateRequired should have returned NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        assertEquals("Attachment URL was set to external, isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleRes.getAttachment("specifyURL").updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        // Now change the URL, this should cause an update on attachment
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "specifyURL", "http://differentblah");
        matching = sampleRes.findMatchingResource();
        assertEquals("Asset not changed, isUpdateRequired should have returned NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        assertEquals("Attachment URL was changed, isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleRes.getAttachment("specifyURL").updateRequired(getFirst(matching)));
        uploadResource(sampleRes);
    }

    /**
     * Check CRC of attachment, upload it then verify CRC matches the input stream
     * from massive for the asset
     */
    @Test
    public void testCRC() throws Exception {
        File attachment = new File(resourceDir, "TestAttachment.txt");
        Method getCRCMethod = MassiveResource.class.getDeclaredMethod("getCRC", InputStream.class);
        getCRCMethod.setAccessible(true);
        long localFileCRC = (Long) getCRCMethod.invoke(null, new FileInputStream(attachment));

        SampleResource sampleRes = createSampleResource();
        sampleRes.addContent(attachment);
        uploadResource(sampleRes);
        long massiveFileCRC = sampleRes.getAttachment(attachment.getName()).getCRC();

        assertEquals("CRC of file before adding to massive is different from the one in massive",
                     localFileCRC, massiveFileCRC);
    }

    @Test
    @Ignore
    // needs update assets
    public void testFeaturedWeight() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException {
        SampleResource sampleRes = createSampleResource();
        uploadResource(sampleRes);
        String id = sampleRes.get_id();

        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertNull("The resource should not have a featuredWeight", readBack.getFeaturedWeight());

        sampleRes.setFeaturedWeight("5");
        updateResource(sampleRes);
        readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertEquals("The resource should not have a featuredWeight", "5", readBack.getFeaturedWeight());

        sampleRes.setFeaturedWeight("4");
        updateResource(sampleRes);
        readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertEquals("The resource should not have a featuredWeight", "4", readBack.getFeaturedWeight());

        sampleRes.setFeaturedWeight(null);
        updateResource(sampleRes);
        readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertNull("The resource should not have a featuredWeight", readBack.getFeaturedWeight());
    }

    /**
     * Checks that the resource should get updated if the CRC changes
     *
     * @throws URISyntaxException
     * @throws RepositoryException
     */
    @Test
    public void testCRCChangeCausesUpdate() throws URISyntaxException, RepositoryException {
        SampleResource sampleRes = createSampleResource();
        List<MassiveResource> matching = sampleRes.findMatchingResource();
        assertEquals("Attachment had been uploaded, isUpdateRequired should have returned ADD",
                     UpdateType.ADD, sampleRes.updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        File attachment1 = new File(resourceDir, "crc1.txt");
        sampleRes.addAttachment(attachment1, AttachmentType.DOCUMENTATION, "documentationAttachment");
        matching = sampleRes.findMatchingResource();
        // The resource should not need updating after an attachment is added
        assertEquals("Attachment was added, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        // ... but the attachment does need adding
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleRes.getAttachment("documentationAttachment").updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        File attachment2 = new File(resourceDir, "crc2.txt");
        sampleRes.addAttachment(attachment2, AttachmentType.DOCUMENTATION, "documentationAttachment");
        matching = sampleRes.findMatchingResource();
        // The resource should not need updating
        assertEquals("Attachment was changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        // ... but the attachment does need updating
        assertEquals("Attachment was added, isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleRes.getAttachment("documentationAttachment").updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());
    }

    /**
     * This test uploads an attachment, then re-uploads a different attachment under the same
     * name (causing an update). This test ensures that the information in AttachmentInfo (in this
     * case we use CRC) is updated correctly when the attachment is updated.
     *
     * @throws Exception
     */
    @Test
    public void testUpdateAttachmentInfo() throws Exception {
        Method getCRCMethod = MassiveResource.class.getDeclaredMethod("getCRC", InputStream.class);
        getCRCMethod.setAccessible(true);

        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        File attachment2 = new File(resourceDir, "TestAttachment2.txt");
        long localFileCRC1 = (Long) getCRCMethod.invoke(null, new FileInputStream(attachment1));
        long localFileCRC2 = (Long) getCRCMethod.invoke(null, new FileInputStream(attachment2));
        System.out.println("local1 = " + localFileCRC1);
        System.out.println("local2 = " + localFileCRC2);

        SampleResource sampleRes = createSampleResource();
        sampleRes.addContent(attachment1);
        uploadResource(sampleRes);
        long massiveFileCRC = sampleRes.getAttachment(attachment1.getName()).getCRC();

        assertEquals("CRC of file before adding to massive is different from the one in massive",
                     localFileCRC1, massiveFileCRC);

        // Specify that the "TestAttachment2.txt" should be stored under the name "TestAttachment.txt". This
        // will mean the attachment gets updated, rather than a new attachment added
        sampleRes.addAttachment(attachment2, AttachmentType.CONTENT, "TestAttachment.txt");
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());
        massiveFileCRC = sampleRes.getAttachment(attachment1.getName()).getCRC();

        assertEquals("CRC of file before adding to massive is different from the one in massive",
                     localFileCRC2, massiveFileCRC);
    }

    /**
     * This method checks that the provider name is set correctly, and doesn't use a default
     */
    @Test
    public void testDifferentProvider() throws RepositoryException, URISyntaxException {
        SampleResource sampleRes = createSampleResource();
        uploadResource(sampleRes);
        MassiveResource readBack = SampleResource.getSample(_loginInfoEntry, sampleRes.get_id());
        System.out.println("Readback " + readBack);
        assertTrue("The asset read back from Massive didn't have the expected provider",
                   readBack.toString().contains(PROVIDER_NAME));
        assertEquals("The asset read back from Massive didn't have the expected provider",
                     sampleRes.getProvider(), readBack.getProvider());
    }

    /**
     * This tests that we can't refreshFromMassive after a resource has been deleted
     */
    @Test
    @Ignore
    // needs exact error message
    public void testRefreshAfterDelete() throws URISyntaxException, RepositoryException {
        populateResource(_testRes);
        uploadResource(_testRes);
        _testRes.delete();
        try {
            _testRes.refreshFromMassive();
            fail("We should not be able to refresh an asset from massive after it has been deleted");
        } catch (RepositoryBackendRequestFailureException e) {
            // expected
            assertTrue("Unexpected exception caught " + e, e.getMessage().contains("Could not find asset for id " + _testRes.get_id()));
        }
    }

    /**
     * This method checks that the size value on an attachment is equal to the
     * size of the attachment payload
     */
    @Test
    public void testGetSize() throws URISyntaxException, RepositoryException {
        SampleResource sampleRes = createSampleResource();
        File attachment = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment);
        uploadResource(sampleRes);
        long size = sampleRes.getAttachment("TestAttachment.txt").getSize();
        assertEquals(size, attachment.length());
    }

    /**
     * This test checks we can upload 2 attachments in one asset. We check that the attachment
     * count is 2 and that we can get an InputStream to both attachments
     *
     */
    @Test
    public void testAddMultipleAttachments() throws URISyntaxException, RepositoryException {
        SampleResource sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addLicense(attachment2, Locale.ENGLISH);
        uploadResource(sampleRes);
        String id = sampleRes.get_id();
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertEquals("We expected 2 attachment", 2, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment1 was found",
                      readBack.getAttachment(attachment1.getName()).getInputStream());
        assertNotNull("No input stream to the attachment2 was found",
                      readBack.getLicense(Locale.ENGLISH));
    }

    /**
     * Tests that we can upload 2 attachments then get a snapshot of the resource from massive.
     * We then make sure that snapshot is equivalent to the resource we uploaded. We then update
     * one of the attachments then upload the resource again. We get a 2nd snapshot of the resource
     * from massive. We ensure that the two snapshots are not equivalent, that the one we uploaded
     * is equivalent to the 2nd snapshot but not equivalent to the first snapshot.
     */
    @Test
    public void testAddThenUpdateAttachment() throws URISyntaxException, RepositoryException,
                    SecurityException, NoSuchMethodException, IllegalArgumentException, FileNotFoundException,
                    IllegalAccessException, InvocationTargetException {
        SampleResource sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addLicense(attachment2, Locale.US);
        uploadResource(sampleRes);

        String id = sampleRes.get_id();
        MassiveResource readBack1 = MassiveResource.getResource(_loginInfoEntry, id);
        assertTrue("Resource should be equivalent", sampleRes.equivalent(readBack1));

        attachment1 = new File(resourceDir, "TestAttachment2.txt");
        sampleRes.addAttachment(attachment1, AttachmentType.CONTENT, "TestAttachment.txt");
        uploadResource(sampleRes);

        MassiveResource readBack2 = MassiveResource.getResource(_loginInfoEntry, id);
        assertFalse("Resource should not be equivalent", readBack1.equivalent(readBack2));
        assertFalse("Resource should not be equivalent", sampleRes.equivalent(readBack1));
        assertTrue("Resource should be equivalent", sampleRes.equivalent(readBack2));

    }

    /**
     * Tests that an attachment can be added to massive, and that we can then delete
     * the attachment. We then confirm that we resource has the correct number of
     * attachments and ensure that we can still get an input stream to the remaining
     * attachment, and that we can't get an input stream to the deleted attachment.
     * We then re-add the attachment to ensure there are no problems with adding
     * an attachment back after deleting it.
     */
    @Test
    public void testAddThenDeleteThenReadAttachment() throws URISyntaxException, RepositoryException {
        SampleResource sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addLicense(attachment2, Locale.ENGLISH);
        uploadResource(sampleRes);

        String id = sampleRes.get_id();

        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertEquals("We expected 2 attachment", 2, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment1 was found",
                      readBack.getAttachment(attachment1.getName()));
        assertNotNull("No input stream to the attachment2 was found",
                      readBack.getLicense(Locale.ENGLISH));

        sampleRes.getAttachment("TestAttachment.txt").deleteNow();

        readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertEquals("We expected 1 attachment", 1, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment2 was found",
                      readBack.getLicense(Locale.ENGLISH));

        AttachmentResource at = readBack.getAttachment(attachment1.getName());
        assertNull(at);

        // Lets add it back to ensure there is no problems re-adding a deleted attachment
        sampleRes.addContent(attachment1);
        updateResource(sampleRes);

        readBack = MassiveResource.getResource(_loginInfoEntry, id);
        assertEquals("We expected 2 attachment", 2, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment1 was found",
                      readBack.getAttachment(attachment1.getName()));
        assertNotNull("No input stream to the attachment2 was found",
                      readBack.getLicense(Locale.US));
    }

    /**
     * Tests that the attachment can be downloaded and the file sizes are the same
     */
    @Test
    public void testDownloadFile() throws RepositoryException, URISyntaxException, IOException {
        SampleResource sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        uploadResource(sampleRes);
        String id = sampleRes.get_id();
        MassiveResource readBack = MassiveResource.getResource(_loginInfoEntry, id);
        AttachmentResource at = readBack.getAttachment("TestAttachment.txt");
        at.dump(System.out);
        File download = new File(resourceDir, "download.jar");
        if (download.exists()) {
            download.delete();
        }
        download.createNewFile();
        download.deleteOnExit();
        at.downloadToFile(download);
        assertEquals(at.getSize(), download.length());
    }

    /**
     * CAUTION: The test name is important for this test.
     * When a sample resource is created it is given the name of the test that created it, so that
     * if any resource are left behind we know where it was created. This test uses the name of
     * the resource to locate a resource it uploads so if you change the method name you must
     * also change the check
     * if (res.getName().endsWith("testGetAllAssetsDoesntGetAttachments")) {
     * to match the new methiod name
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetAllAssetsDoesntGetAttachments() throws Exception {
        SampleResource sampleRes = createSampleResource();
        String sampleName = "testGetAllAssetsDoesntGetAttachments";
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addContent(attachment1);
        sampleRes.addLicense(attachment2, Locale.US);
        sampleRes.setName(sampleName);
        uploadResource(sampleRes);

        Collection<MassiveResource> allResources = MassiveResource.getAllResources(new LoginInfo(_loginInfoEntry));

        MassiveResource readBack = null;
        for (MassiveResource res : allResources) {
            System.out.println("Found res " + res.getName());
            if (res.getName().equals(sampleName)) {
                readBack = res;
                break;
            }
        }

        assertNotNull("Wasn't able to find the resource that was just uploaded. Uh oh", readBack);

        Field f = MassiveResource.class.getDeclaredField("_attachments");
        f.setAccessible(true);
        Object o = f.get(readBack);
        HashMap<String, AttachmentResource> attachments = (HashMap<String, AttachmentResource>) o;
        assertTrue(attachments.isEmpty());

        assertEquals("There should be 2 attachments (lazily attached) when we try and get them", 2,
                     readBack.getAttachmentCount());

    }

    /**
     * Test to ensure we can read the attachment of type DOCUMENTATION from an asset without
     * authentication.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleUnauthDOCUMENTATION() throws Exception {
        simpleUserBASE(AttachmentType.DOCUMENTATION, false);
    }

    /**
     * Test to ensure we can read the attachment of type DOCUMENTATION from an asset without
     * authentication.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleUnauthLicenseDOCUMENTATION() throws Exception {
        simpleUserBASE(AttachmentType.DOCUMENTATION, true);
    }

    /**
     * Test to ensure we can read the attachment of type CONTENT from an asset without
     * authentication.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleUnauthCONTENT() throws Exception {
        simpleUserBASE(AttachmentType.CONTENT, false);
    }

    /**
     * Test to ensure we can read the attachment of type CONTENT from an asset without
     * authentication.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleUnauthLicenseCONTENT() throws Exception {
        simpleUserBASE(AttachmentType.CONTENT, true);
    }

    /**
     * Test to make sure that we always set the asset type when we create it
     */
    @Test
    public void testTypeSetOnCreation() {
        for (MassiveResource.Type type : MassiveResource.Type.values()) {
            MassiveResource resource = type.createResource(null);
            assertEquals("The type should be set", type, resource.getType());
        }
    }

    /**
     * Method to add an asset with an attachment of the specified type and ensure it can be read back
     * by a user that has the User role (but not the Administrator role).
     *
     * @param type
     * @throws RepositoryException
     */
    public void simpleUserBASE(AttachmentType type, boolean licensed) throws RepositoryException
    {
        // create provider
        Provider prov = new Provider();
        prov.setName("IBM");

        // create asset
        System.out.println("Creating asset with attachment of type " + type);
        MassiveResource res = new IfixResource(_loginInfoEntry);
        res.addAttachment(new File(resourceDir, "TestAttachment.txt"), type, "TestAttachment.txt");
        res.setProvider(prov);
        res.setName("Test ifix resource " + createDateString());
        if (licensed) {
            res.addLicense(new File(resourceDir, "license_enus.txt"), Locale.ENGLISH);
        }

        // upload asset and publish it
        res.uploadToMassive(new UpdateInPlaceStrategy());
        String id = res.get_id();
        res.publish();
        res.approve();

        // Get resource without authentication
        LoginInfoEntry userLoginInfo = repo.getUserLoginInfoEntry();

        IfixResource userIfix;
        try {
            userIfix = IfixResource.getIfix(userLoginInfo, id); //UNAUTH
            // expected
        } catch (RepositoryBackendException e) {
            throw new AssertionError(e);
        }

        // check we can get an input stream to all
        Collection<MassiveResource.AttachmentResource> attResources = userIfix.getAttachments();
        for (MassiveResource.AttachmentResource attResource : attResources) {
            attResource.getInputStream();
        }
    }

    /**
     * Create two samples with the same name and type but different providers to ensure that
     * separate test items are created (name, provider and type are used to work out whether
     * to do an add or replace) BUT with the same generated vanityRelativeURL (as provider is
     * not part of the generated vanityURL). Call getAllResourcesWithVanityRelativeURL and
     * ensure that both are returned.
     */
    @Test
    public void testGetAllResourcesWithVanityRelativeURL() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {

        SampleResource sampleRes1 = createSampleResource();
        sampleRes1.setName("testGetAllResourcesWithVanityRelativeURL");
        Provider p1 = new Provider();
        p1.setName("Provider1");
        sampleRes1.setProvider(p1);
        uploadResource(sampleRes1);

        SampleResource sampleRes2 = createSampleResource();
        sampleRes2.setName("testGetAllResourcesWithVanityRelativeURL");
        Provider p2 = new Provider();
        p2.setName("Provider2");
        sampleRes2.setProvider(p2);
        uploadResource(sampleRes2);

        String vanityRelativeURL = "samples-testGetAllResourcesWithVanityRelativeURL";
        Collection<MassiveResource> collection = MassiveResource.
                        getAllResourcesWithVanityRelativeURL(new LoginInfo(_loginInfoEntry), vanityRelativeURL);

        assertEquals("There should be 2 resources returned", 2,
                     collection.size());
    }

    @Test
    @Ignore
    // Multi-repository
    public void testSimpleGetAllFromMultipleRepos() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {

//        LoginInfoEntry repo2 = loginInfoProvider.createNewRepoFromProperties();
        LoginInfoEntry repo2 = null;

        // _loginInfo contains just repo1

        // Creates a collection with just repo2 in (for uploading)
        LoginInfo repo1Only = new LoginInfo(_loginInfoEntry);
        LoginInfo repo2Only = new LoginInfo(repo2);

        // Creates a collection with both repos in (for reading from). Get repo1 from _loginInfo
        LoginInfo bothRepos = new LoginInfo(_loginInfoEntry);
        bothRepos.add(repo2);

        // Add an asset to repo1
        SampleResource sampleRes1 = createSampleResource();
        sampleRes1.setName("samp1");
        uploadResource(sampleRes1);

        // Add an asset to repo2
        SampleResource sampleRes2 = createSampleResource();
        sampleRes2.setName("samp2");
        // Override default so asset goes to repo2
        sampleRes2.setLoginInfoEntry(repo2);
        uploadResource(sampleRes2);

        // Should only get one asset from repo1
        assertEquals("Should only be 1 asset in repo1", 1, MassiveResource.getAllResources(repo1Only).size());

        // Should only get one asset from repo2
        assertEquals("Should only be 1 asset in repo2", 1, MassiveResource.getAllResources(repo2Only).size());

        // Should get two assets when using both repos
        assertEquals("Should get two assets when using both repos", 2, MassiveResource.getAllResources(bothRepos).size());
    }

    @Test
    @Ignore
    // Multi-repository
    public void testGetAllFromMultipleReposWithDupes() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {

//        LoginInfoEntry repo2 = loginInfoProvider.createNewRepoFromProperties();
        LoginInfoEntry repo2 = null;

        // _loginInfo contains just repo1

        // Creates a collection with just repo2 in (for uploading)
        LoginInfo repo1Only = new LoginInfo(_loginInfoEntry);
        LoginInfo repo2Only = new LoginInfo(repo2);

        // Creates a collection with both repos in (for reading from). Get repo1 from _loginInfo
        LoginInfo bothRepos = new LoginInfo(_loginInfoEntry);
        bothRepos.add(repo2);

        // Add an asset to repo1
        SampleResource sampleRes1 = createSampleResource();
        sampleRes1.setName("samp1");
        uploadResource(sampleRes1);

        // Add an asset to repo1
        SampleResource sampleRes2 = createSampleResource();
        sampleRes2.setName("samp2");
        uploadResource(sampleRes2);

        // Add the same asset again to repo2. Override default so asset goes to repo2
        sampleRes2.setLoginInfoEntry(repo2);
        uploadResource(sampleRes2);

        // Should only get one asset from repo1
        assertEquals("Should be 2 assets in repo1", 2, MassiveResource.getAllResources(repo1Only).size());

        // Should only get one asset from repo2
        assertEquals("Should only be 1 asset in repo2", 1, MassiveResource.getAllResources(repo2Only).size());

        // Should get two assets when using both repos
        assertEquals("Should get two assets when using both repos", 2, MassiveResource.getAllResources(bothRepos).size());
    }

    /**
     * Tests that if you filter for a specific version you get back the expected result
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringWithRightVersion() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        FilterResources expectedResources = setupRepoForFilterTests();

        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition), null, null, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected there to be 5 features", 5, features.size());
        assertTrue("Simple feature should be included", features.contains(expectedResources.simpleFeature));
        assertTrue("Feature with two products should be included", features.contains(expectedResources.featureTwoProducts));
        assertTrue("Feature with no version should be included", features.contains(expectedResources.featureWithNoVersion));
        assertTrue("Feature with editions should be included", features.contains(expectedResources.featureWithEditions));
        assertTrue("Feature with install type should be included", features.contains(expectedResources.featureWithInstallType));

        Collection<SampleResource> samples = (Collection<SampleResource>) result.get(Type.PRODUCTSAMPLE);
        assertEquals("The sample should be returned", 1, samples.size());
        assertEquals("The right sample should be returned", expectedResources.sample, samples.iterator().next());

        Collection<ProductRelatedResource> addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertEquals("The addon should be returned", 1, addons.size());
        assertEquals("The right addon should be returned", expectedResources.addon, addons.iterator().next());
    }

    /**
     * Tests that if you filter for version lower than most of the resources in the repo you get back the expected result
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringWithLowVersion() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.0.0.0", "Archive", "ILAN", "DEVELOPERS");
        FilterResources expectedResources = setupRepoForFilterTests();

        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition), null, null, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected there to be 1 feature", 1, features.size());
        assertTrue("Feature with no version should be the only feature returned", features.contains(expectedResources.featureWithNoVersion));

        Collection<SampleResource> samples = (Collection<SampleResource>) result.get(Type.PRODUCTSAMPLE);
        assertNull("No samples should be returned", samples);

        Collection<ProductRelatedResource> addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you filter for version higher than most of the resources in the repo you get back the expected result
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringWithHighVersion() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.4", "Archive", "ILAN", "DEVELOPERS");
        FilterResources expectedResources = setupRepoForFilterTests();

        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition), null, null, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected there to be 2 features", 2, features.size());
        assertTrue("Feature with no version should be returned", features.contains(expectedResources.featureWithNoVersion));
        assertTrue("Feature with high version should be returned", features.contains(expectedResources.featureWithLaterVersion));

        Collection<SampleResource> samples = (Collection<SampleResource>) result.get(Type.PRODUCTSAMPLE);
        assertEquals("The sample should be returned as it only specifies a min version", 1, samples.size());
        assertEquals("The right sample should be returned", expectedResources.sample, samples.iterator().next());

        Collection<ProductRelatedResource> addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you filter for a specific type you get back the expected result
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringByType() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        FilterResources expectedResources = setupRepoForFilterTests();

        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition),
                                                                                               Collections.singleton(Type.PRODUCTSAMPLE), null, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertNull("Expected there to be no features", features);

        Collection<SampleResource> samples = (Collection<SampleResource>) result.get(Type.PRODUCTSAMPLE);
        assertEquals("The sample should be returned", 1, samples.size());
        assertEquals("The right sample should be returned", expectedResources.sample, samples.iterator().next());

        Collection<ProductRelatedResource> addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you filter for two types you get back the expected result
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringByMultipleTypes() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        FilterResources expectedResources = setupRepoForFilterTests();

        Collection<Type> types = new HashSet<MassiveResource.Type>();
        types.add(Type.PRODUCTSAMPLE);
        types.add(Type.ADDON);
        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition), types, null,
                                                                                               new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertNull("Expected there to be no features", features);

        Collection<SampleResource> samples = (Collection<SampleResource>) result.get(Type.PRODUCTSAMPLE);
        assertEquals("The sample should be returned", 1, samples.size());
        assertEquals("The right sample should be returned", expectedResources.sample, samples.iterator().next());

        Collection<ProductRelatedResource> addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertEquals("The addon should be returned", 1, addons.size());
        assertEquals("The right addon should be returned", expectedResources.addon, addons.iterator().next());
    }

    /**
     * Tests that if you filter for a specific type and no product defintiion you get back the expected result
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringByTypeOnly() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        FilterResources expectedResources = setupRepoForFilterTests();

        Collection<Type> types = new HashSet<MassiveResource.Type>();
        types.add(Type.FEATURE);
        types.add(Type.PRODUCTSAMPLE);
        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(null, types, null, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("All the features should be returned", 8, features.size());
        assertTrue("Simple feature should be included", features.contains(expectedResources.simpleFeature));
        assertTrue("Feature with two products should be included", features.contains(expectedResources.featureTwoProducts));
        assertTrue("Feature with no version should be included", features.contains(expectedResources.featureWithNoVersion));
        assertTrue("Feature with editions should be included", features.contains(expectedResources.featureWithEditions));
        assertTrue("Feature with install type should be included", features.contains(expectedResources.featureWithInstallType));
        assertTrue("Feature from other product but same version should be included", features.contains(expectedResources.featureOtherProductSameVersion));
        assertTrue("Feature from other product should be included", features.contains(expectedResources.featureOtherProduct));
        assertTrue("Feature with later version should be included", features.contains(expectedResources.featureWithLaterVersion));

        Collection<SampleResource> samples = (Collection<SampleResource>) result.get(Type.PRODUCTSAMPLE);
        assertEquals("The sample should be returned", 1, samples.size());
        assertEquals("The right sample should be returned", expectedResources.sample, samples.iterator().next());

        Collection<ProductRelatedResource> addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you are just getting features you can filter them by visibility
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringFeaturesByVisibility() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        FilterResources expectedResources = setupRepoForFilterTests();

        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition), Collections.singleton(Type.FEATURE),
                                                                                               Visibility.PUBLIC, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected two public feature", 2, features.size());
        assertTrue("Expected the simple public feature", features.contains(expectedResources.simpleFeature));
        assertTrue("Expected the public feature in two products", features.contains(expectedResources.featureTwoProducts));

        result = MassiveResource.getResources(Collections.singleton(productDefinition),
                                              Collections.singleton(Type.FEATURE), Visibility.PRIVATE, new LoginInfo(_loginInfoEntry));
        features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected a single private feature", 1, features.size());
        assertTrue("Expected the private feature", features.contains(expectedResources.featureWithNoVersion));

        result = MassiveResource.getResources(Collections.singleton(productDefinition),
                                              Collections.singleton(Type.FEATURE), Visibility.INSTALL, new LoginInfo(_loginInfoEntry));
        features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected a single install feature", 1, features.size());
        assertTrue("Expected the install feature", features.contains(expectedResources.featureWithInstallType));

        result = MassiveResource.getResources(Collections.singleton(productDefinition),
                                              Collections.singleton(Type.FEATURE), Visibility.PROTECTED, new LoginInfo(_loginInfoEntry));
        features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected a single protected feature", 1, features.size());
        assertTrue("Expected the protected feature", features.contains(expectedResources.featureWithEditions));
    }

    /**
     * Tests that if you are just getting features and other types you can filter them by visibility, note that this takes a different code path to when doing just features as
     * above hence the two tests
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringByVisibilityAndMultipleTypes() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        FilterResources expectedResources = setupRepoForFilterTests();

        Collection<Type> types = new HashSet<MassiveResource.Type>();
        types.add(Type.FEATURE);
        types.add(Type.ADDON);
        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition), types, Visibility.PUBLIC,
                                                                                               new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected a single public feature", 2, features.size());
        assertTrue("Expected the simple public feature", features.contains(expectedResources.simpleFeature));
        assertTrue("Expected the public feature in two products", features.contains(expectedResources.featureTwoProducts));
        Collection<ProductRelatedResource> addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertEquals("The addon should be returned", 1, addons.size());
        assertEquals("The right addon should be returned", expectedResources.addon, addons.iterator().next());

        result = MassiveResource.getResources(Collections.singleton(productDefinition), types, Visibility.PRIVATE, new LoginInfo(_loginInfoEntry));
        features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected a single private feature", 1, features.size());
        assertTrue("Expected the private feature", features.contains(expectedResources.featureWithNoVersion));
        addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertEquals("The addon should be returned", 1, addons.size());
        assertEquals("The right addon should be returned", expectedResources.addon, addons.iterator().next());

        result = MassiveResource.getResources(Collections.singleton(productDefinition), types, Visibility.INSTALL, new LoginInfo(_loginInfoEntry));
        features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected a single install feature", 1, features.size());
        assertTrue("Expected the install feature", features.contains(expectedResources.featureWithInstallType));
        addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertEquals("The addon should be returned", 1, addons.size());
        assertEquals("The right addon should be returned", expectedResources.addon, addons.iterator().next());

        result = MassiveResource.getResources(Collections.singleton(productDefinition), types, Visibility.PROTECTED, new LoginInfo(_loginInfoEntry));
        features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected a single protected feature", 1, features.size());
        assertTrue("Expected the protected feature", features.contains(expectedResources.featureWithEditions));
        addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertEquals("The addon should be returned", 1, addons.size());
        assertEquals("The right addon should be returned", expectedResources.addon, addons.iterator().next());
    }

    /**
     * Tests that if you filter for a different product ID you get back the expected result
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringWithDifferentProduct() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.plw", "1.0.0.0", "Archive", "ILAN", "DEVELOPERS");
        FilterResources expectedResources = setupRepoForFilterTests();

        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition), null, null, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected there to be two features", 2, features.size());
        assertTrue("Feature for the other product should be included", features.contains(expectedResources.featureOtherProduct));
        assertTrue("Feature with two products should be included", features.contains(expectedResources.featureTwoProducts));

        Collection<SampleResource> samples = (Collection<SampleResource>) result.get(Type.PRODUCTSAMPLE);
        assertNull("No samples should be returned", samples);

        Collection<ProductRelatedResource> addons = (Collection<ProductRelatedResource>) result.get(Type.ADDON);
        assertNull("No addons should be returned", addons);
    }

    /**
     * Tests that if you filter for a different edition you get back the expected result
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringWithEdition() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "ND");
        FilterResources expectedResources = setupRepoForFilterTests();

        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition), Collections.singleton(Type.FEATURE),
                                                                                               null, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected there to be 4 features", 4, features.size());
        assertTrue("Simple feature should be included", features.contains(expectedResources.simpleFeature));
        assertTrue("Feature with no version should be included", features.contains(expectedResources.featureWithNoVersion));
        assertTrue("Feature with install type should be included", features.contains(expectedResources.featureWithInstallType));
        assertTrue("Feature with two products should be included", features.contains(expectedResources.featureTwoProducts));
    }

    /**
     * Tests that if you filter for a different install type you get back the expected result
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringWithInstallType() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "InstallationManager", "ILAN", "DEVELOPERS");
        FilterResources expectedResources = setupRepoForFilterTests();

        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(Collections.singleton(productDefinition), Collections.singleton(Type.FEATURE),
                                                                                               null, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected there to be 4 features", 4, features.size());
        assertTrue("Simple feature should be included", features.contains(expectedResources.simpleFeature));
        assertTrue("Feature with no version should be included", features.contains(expectedResources.featureWithNoVersion));
        assertTrue("Feature with editions should be included", features.contains(expectedResources.featureWithEditions));
        assertTrue("Feature with two products should be included", features.contains(expectedResources.featureTwoProducts));
    }

    /**
     * Tests that if you filter for two different products you get everything back
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilteringWithMultipleProducts() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        ProductDefinition productDefinition1 = new SimpleProductDefinition("com.ibm.ws.wlp", "8.5.5.0", "Archive", "ILAN", "DEVELOPERS");
        ProductDefinition productDefinition2 = new SimpleProductDefinition("com.ibm.ws.plw", "1.0.0.0", "Archive", "ILAN", "DEVELOPERS");
        Collection<ProductDefinition> products = new HashSet<ProductDefinition>();
        products.add(productDefinition1);
        products.add(productDefinition2);
        FilterResources expectedResources = setupRepoForFilterTests();

        Map<Type, Collection<? extends MassiveResource>> result = MassiveResource.getResources(products, Collections.singleton(Type.FEATURE), null, new LoginInfo(_loginInfoEntry));
        Collection<EsaResource> features = (Collection<EsaResource>) result.get(Type.FEATURE);
        assertEquals("Expected there to be 6 features", 6, features.size());
        assertTrue("Simple feature should be included", features.contains(expectedResources.simpleFeature));
        assertTrue("Feature with no version should be included", features.contains(expectedResources.featureWithNoVersion));
        assertTrue("Feature with editions should be included", features.contains(expectedResources.featureWithEditions));
        assertTrue("Feature with two products should be included", features.contains(expectedResources.featureTwoProducts));
        assertTrue("Feature with install type should be included", features.contains(expectedResources.featureWithInstallType));
        assertTrue("Feature for other product should be included", features.contains(expectedResources.featureOtherProduct));
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * HELPER METHODS
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * <p>This will create the following resources:</p>
     * <table>
     * <thead><th>Name</th><th>Type</th><th>Visibility</th><th>Applies To</th></thead>
     * <tbody>
     * <tr><td>Simple Feature</td><td>Feature</td><td>Public</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0</td></tr>
     * <tr><td>Feature With No Version</td><td>Feature</td><td>Private</td><td>com.ibm.ws.wlp</td></tr>
     * <tr><td>Feature with later version</td><td>Feature</td><td>Public</td><td>com.ibm.ws.wlp; productVersion=8.5.5.4</td></tr>
     * <tr><td>Feature other product</td><td>Feature</td><td>Public</td><td>com.ibm.ws.plw; productVersion=1.0.0.0</td></tr>
     * <tr><td>Feature other product but same version</td><td>Feature</td><td>Public</td><td>com.ibm.ws.plw; productVersion=8.5.5.4</td></tr>
     * <tr><td>Feature two products</td><td>Feature</td><td>Public</td><td>com.ibm.ws.plw; productVersion=1.0.0.0, com.ibm.ws.wlp; productVersion=8.5.5.0</td></tr>
     * <tr><td>Feature with editions</td><td>Feature</td><td>Protected</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0; productEdition="BASE,DEVELOPERS"</td></tr>
     * <tr><td>Feature with install type</td><td>Feature</td><td>Install</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0; productInstallType=Archive</td></tr>
     * <tr><td>Sample</td><td>Prouct Sample</td><td>n/a</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0+</td></tr>
     * <tr><td>Addon</td><td>Addon</td><td>n/a</td><td>com.ibm.ws.wlp; productVersion=8.5.5.0</td></tr>
     * </tbody>
     * </table>
     *
     * @return a data object containing the resources
     * @throws URISyntaxException
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    private FilterResources setupRepoForFilterTests() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {
        EsaResource simpleFeature = new EsaResource(_loginInfoEntry);
        populateResource(simpleFeature);
        simpleFeature.setName("Simple Feature");
        simpleFeature.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0");
        simpleFeature.setVisibility(Visibility.PUBLIC);
        uploadResource(simpleFeature);

        EsaResource featureWithNoVersion = new EsaResource(_loginInfoEntry);
        populateResource(featureWithNoVersion);
        featureWithNoVersion.setName("Feature With No Version");
        featureWithNoVersion.setAppliesTo("com.ibm.ws.wlp");
        featureWithNoVersion.setVisibility(Visibility.PRIVATE);
        uploadResource(featureWithNoVersion);

        EsaResource featureWithLaterVersion = new EsaResource(_loginInfoEntry);
        populateResource(featureWithLaterVersion);
        featureWithLaterVersion.setName("Feature with later version");
        featureWithLaterVersion.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.4");
        featureWithLaterVersion.setVisibility(Visibility.PUBLIC);
        uploadResource(featureWithLaterVersion);

        EsaResource featureOtherProduct = new EsaResource(_loginInfoEntry);
        populateResource(featureOtherProduct);
        featureOtherProduct.setName("Feature other product");
        featureOtherProduct.setAppliesTo("com.ibm.ws.plw; productVersion=1.0.0.0");
        featureOtherProduct.setVisibility(Visibility.PUBLIC);
        uploadResource(featureOtherProduct);

        EsaResource featureOtherProductSameVersion = new EsaResource(_loginInfoEntry);
        populateResource(featureOtherProductSameVersion);
        featureOtherProductSameVersion.setName("Feature other product");
        featureOtherProductSameVersion.setAppliesTo("com.ibm.ws.plw; productVersion=8.5.5.0");
        featureOtherProductSameVersion.setVisibility(Visibility.PUBLIC);
        uploadResource(featureOtherProductSameVersion);

        EsaResource featureTwoProducts = new EsaResource(_loginInfoEntry);
        populateResource(featureTwoProducts);
        featureTwoProducts.setName("Feature two products");
        featureTwoProducts.setAppliesTo("com.ibm.ws.plw; productVersion=1.0.0.0, com.ibm.ws.wlp; productVersion=8.5.5.0");
        featureTwoProducts.setVisibility(Visibility.PUBLIC);
        uploadResource(featureTwoProducts);

        EsaResource featureWithEditions = new EsaResource(_loginInfoEntry);
        populateResource(featureWithEditions);
        featureWithEditions.setName("Feature with editions");
        featureWithEditions.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0; productEdition=\"BASE,DEVELOPERS\"");
        featureWithEditions.setVisibility(Visibility.PROTECTED);
        uploadResource(featureWithEditions);

        EsaResource featureWithInstallType = new EsaResource(_loginInfoEntry);
        populateResource(featureWithInstallType);
        featureWithInstallType.setName("Feature with install type");
        featureWithInstallType.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0; productInstallType=Archive");
        featureWithInstallType.setVisibility(Visibility.INSTALL);
        uploadResource(featureWithInstallType);

        SampleResource sample = new SampleResource(_loginInfoEntry);
        populateResource(sample);
        sample.setName("Sample");
        sample.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0+");
        sample.setType(Type.PRODUCTSAMPLE);
        uploadResource(sample);

        ProductResource addon = new ProductResource(_loginInfoEntry);
        populateResource(addon);
        addon.setName("Addon");
        addon.setType(Type.ADDON);
        addon.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0");
        uploadResource(addon);

        return new FilterResources(simpleFeature, featureWithNoVersion, featureWithLaterVersion, featureOtherProduct, featureOtherProductSameVersion, featureTwoProducts, featureWithEditions, featureWithInstallType, sample, addon);
    }

    private static class FilterResources {
        private final EsaResource simpleFeature;
        private final EsaResource featureWithNoVersion;
        private final EsaResource featureWithLaterVersion;
        private final EsaResource featureOtherProduct;
        private final EsaResource featureOtherProductSameVersion;
        private final EsaResource featureTwoProducts;
        private final EsaResource featureWithEditions;
        private final EsaResource featureWithInstallType;
        private final SampleResource sample;
        private final ProductRelatedResource addon;

        public FilterResources(EsaResource simpleFeature, EsaResource featureWithNoVersion, EsaResource featureWithLaterVersion, EsaResource featureOtherProduct,
                               EsaResource featureOtherProductSameVersion, EsaResource featureTwoProducts, EsaResource featureWithEditions, EsaResource featureWithInstallType,
                               SampleResource sample, ProductRelatedResource addon) {
            super();
            this.simpleFeature = simpleFeature;
            this.featureWithNoVersion = featureWithNoVersion;
            this.featureWithLaterVersion = featureWithLaterVersion;
            this.featureOtherProduct = featureOtherProduct;
            this.featureOtherProductSameVersion = featureOtherProductSameVersion;
            this.featureTwoProducts = featureTwoProducts;
            this.featureWithEditions = featureWithEditions;
            this.featureWithInstallType = featureWithInstallType;
            this.sample = sample;
            this.addon = addon;
        }
    }

    /**
     * Tests an enum's mapping. Each value for an enum contains a reference to the base
     * enum it is mapping, this value can be obtained by invoking the "getterMethod" call.
     * It is possible to find the right enum value given the base enum value. A lookup table
     * is stored within each enum, and the "lookupMethod" can be invoked, passing in the
     * base enum value, to find the new mapped enum value.
     *
     * @param lookupMethod The method to invoke that finds the new mapped enum from the base
     *            enum
     * @param getterMethod The method to invoke to find the base enum from the mapped enum
     * @param values A list of values of the mapped enum
     * @param baseValues A list of values of base enum
     */
    private void runEnumTest(Method lookupMethod, Method getterMethod,
                             Object[] values, Object[] baseValues)
                    throws IllegalArgumentException, IllegalAccessException,
                    InvocationTargetException {
        lookupMethod.setAccessible(true);
        int i = 0;
        for (Object o : values) {
            assertEquals(o, lookupMethod.invoke(null, baseValues[i++]));
        }

        getterMethod.setAccessible(true);
        i = 0;
        for (Object o : baseValues) {
            assertEquals(o, getterMethod.invoke(values[i++]));
        }
    }

    private class TestResource extends MassiveResource {

        public TestResource(LoginInfoEntry loginInfo) {
            super(loginInfo);
            setType(Type.PRODUCTSAMPLE);
        }

        @Override
        public Asset getAsset() {
            return _asset;
        }

    }

    /**
     * Creates a string saying when something is created. This can be used to ensure that
     * a newly created asset will not be a match for a prior one to ensure that an add not
     * an update occurs
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

}
