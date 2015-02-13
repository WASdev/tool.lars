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

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a list of LoginInfoEntry objects used to connect to repositories.
 * Do not use a LoginInfoEntry inside multiple LoginInfo objects as setting a userAgent in the LoginInfo object
 * will update each LoginInfoEntry it contains.
 */
public class LoginInfo extends ArrayList<LoginInfoEntry> {

    /** SerialVesionUID */
    private static final long serialVersionUID = -4525841713558054978L;

    private String _userAgent;

    /**
     * Creates the collection of LoginInfoEntry objects, populating it with the default LoginInfoEntry
     *
     * @throws RepositoryBackendIOException
     */
    public LoginInfo() throws RepositoryBackendIOException {
        add(new LoginInfoEntry());
    }

    /**
     * Creates the LoginInfo list populating it with the contents of the supplied list. This can be used with an empty
     * list to create a LoginInfo with no entries in it.
     *
     * @param list
     */
    public LoginInfo(List<LoginInfoEntry> list) {
        addAll(list);
    }

    /**
     * Creates the collection of LoginInfoEntry objects, populating it with the supplied LoginInfoEntry
     *
     * @throws RepositoryBackendIOException
     */
    public LoginInfo(LoginInfoEntry LoginInfoEntry) {
        add(LoginInfoEntry);
    }

    /**
     * Creates the collection of LoginInfoEntry objects, populating it with a LoginInfoEntry created from the supplied parameters
     *
     * @throws RepositoryBackendIOException
     */
    public LoginInfo(String userId, String password, String apiKey, String repositoryUrl,
                     String softlayerUserId, String softlayerPassword, String attachmentBasicAuthUserId, String gsaPassword) {
        add(new LoginInfoEntry(userId, password, apiKey, repositoryUrl, softlayerUserId, softlayerPassword, attachmentBasicAuthUserId, gsaPassword));
    }

    /**
     * Creates the collection of LoginInfoEntry objects, populating it with a LoginInfoEntry created from the supplied parameters
     *
     * @throws RepositoryBackendIOException
     */
    public LoginInfo(String userId, String password, String apiKey, String repositoryUrl,
                     String softlayerUserId, String softlayerPassword) {
        this(userId, password, apiKey, repositoryUrl, softlayerUserId, softlayerPassword, null, null);
    }

    /**
     * Creates the collection of LoginInfoEntry objects, populating it with a LoginInfoEntry created from the supplied parameters
     *
     * @throws RepositoryBackendIOException
     */
    public LoginInfo(String userId, String password, String apiKey, String repositoryUrl) {
        this(userId, password, apiKey, repositoryUrl, null, null, null, null);
    }

