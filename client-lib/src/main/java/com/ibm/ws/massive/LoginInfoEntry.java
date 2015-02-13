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

package com.ibm.ws.massive;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

public class LoginInfoEntry {

    private static volatile Properties repoProperties;

    private String repositoryUrl;
    private String userId;
    private String password;
    private String apiKey;

    private String softlayerUserId;
    private String softlayerPassword;

    private String attachmentBasicAuthUserId;
    private String attachmentBasicAuthPassword;

    private String userAgent;

    private LoginInfoProxy proxy;

    // System property to override the default properties file location
    public static final String LOCATION_OVERRIDE_SYS_PROP_NAME = "repository.description.url";

    // The default location for the properties file
    private static final String DEFAULT_PROPERTIES_FILE_LOCATION = "https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/assetservicelocation.props";

    // Keys in the properties file
    private static final String REPOSITORY_URL_PROP = "repository.url";
    private static final String API_KEY_PROP = "apiKey";
    private static final String USERID_PROP = "userId";
    private static final String PASSWORD_PROP = "password";
    private static final String SOFTLAYER_USERID_PROP = "softlayerUserId";
    private static final String SOFTLAYER_PASSWORD_PROP = "softlayerPassword";
    private static final String ATTACHMENT_BASIC_AUTH_USERID_PROP = "attachmentBasicAuthUserId";
    private static final String ATTACHMENT_BASIC_AUTH_PASSWORD_PROP = "attachmentBasicAuthPassword";

