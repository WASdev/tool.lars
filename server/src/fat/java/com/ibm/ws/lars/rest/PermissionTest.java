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

import static com.ibm.ws.lars.rest.RepositoryContext.RC_REJECT;
import static com.ibm.ws.lars.rest.matchers.ServerAssetByIdMatcher.assetsWithIds;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.lars.rest.RepositoryContext.Protocol;
import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.testutils.FatUtils;

/**
 * Test that security permissions work correctly.
 */
@RunWith(Parameterized.class)
public class PermissionTest {

    @Rule
    public RepositoryContext adminContext;

    // This is not a rule, as a rule does setup/tear down which is not wanted.
    public RepositoryContext testContext;

    /**
     * HTTP URL for the test instance where read operations are restricted to users with the User
     * role.
     */
    private static final String RESTRICTED_URL_HTTP = RepositoryContext.DEFAULT_URLS.get(Protocol.HTTP);

    /**
     * HTTPS URL for the test instance where read operations are restricted to users with the User
     * role.
     */
    private static final String RESTRICTED_URL_HTTPS = RepositoryContext.DEFAULT_URLS.get(Protocol.HTTPS);

    /**
     * HTTP URL for the test instance where read operations are unrestricted.
     * <p>
     * Write operations are still restricted to users with the Admin role.
     */
    private static final String UNRESTRICTED_URL_HTTP = "http://localhost:" + FatUtils.LIBERTY_PORT_HTTP + "/unrestricted" + FatUtils.LARS_APPLICATION_ROOT;

    /**
     * HTTPS URL for the test instance where read operations are unrestricted.
     * <p>
     * Write operations are still restricted to users with the Admin role.
     */
    private static final String UNRESTRICTED_URL_HTTPS = "https://localhost:" + FatUtils.LIBERTY_PORT_HTTPS + "/unrestricted" + FatUtils.LARS_APPLICATION_ROOT;

    /**
     * The available roles that we expect test users to be mapped to by the test server
     * configuration.
     */
    private enum Role {
        ADMIN,
        USER,
        NONE;

        public boolean isAdmin() {
            return this == ADMIN;
        }

        public boolean isUser() {
            return this == ADMIN || this == USER;
        }
    }

    /**
     * The users to test with.
     * <p>
     * Each user has a username and password and two roles. One is the role we expect the user to be
     * mapped to for the unrestricted configuration. The other is the role we expect the user to be
     * mapped to in the restricted configuration.
     * <p>
     * This enum is used to build the test parameters in {@link PermissionTest#makeParameters()}.
     */
    private enum User {
        ADMIN("admin", "passw0rd", Role.ADMIN, Role.ADMIN),
        USER("user", "passw0rd", Role.USER, Role.USER),
        NO_ROLE("noRoleUser", "passw0rd", Role.USER, Role.NONE),
        BAD_ADMIN_PW("admin", "wrongPassw0rd", Role.USER, Role.NONE),
        BAD_USER_PW("user", "wrongPassw0rd", Role.USER, Role.NONE),
        UNAUTHENTICATED(null, null, Role.USER, Role.NONE);

        String username;
        String password;
        Role restrictedConfigRole;
        Role unrestrictedConfigRole;

        private User(String username, String password, Role unrestrictedConfigRole, Role restrictedConfigRole) {
            this.username = username;
            this.password = password;
            this.restrictedConfigRole = restrictedConfigRole;
            this.unrestrictedConfigRole = unrestrictedConfigRole;
        }
    }

    private Asset testAsset;
    private Asset createdPublishedAsset;

    private Asset assetWithAttachments;
    private Asset createAssetWithAttachments;
    private String attachmentName;
    private byte[] attachmentContent;
    private Attachment attachment;
    private Attachment createdAttachment;

    /**
     * The role that we expect the user in the userContext to be mapped to
     * <p>
     * Tests should look at this field when deciding whether or not operations that they run from
     * the userContext should succeed or fail.
     */
    private final Role role;