    /**
     * Adds a new LoginInfoEntry - the userAgent of the LoginInfo object will automatically be pushed into
     * the LoginInfoEntry {@inheritDoc}
     */
    @Override
    public boolean add(LoginInfoEntry log) {
        boolean ret = super.add(log);
        log.setUserAgent(_userAgent);
        return ret;
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the userId from the first
     * LoginInfoEntry in the collection
     *
     * @return The userId of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public String getUserId() {
        return get(0).getUserId();
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the userId into the first
     * LoginInfoEntry in the collection
     *
     * @param userId The userId to put into the first LoginInfoEntry in the collection
     */
    @Deprecated
    public void setUserId(String userId) {
        get(0).setUserId(userId);
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the password from the first
     * LoginInfoEntry in the collection
     *
     * @return The password of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public String getPassword() {
        return get(0).getPassword();
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the password into the first
     * LoginInfoEntry in the collection
     *
     * @param userId The password to put into the first LoginInfoEntry in the collection
     */
    @Deprecated
    public void setPassword(String password) {
        get(0).setPassword(password);
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the apiKey from the first
     * LoginInfoEntry in the collection
     *
     * @return The apiKey of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public String getApiKey() {
        return get(0).getApiKey();
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the apiKey into the first
     * LoginInfoEntry in the collection
     *
     * @param userId The apiKey to put into the first LoginInfoEntry in the collection
     */
    @Deprecated
    public void setApiKey(String apiKey) {
        get(0).setApiKey(apiKey);
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the repositoyUrl from the first
     * LoginInfoEntry in the collection
     *
     * @return The repositoryUrl of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public String getRepositoryUrl() {
        return get(0).getRepositoryUrl();
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the repositoryUrl into the first
     * LoginInfoEntry in the collection
     *
     * @param userId The repositoryUrl to put into the first LoginInfoEntry in the collection
     */
    @Deprecated
    public void setRepositoryUrl(String repositoryUrl) {
        get(0).setRepositoryUrl(repositoryUrl);
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the softLayerUserId from the first
     * LoginInfoEntry in the collection
     *
     * @return The softLayerUserId of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public String getSoftlayerUserId() {
        return get(0).getSoftlayerUserId();
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the softlayerUserId into the first
     * LoginInfoEntry in the collection
     *
     * @param userId The softlayerUserId to put into the first LoginInfoEntry in the collection
     */
    @Deprecated
    public void setSoftlayerUserId(String softlayerUserId) {
        get(0).setSoftlayerUserId(softlayerUserId);
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the softLayerPassword from the first
     * LoginInfoEntry in the collection
     *
     * @return The softLayerPassword of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public String getSoftlayerPassword() {
        return get(0).getSoftlayerPassword();
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the softlayerPassword into the first
     * softlayerPassword in the collection
     *
     * @param userId The softlayerUserId to put into the first LoginInfoEntry in the collection
     */
    @Deprecated
    public void setSoftlayerPassword(String softlayerPassword) {
        get(0).setSoftlayerPassword(softlayerPassword);
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the attachmentBasicAuthUserId from the first
     * LoginInfoEntry in the collection
     *
     * @return The attachmentBasicAuthUserId of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public String getAttachmentBasicAuthUserId() {
        return get(0).getAttachmentBasicAuthUserId();
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the attachmentBasicAuthUserId into the first
     * softlayerPassword in the collection
     *
     * @param userId The attachmentBasicAuthUserId to put into the first LoginInfoEntry in the collection
     */
    @Deprecated
    public void setAttachmentBasicAuthUserId(String attachmentBasicAuthUserId) {
        get(0).setAttachmentBasicAuthUserId(attachmentBasicAuthUserId);
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the attachmentBasicAuthPassword from the first
     * LoginInfoEntry in the collection
     *
     * @return The attachmentBasicAuthPassword of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public String getAttachmentBasicAuthPassword() {
        return get(0).getAttachmentBasicAuthPassword();
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the attachmentBasicAuthPassword into the first
     * softlayerPassword in the collection
     *
     * @param userId The attachmentBasicAuthPassword to put into the first LoginInfoEntry in the collection
     */
    @Deprecated
    public void setAttachmentBasicAuthPassword(String attachmentBasicAuthPassword) {
        get(0).setAttachmentBasicAuthPassword(attachmentBasicAuthPassword);
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the proxy into the first
     * softlayerPassword in the collection
     *
     * @param userId The proxy to put into the first LoginInfoEntry in the collection
     */
    @Deprecated
    public void setProxy(LoginInfoProxy proxy) {
        get(0).setProxy(proxy);
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the proxy from the first
     * LoginInfoEntry in the collection
     *
     * @return The proxy of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public LoginInfoProxy getProxy() {
        return get(0).getProxy();
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return _userAgent;
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will set the userAgent into all the
     * the LoginInfoEntrys in the collection.
     *
     * @param userId The userAgent to put into the first LoginInfoEntry in the collection
     */
    public void setUserAgent(String userAgent) {
        for (LoginInfoEntry r : this) {
            r.setUserAgent(userAgent);
        }
    }

    /**
     * This object is used to hold a collection of {@link LoginInfoEntry} objects, calling this method will return the client level LoginInfoClient object from the first
     * LoginInfoEntry in the collection
     *
     * @return The LoginInfoClient of the first LoginInfoEntry in the collection
     */
    @Deprecated
    public com.ibm.ws.massive.sa.client.ClientLoginInfo getClientLoginInfo() {
        return get(0).getClientLoginInfo();
    }

    /**
     * Clears the cached repository properties file so it will be re-read on the next create
     * Deprecated, please use the same method on the {@link LoginInfoEntry} object
     */
    @Deprecated
    public static void clearCachedRepoProperties() {
        LoginInfoEntry.clearCachedRepoProperties();
    }

}
