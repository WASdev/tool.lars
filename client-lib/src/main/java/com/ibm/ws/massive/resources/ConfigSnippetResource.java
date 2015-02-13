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

import java.util.Collection;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;

public class ConfigSnippetResource extends MassiveResource {

    /*
     * ----------------------------------------------------------------------------------------------------
     * STATIC HELPER METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    /**
     *
     * @param userId
     * @param password
     * @param apiKey
     * @return
     * @throws RepositoryBackendException
     */
    public static Collection<ConfigSnippetResource> getAllConfigSnippets(LoginInfo loginInfo) throws RepositoryBackendException {
        return MassiveResource.getAllResources(Type.CONFIGSNIPPET, loginInfo);
    }

    /**
     *
     * @param userId
     * @param password
     * @param apiKey
     * @param id
     * @return
     * @throws RepositoryBackendException
     */
    public static ConfigSnippetResource getConfigSnippet(LoginInfoEntry loginInfoResource, String id) throws RepositoryBackendException, RepositoryBadDataException {
        return getResource(loginInfoResource, id);
    }

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public ConfigSnippetResource(LoginInfoEntry loginInfoResource) {
        super(loginInfoResource);
        setType(Type.CONFIGSNIPPET);
    }

    public void setAppliesTo(String appliesTo) {
        _asset.getWlpInformation().setAppliesTo(appliesTo);
    }

    public String getAppliesTo() {
        return _asset.getWlpInformation().getAppliesTo();
    }

    public void setRequireFeature(Collection<String> requireFeature) {
        _asset.getWlpInformation().setRequireFeature(requireFeature);
    }

    public Collection<String> getRequireFeature() {
        return _asset.getWlpInformation().getRequireFeature();
    }

    @Override
    public void copyFieldsFrom(MassiveResource fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        ConfigSnippetResource configSnipRes = (ConfigSnippetResource) fromResource;
        setAppliesTo(configSnipRes.getAppliesTo());
        setRequireFeature(configSnipRes.getRequireFeature());
    }

}