    /**
     * Build the test parameters
     * <p>
     * We want to test the same methods with multiple users which we expect to map to different
     * roles. JUnit lets us construct a list of test parameters and each test will run which each
     * set of test parameters.
     * <p>
     * The parameters we are passing are URL, username, password, expected role and label. The label
     * is just used to name the test so we can tell them apart in the test results.
     *
     * @return the test parameters
     */
    @Parameters(name = "{4}")
    public static Collection<Object[]> makeParameters() {
        Collection<Object[]> data = new ArrayList<>();
        for (User user : User.values()) {
            data.add(new Object[] { RESTRICTED_URL_HTTP, user.username, user.password, user.restrictedConfigRole, user + " - restricted - http" });
            data.add(new Object[] { UNRESTRICTED_URL_HTTP, user.username, user.password, user.unrestrictedConfigRole, user + " - unrestricted - http" });
            data.add(new Object[] { RESTRICTED_URL_HTTPS, user.username, user.password, user.restrictedConfigRole, user + " - restricted - https" });
            data.add(new Object[] { UNRESTRICTED_URL_HTTPS, user.username, user.password, user.unrestrictedConfigRole, user + " - unrestricted - https" });
        }
        System.out.println("sending data: " + data.size());
        return data;
    }

    /**
     * Process the test parameters and set up the adminContext and userContext.
     */
    public PermissionTest(String url, String username, String password, Role role, String label) {
        adminContext = RepositoryContext.createAsAdmin(url);
        testContext = new RepositoryContext(url, username, password);
        this.role = role;
    }

    @Before
    public void setUp() throws IOException, InvalidJsonAssetException {
        testAsset = AssetUtils.getTestAsset();
        createdPublishedAsset = adminContext.addAssetNoAttachmentsWithState(testAsset, Asset.State.PUBLISHED);
        adminContext.addAssetNoAttachments(AssetUtils.getTestAsset());

        assetWithAttachments = AssetUtils.getTestAsset();
        createAssetWithAttachments = adminContext.addAssetNoAttachmentsWithState(assetWithAttachments, Asset.State.PUBLISHED);
        attachmentName = "nocontent.txt";
        attachmentContent = "I am the content.\nThere is not much content to be had.\n".getBytes(StandardCharsets.UTF_8);
        attachment = AssetUtils.getTestAttachmentWithContent();
        createdAttachment = adminContext.doPostAttachmentWithContent(createAssetWithAttachments.get_id(),
                                                                     attachmentName,
                                                                     attachment,
                                                                     attachmentContent,
                                                                     ContentType.APPLICATION_OCTET_STREAM);
    }

    /**
     *
     * GET /assets
     *
     * Allowed for ADMIN and USER
     */
    @Test
    public void testGetAllAssets() throws InvalidJsonAssetException, ParseException, IOException {
        if (role.isAdmin()) {
            AssetList assets = testContext.doGetAllAssets(200);
            assertEquals("Wrong number of assets", 3, assets.size());
        } else if (role.isUser()) {
            AssetList assets = testContext.doGetAllAssets(200);
            assertEquals("Wrong number of assets", 2, assets.size());
        } else {
            testContext.doGetAllAssetsBad(RC_REJECT);
        }
    }

    /**
     * POST /assets
     *
     * Allowed for ADMIN
     *
     */
    @Test
    public void testPostAsset() throws InvalidJsonAssetException, IOException {
        Asset asset = AssetUtils.getTestAsset();
        if (role.isAdmin()) {
            Asset returnedAsset = testContext.addAssetNoAttachmentsWithState(asset, Asset.State.PUBLISHED);
            AssetUtils.assertUploadedAssetEquivalentToOriginal("Returned asset should match the asset that was uploaded", asset, returnedAsset);
        } else {
            testContext.addBadAsset(asset, RC_REJECT);
        }
    }

