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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendIOException;

public class LoginInfoTest {

    private static final URL LOCAL_DEFAULT_PROPERTIES_URL;
    private static final File loginDir = new File("resources/login");

    static {
        // Initialize LOCAL_DEFAULT_PROPERTIES_URL, handling exception that won't be thrown...
        URL liveLoginUrl = null;
        try {
            liveLoginUrl = new File(loginDir, "liveLoginInfo.props").toURI().toURL();
        } catch (MalformedURLException e) {
            // Won't happen, URL is hard coded and valid
        } finally {
            LOCAL_DEFAULT_PROPERTIES_URL = liveLoginUrl;
        }
    }

    @Before
    public void clearCacheAndProperties() throws MalformedURLException {
        // Ensure that each test will re-read the repo properties so that
        // we can test changing which properties files is read using a
        // system property.
        LoginInfo.clearCachedRepoProperties();

        // Set the system property to read repo properties from a local file rather than fetching it from the default location on DHE.
        // Individual tests can override this before creating a LoginInfo object
        setRepoSystemProperty(LOCAL_DEFAULT_PROPERTIES_URL);
    }

    @AfterClass
    public static void clearAfterTests() throws MalformedURLException {
        // Clear the LoginInfo properties and reset the system property
        LoginInfo.clearCachedRepoProperties();
        setRepoSystemProperty(null);
    }

    @Test
    public void testNoArgs() throws RepositoryBackendIOException {
        LoginInfo loginInfo = new LoginInfo();
        assertEquals("https://asset-websphere.ibm.com/ma/v1", loginInfo.getRepositoryUrl());
        assertEquals("75621234192", loginInfo.getApiKey());
        assertNull(loginInfo.getUserId());
        assertNull(loginInfo.getPassword());
    }

