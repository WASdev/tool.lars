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
import static org.junit.Assert.assertEquals;

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

    @Rule
    public RepositoryContext userContext;

    /**
     * URL for the test instance where read operations are restricted to users with the User role.
     */
    private static final String RESTRICTED_URL = RepositoryContext.DEFAULT_URL;

    /**
     * URL for the test instance where read operations are unrestricted.
     * <p>
     * Write operations are still restricted to users with the Admin role.
     */
    private static final String UNRESTRICTED_URL = "http://localhost:" + FatUtils.LIBERTY_PORT + "/unrestricted" + FatUtils.LARS_APPLICATION_ROOT;

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
    private Asset createdAsset;

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
            data.add(new Object[] { RESTRICTED_URL, user.username, user.password, user.restrictedConfigRole, user + " - restricted" });
            data.add(new Object[] { UNRESTRICTED_URL, user.username, user.password, user.unrestrictedConfigRole, user + " - unrestricted" });
        }
        System.out.println("sending data: " + data.size());
        return data;
    }

    /**
     * Process the test parameters and set up the adminContext and userContext.
     */
    public PermissionTest(String url, String username, String password, Role role, String label) {
        adminContext = RepositoryContext.createAsAdmin(url, true);
        userContext = new RepositoryContext(url, username, password, false);
        this.role = role;
    }

    @Before
    public void setUp() throws IOException, InvalidJsonAssetException {
        testAsset = AssetUtils.getTestAsset();
        createdAsset = adminContext.addAssetNoAttachments(testAsset);

        assetWithAttachments = AssetUtils.getTestAsset();
        createAssetWithAttachments = adminContext.addAssetNoAttachments(assetWithAttachments);
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
        if (role.isUser()) {
            AssetList assets = userContext.doGetAllAssets(200);
            assertEquals("Wrong number of assets", 2, assets.size());
        } else {
            userContext.doGetAllAssetsBad(RC_REJECT);
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
            Asset returnedAsset = userContext.addAssetNoAttachments(asset);
            AssetUtils.assertUploadedAssetEquivalentToOriginal("Returned asset should match the asset that was uploaded", asset, returnedAsset);
        } else {
            userContext.addBadAsset(asset, RC_REJECT);
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
            Asset returnedAsset = userContext.getAsset(createdAsset.get_id());
            AssetUtils.assertUploadedAssetEquivalentToOriginal("Returned asset should match the asset that was uploaded", testAsset, returnedAsset);
        } else {
            userContext.getBadAsset(createdAsset.get_id(), RC_REJECT);
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
            userContext.deleteAsset(createdAsset.get_id(), 204);
        } else {
            userContext.deleteAsset(createdAsset.get_id(), RC_REJECT);
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
            userContext.doPostAttachmentNoContent(createdAsset.get_id(), "theAttachment", attachment);
        } else {
            userContext.doPostBadAttachmentNoContent(createdAsset.get_id(), "theAttachment", attachment, RC_REJECT, null);
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
            userContext.doPostAttachmentWithContent(createdAsset.get_id(),
                                                    name,
                                                    attachment,
                                                    content,
                                                    ContentType.APPLICATION_OCTET_STREAM);
        } else {
            userContext.doPostBadAttachmentWithContent(createdAsset.get_id(),
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
            userContext.doDeleteAttachment(createdAsset.get_id(), createdAttachment.get_id(), 204);
        } else {
            userContext.doDeleteAttachment(createdAsset.get_id(), createdAttachment.get_id(), RC_REJECT);
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
            byte[] returnedContent = userContext.doGetAttachmentContent(createAssetWithAttachments.get_id(), createdAttachment.get_id(), attachmentName);
            Assert.assertArrayEquals("Returned content should be equal to that which was uploaded", attachmentContent, returnedContent);
        } else {
            userContext.doGetAttachmentContentInError(createAssetWithAttachments.get_id(), createdAttachment.get_id(), attachmentName, RC_REJECT, null);
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
            userContext.updateAssetState(createdAsset.get_id(), Asset.StateAction.PUBLISH.getValue(), 200);
        } else {
            userContext.updateAssetState(createdAsset.get_id(), Asset.StateAction.PUBLISH.getValue(), RC_REJECT);
        }
    }
}