    /**
     * GET /assets/{assetId}
     *
     * Allowed for ADMIN and USER
     */
    @Test
    public void testGetAsset() throws InvalidJsonAssetException, IOException {
        if (role.isUser()) {
            Asset returnedAsset = testContext.getAsset(createdPublishedAsset.get_id());
            AssetUtils.assertUploadedAssetEquivalentToOriginal("Returned asset should match the asset that was uploaded", testAsset, returnedAsset);
        } else {
            testContext.getBadAsset(createdPublishedAsset.get_id(), RC_REJECT);
        }
    }

    /**
     * DELETE /assets/{assetId}
     *
     * Allowed for ADMIN
     */
    @Test
    public void testDeleteAsset() throws IOException {
        if (role.isAdmin()) {
            testContext.deleteAsset(createdPublishedAsset.get_id(), 204);
        } else {
            testContext.deleteAsset(createdPublishedAsset.get_id(), RC_REJECT);
        }
    }

    /**
     * POST /assets/{assetId}/attachments Content-type: multipart/formdata
     *
     * Allowed for ADMIN
     *
     * @throws IOException
     * @throws InvalidJsonAssetException
     * @throws ClientProtocolException
     */
    @Test
    public void testPostAttachmentNoContent() throws ClientProtocolException, InvalidJsonAssetException, IOException {
        Attachment attachment = AssetUtils.getTestAttachmentNoContent();
        if (role.isAdmin()) {
            testContext.doPostAttachmentNoContent(createdPublishedAsset.get_id(), "theAttachment", attachment);
        } else {
            testContext.doPostBadAttachmentNoContent(createdPublishedAsset.get_id(), "theAttachment", attachment, RC_REJECT, null);
        }
    }

    /**
     * POST /assets/{assetId}/attachments Content-type: application/json
     *
     * Allowed for ADMIN
     *
     */
    @Test
    public void testPostAttachmentWithContent() throws ClientProtocolException, InvalidJsonAssetException, IOException {
        byte[] content = "I am the content.\nThere is not much to me.\n".getBytes(StandardCharsets.UTF_8);
        String name = "theAttachment";

        if (role.isAdmin()) {
            testContext.doPostAttachmentWithContent(createdPublishedAsset.get_id(),
                                                    name,
                                                    attachment,
                                                    content,
                                                    ContentType.APPLICATION_OCTET_STREAM);
        } else {
            testContext.doPostBadAttachmentWithContent(createdPublishedAsset.get_id(),
                                                       "I am the attachment!",
                                                       attachment,
                                                       content,
                                                       ContentType.APPLICATION_OCTET_STREAM,
                                                       RC_REJECT,
                                                       null);
        }
    }

    /**
     * DELETE /assets/{assetId}/attachments/{attachmentId}
     *
     * Allowed for ADMIN
     */
    @Test
    public void testDeleteAttachment() throws ClientProtocolException, IOException {
        if (role.isAdmin()) {
            testContext.doDeleteAttachment(createdPublishedAsset.get_id(), createdAttachment.get_id(), 204);
        } else {
            testContext.doDeleteAttachment(createdPublishedAsset.get_id(), createdAttachment.get_id(), RC_REJECT);
        }
    }

    /**
     * GET /assets/{assetId}/attachments/{attachmentId}/{name}
     *
     * Allowed for ADMIN and USER
     */
    @Test
    public void testGetAttachmentContent() throws IOException {
        if (role.isUser()) {
            byte[] returnedContent = testContext.doGetAttachmentContent(createAssetWithAttachments.get_id(), createdAttachment.get_id(), attachmentName);
            Assert.assertArrayEquals("Returned content should be equal to that which was uploaded", attachmentContent, returnedContent);
        } else {
            testContext.doGetAttachmentContentInError(createAssetWithAttachments.get_id(), createdAttachment.get_id(), attachmentName, RC_REJECT, null);
        }
    }