    /**
     * Reset the system property and check that the URL points to the correct file on DHE.
     * <p>
     * We don't actually create a LoginInfo object in this test as we don't want to be making requests to DHE in our unit tests.
     */
    @Test
    public void testDefaultUrl() throws RepositoryBackendIOException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        setRepoSystemProperty(null);
        Method m = LoginInfoEntry.class.getDeclaredMethod("getPropertiesFileLocation", (Class<?>[]) null);
        m.setAccessible(true);
        URL url = (URL) m.invoke(null, (Object[]) null);
        assertEquals("Wrong default URL used",
                     "https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/assetservicelocation.props",
                     url.toString());
        // Also, in case the URL changes, the above check may be updated, and someone might change the URL to be bttp
        // rather than https so this extra test below is included to try and catch that.
        assertTrue("URL should start with HTTPS, if URL is updated (and above check updated too) it should be changed to an address that starts with HTTPS",
                   url.toString().toLowerCase().startsWith("https"));
    }

    @Test
    public void testArgs() throws RepositoryBackendIOException {
        LoginInfo loginInfo = new LoginInfo("a", "b", "c", "d", "e", "f");
        assertEquals("a", loginInfo.getUserId());
        assertEquals("b", loginInfo.getPassword());
        assertEquals("c", loginInfo.getApiKey());
        assertEquals("d", loginInfo.getRepositoryUrl());
        assertEquals("e", loginInfo.getSoftlayerUserId());
        assertEquals("f", loginInfo.getSoftlayerPassword());
    }

    @Test
    public void test4ArgsConstructor() throws RepositoryBackendIOException {
        LoginInfo loginInfo = new LoginInfo("a", "b", "c", "d");
        assertEquals("a", loginInfo.getUserId());
        assertEquals("b", loginInfo.getPassword());
        assertEquals("c", loginInfo.getApiKey());
        assertEquals("d", loginInfo.getRepositoryUrl());
        assertNull(loginInfo.getSoftlayerUserId());
        assertNull(loginInfo.getSoftlayerPassword());
    }

    @Test
    public void test8ArgsConstructor() {
        LoginInfo loginInfo = new LoginInfo("a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals("a", loginInfo.getUserId());
        assertEquals("b", loginInfo.getPassword());
        assertEquals("c", loginInfo.getApiKey());
        assertEquals("d", loginInfo.getRepositoryUrl());
        assertEquals("e", loginInfo.getSoftlayerUserId());
        assertEquals("f", loginInfo.getSoftlayerPassword());
        assertEquals("g", loginInfo.getAttachmentBasicAuthUserId());
        assertEquals("h", loginInfo.getAttachmentBasicAuthPassword());
    }

    /**
     * Ensure that when all the pre release test parameters are removed we get no errors
     *
     * @throws MalformedURLException
     * @throws RepositoryBackendIOException
     */
    @Test
    public void testGsaCredentialsSet() throws MalformedURLException, RepositoryBackendIOException {
        setRepoSystemProperty(new File(loginDir, "gsaLogin.props").toURI().toURL());

        LoginInfo loginInfo = new LoginInfo();
        assertEquals("<user>", loginInfo.getUserId());
        assertEquals("<pwd>", loginInfo.getPassword());
        assertEquals("1234", loginInfo.getApiKey());
        assertEquals("https://blah.ibm.com/ma/v1", loginInfo.getRepositoryUrl());
        assertEquals("<SoftlayerUserId>", loginInfo.getSoftlayerUserId());
        assertEquals("<SoftlayerPassword>", loginInfo.getSoftlayerPassword());
        assertEquals("<gsaUserId>", loginInfo.getAttachmentBasicAuthUserId());
        assertEquals("<gsaPassword>", loginInfo.getAttachmentBasicAuthPassword());
    }

    /**
     * Ensure that when all the pre release test parameters are removed we get no errors
     *
     * @throws MalformedURLException
     * @throws RepositoryBackendIOException
     */
    @Test
    public void testPostReleasePropertiesFile() throws MalformedURLException, RepositoryBackendIOException {
        setRepoSystemProperty(new File(loginDir, "postReleaseLoginInfo.props").toURI().toURL());

        LoginInfo loginInfo = new LoginInfo();
        assertEquals("user", loginInfo.getUserId());
        assertEquals("pwd", loginInfo.getPassword());
        assertEquals("1234", loginInfo.getApiKey());
        assertEquals("http://foo/bla", loginInfo.getRepositoryUrl());
        assertEquals(null, loginInfo.getSoftlayerUserId());
        assertEquals(null, loginInfo.getSoftlayerPassword());
    }

    @Test
    public void testSystemPropertyOverride() throws RepositoryBackendIOException, MalformedURLException {
        setRepoSystemProperty(new File(loginDir, "testLoginInfo.props").toURI().toURL());

        LoginInfo loginInfo = new LoginInfo();
        assertEquals("x", loginInfo.getUserId());
        assertEquals("y", loginInfo.getPassword());
        assertEquals("1234", loginInfo.getApiKey());
        assertEquals("http://foo/bla", loginInfo.getRepositoryUrl());
        assertEquals("amelia", loginInfo.getSoftlayerUserId());
        assertEquals("ariana", loginInfo.getSoftlayerPassword());
    }

    @Test
    public void testRepositoryDescriptionFileExists() throws RepositoryBackendIOException, MalformedURLException {
        assertTrue(LoginInfoEntry.repositoryDescriptionFileExists());

        setRepoSystemProperty(new File(loginDir, "non-existent.props").toURI().toURL());

        assertFalse(LoginInfoEntry.repositoryDescriptionFileExists());
    }

    private static void setRepoSystemProperty(final URL url) {
        AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                if (url == null) {
                    System.clearProperty(LoginInfoEntry.LOCATION_OVERRIDE_SYS_PROP_NAME);
                } else {
                    System.setProperty(LoginInfoEntry.LOCATION_OVERRIDE_SYS_PROP_NAME, url.toString());
                }
                return null;
            }
        });
    }

    @Test
    public void testSoftLayer() throws RepositoryBackendIOException, MalformedURLException {

        LoginInfo loginInfo = new LoginInfo();
        assertNull(loginInfo.getSoftlayerUserId());
        assertNull(loginInfo.getSoftlayerPassword());

        assertNull(loginInfo.getClientLoginInfo().getSoftlayerUserId());
        assertNull(loginInfo.getClientLoginInfo().getSoftlayerPassword());

        loginInfo.setSoftlayerUserId("amelia");
        loginInfo.setSoftlayerPassword("ariana");
        assertEquals("amelia", loginInfo.getClientLoginInfo().getSoftlayerUserId());
        assertEquals("ariana", loginInfo.getClientLoginInfo().getSoftlayerPassword());
    }

    @Test
    public void testClear() throws RepositoryBackendIOException, MalformedURLException {
        try {

            setRepoSystemProperty(new File(loginDir, "testLoginInfo.props").toURI().toURL());

            LoginInfo loginInfo = new LoginInfo();
            assertEquals("1234", loginInfo.getApiKey());

        } finally {
            setRepoSystemProperty(LOCAL_DEFAULT_PROPERTIES_URL);
        }

        LoginInfo loginInfo = new LoginInfo();
        assertEquals("1234", loginInfo.getApiKey());

        LoginInfo.clearCachedRepoProperties();

        loginInfo = new LoginInfo();
        assertEquals("75621234192", loginInfo.getApiKey());
    }

    @Test
    public void testInvalidProps() throws RepositoryBackendIOException, MalformedURLException {
        try {

            setRepoSystemProperty(new File(loginDir, "invalidLoginInfo.props").toURI().toURL());

            try {
                new LoginInfo();
                fail("expecting IllegalArgumentException apiKey");
            } catch (IllegalArgumentException e) {
                // expected
            }

        } finally {
            setRepoSystemProperty(LOCAL_DEFAULT_PROPERTIES_URL);
        }

        LoginInfo loginInfo = new LoginInfo();
        assertEquals("75621234192", loginInfo.getApiKey());
    }

}