    /**
     * Tests if the repository description properties file exists as defined by the location override system property or at the default location
     *
     * @return true if the properties file exists, otherwise false
     */
    public static boolean repositoryDescriptionFileExists() {
        boolean exixts = false;
        try {

            URL propertiesFileURL = getPropertiesFileLocation();
            InputStream is = propertiesFileURL.openStream();
            exixts = true;
            is.close();

        } catch (MalformedURLException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        return exixts;
    }

    /**
     * Clears the cached repository properties file so it will be re-read on the next create
     */
    public static void clearCachedRepoProperties() {
        LoginInfoEntry.repoProperties = null;
    }

    public LoginInfoEntry() throws RepositoryBackendIOException {

        readRepoProperties();

        this.repositoryUrl = repoProperties.getProperty(REPOSITORY_URL_PROP).trim();
        this.apiKey = repoProperties.getProperty(API_KEY_PROP).trim();

        if (repoProperties.containsKey(USERID_PROP)) {
            this.userId = repoProperties.getProperty(USERID_PROP).trim();
        }
        if (repoProperties.containsKey(PASSWORD_PROP)) {
            this.password = repoProperties.getProperty(PASSWORD_PROP).trim();
        }
        if (repoProperties.containsKey(SOFTLAYER_USERID_PROP)) {
            this.softlayerUserId = repoProperties.getProperty(SOFTLAYER_USERID_PROP).trim();
        }
        if (repoProperties.containsKey(SOFTLAYER_PASSWORD_PROP)) {
            this.softlayerPassword = repoProperties.getProperty(SOFTLAYER_PASSWORD_PROP).trim();
        }
        if (repoProperties.containsKey(ATTACHMENT_BASIC_AUTH_USERID_PROP)) {
            this.attachmentBasicAuthUserId = repoProperties.getProperty(ATTACHMENT_BASIC_AUTH_USERID_PROP).trim();
        }
        if (repoProperties.containsKey(ATTACHMENT_BASIC_AUTH_PASSWORD_PROP)) {
            this.attachmentBasicAuthPassword = repoProperties.getProperty(ATTACHMENT_BASIC_AUTH_PASSWORD_PROP).trim();
        }
    }

    public LoginInfoEntry(String userId, String password, String apiKey, String repositoryUrl,
                          String softlayerUserId, String softlayerPassword, String attachmentBasicAuthUserId, String gsaPassword) {
        this.userId = userId;
        this.password = password;
        this.apiKey = apiKey;
        this.repositoryUrl = repositoryUrl;
        this.softlayerUserId = softlayerUserId;
        this.softlayerPassword = softlayerPassword;
        this.attachmentBasicAuthUserId = attachmentBasicAuthUserId;
        this.attachmentBasicAuthPassword = gsaPassword;
    }

    public LoginInfoEntry(String userId, String password, String apiKey, String repositoryUrl,
                          String softlayerUserId, String softlayerPassword) {
        this(userId, password, apiKey, repositoryUrl, softlayerUserId, softlayerPassword, null, null);
    }

    public LoginInfoEntry(String userId, String password, String apiKey, String repositoryUrl) {
        this(userId, password, apiKey, repositoryUrl, null, null, null, null);
    }

    private void readRepoProperties() throws RepositoryBackendIOException {
        if (LoginInfoEntry.repoProperties == null) {
            synchronized (LoginInfoEntry.class) {
                if (LoginInfoEntry.repoProperties == null) {
                    try {
                        URL propertiesFileURL = getPropertiesFileLocation();
                        Reader reader = new InputStreamReader(propertiesFileURL.openStream(), "UTF-8");
                        Properties props = new Properties();
                        try {
                            props.load(reader);
                        } finally {
                            reader.close();
                        }

                        if (!!!props.containsKey(REPOSITORY_URL_PROP)) {
                            throw new IllegalArgumentException(REPOSITORY_URL_PROP);
                        }
                        if (!!!props.containsKey(API_KEY_PROP)) {
                            throw new IllegalArgumentException(API_KEY_PROP);
                        }

                        LoginInfoEntry.repoProperties = props;

                    } catch (IOException e) {
                        throw new RepositoryBackendIOException(e);
                    }
                }
            }
        }
    }

    private static URL getPropertiesFileLocation() throws MalformedURLException {
        final String overrideLocation = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(LOCATION_OVERRIDE_SYS_PROP_NAME);
            }
        });

        URL url;
        try {
            url = AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                public URL run() throws MalformedURLException {
                    if (overrideLocation != null && overrideLocation.length() > 0) {
                        return new URL(overrideLocation);
                    } else {
                        return new URL(DEFAULT_PROPERTIES_FILE_LOCATION);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw (MalformedURLException) e.getCause();
        }

        return url;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getSoftlayerUserId() {
        return softlayerUserId;
    }

    public void setSoftlayerUserId(String softlayerUserId) {
        this.softlayerUserId = softlayerUserId;
    }

    public String getSoftlayerPassword() {
        return softlayerPassword;
    }

    public void setSoftlayerPassword(String softlayerPassword) {
        this.softlayerPassword = softlayerPassword;
    }

    public String getAttachmentBasicAuthUserId() {
        return attachmentBasicAuthUserId;
    }

    public void setAttachmentBasicAuthUserId(String attachmentBasicAuthUserId) {
        this.attachmentBasicAuthUserId = attachmentBasicAuthUserId;
    }

    public String getAttachmentBasicAuthPassword() {
        return attachmentBasicAuthPassword;
    }

    public void setAttachmentBasicAuthPassword(String attachmentBasicAuthPassword) {
        this.attachmentBasicAuthPassword = attachmentBasicAuthPassword;
    }

    public com.ibm.ws.massive.sa.client.ClientLoginInfo getClientLoginInfo() {
        com.ibm.ws.massive.sa.client.ClientLoginInfo clientLogin =
                        new com.ibm.ws.massive.sa.client.ClientLoginInfo(userId, password, apiKey, repositoryUrl,
                                        softlayerUserId, softlayerPassword, attachmentBasicAuthUserId,
                                        attachmentBasicAuthPassword, userAgent);
        if (proxy != null) {
            clientLogin.setProxy(proxy.getLoginInfoClientProxy());
        }
        return clientLogin;
    }

    public void setProxy(LoginInfoProxy proxy) {
        this.proxy = proxy;
    }

    public LoginInfoProxy getProxy() {
        return this.proxy;
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * <p>Sets the user agent that is being used to access the Massive client. This follows the HTTP user-agent header specification here:</p>
     * <p><a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.43">http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.43</a></p>
     * <p>For example:</p>
     * <code>com.ibm.ws.st.ui/8.5.5.4</code>
     *
     * @param userAgent the userAgent to set
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