    /**
     * PUT /assets/{assetId}/state
     *
     * Allowed for ADMIN
     *
     */
    @Test
    public void testPutAssetState() throws InvalidJsonAssetException, IOException {
        if (role.isAdmin()) {
            testContext.updateAssetState(createdPublishedAsset.get_id(), Asset.StateAction.UNPUBLISH, 200);
        } else {
            testContext.updateAssetState(createdPublishedAsset.get_id(), Asset.StateAction.UNPUBLISH, RC_REJECT);
        }
    }

    /**
     * GET /assets/summary
     *
     * Allowed for ADMIN and USER
     */
    @Test
    public void testGetAssetSummary() throws Exception {
        if (role.isUser()) {
            testContext.getAssetSummary("fields=name");
        } else {
            testContext.getBadAssetSummary("fields=name", RC_REJECT);
        }
    }

    @Test
    public void testGetFilteredAssets() throws Exception {
        Asset asset1 = AssetUtils.getTestAsset();
        asset1.setProperty("type", "feature");
        Asset publishedAsset1 = adminContext.addAssetNoAttachmentsWithState(asset1, Asset.State.PUBLISHED);

        Asset asset2 = AssetUtils.getTestAsset();
        asset2.setProperty("type", "feature");
        Asset notPublishedAsset2 = adminContext.addAssetNoAttachments(asset2);

        Asset asset3 = AssetUtils.getTestAsset();
        asset3.setProperty("type", "product");
        adminContext.addAssetNoAttachmentsWithState(asset3, Asset.State.PUBLISHED);

        Asset asset4 = AssetUtils.getTestAsset();
        asset4.setProperty("type", "product");
        adminContext.addAssetNoAttachments(asset4);

        if (role.isAdmin()) {
            AssetList assets = testContext.getAllAssets("type=feature");
            assertEquals("Wrong number of assets", 2, assets.size());
            assertThat(assets, containsInAnyOrder(assetsWithIds(publishedAsset1, notPublishedAsset2)));
        } else if (role.isUser()) {
            AssetList assets = testContext.getAllAssets("type=feature");
            assertEquals("Wrong number of assets", 1, assets.size());
            assertEquals("The wrong asset came back", publishedAsset1.get_id(), assets.get(0).get_id());
        } else {
            testContext.doGetAllAssetsBad(RC_REJECT);
        }
    }

    @Test
    public void testSearchAssets() throws Exception {

        Asset asset1 = AssetUtils.getTestAsset();
        asset1.setProperty("name", "foo one");
        Asset publishedAsset1 = adminContext.addAssetNoAttachmentsWithState(asset1, Asset.State.PUBLISHED);

        Asset asset2 = AssetUtils.getTestAsset();
        asset2.setProperty("name", "foo two");
        Asset notPublishedAsset2 = adminContext.addAssetNoAttachments(asset2);

        Asset asset3 = AssetUtils.getTestAsset();
        asset3.setProperty("name", "nofoo three");
        adminContext.addAssetNoAttachmentsWithState(asset3, Asset.State.PUBLISHED);

        Asset asset4 = AssetUtils.getTestAsset();
        asset4.setProperty("name", "nofoo four");
        adminContext.addAssetNoAttachments(asset4);

        if (role.isAdmin()) {
            AssetList assets = testContext.getAllAssets("q=foo");
            assertEquals("Wrong number of assets", 2, assets.size());
            assertThat(assets, containsInAnyOrder(assetsWithIds(publishedAsset1, notPublishedAsset2)));
        } else if (role.isUser()) {
            AssetList assets = testContext.getAllAssets("q=foo");
            assertEquals("Wrong number of assets", 1, assets.size());
            assertEquals("The wrong asset came back", publishedAsset1.get_id(), assets.get(0).get_id());
        } else {
            testContext.doGetAllAssetsBad(RC_REJECT);
        }

    }
}
