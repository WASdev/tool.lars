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

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;

public class IfixResource extends MassiveResource implements WebDisplayable {

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
     * @throws IOException
     * @throws RepositoryBackendException
     */
    public static Collection<IfixResource> getAllIfixes(LoginInfo loginInfo) throws RepositoryBackendException {
        return MassiveResource.getAllResources(Type.IFIX, loginInfo);
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
    public static IfixResource getIfix(LoginInfoEntry loginInfoResource, String id) throws RepositoryBackendException, RepositoryBadDataException {
        return getResource(loginInfoResource, id);
    }

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public IfixResource(LoginInfoEntry loginInfoResource) {
        super(loginInfoResource);
        setType(Type.IFIX);
        setDisplayPolicy(DisplayPolicy.HIDDEN);
        setWebDisplayPolicy(DisplayPolicy.HIDDEN);
    }

    public void setProvideFix(Collection<String> provides) {
        _asset.getWlpInformation().setProvideFix(provides);
    }

    public Collection<String> getProvideFix() {
        return _asset.getWlpInformation().getProvideFix();
    }

    public void setAppliesTo(String appliesTo) {
        _asset.getWlpInformation().setAppliesTo(appliesTo);
    }

    public String getAppliesTo() {
        return _asset.getWlpInformation().getAppliesTo();
    }

    /**
     * The {@link DisplayPolicy} to use
     */
    public void setDisplayPolicy(DisplayPolicy policy) {
        _asset.getWlpInformation().setDisplayPolicy(policy == null ? null : policy.getWlpDisplayPolicy());
    }

    /**
     * Get the {@link DisplayPolicy}
     *
     * @return {@link DisplayPolicy} in use
     */
    @Deprecated
    public DisplayPolicy getDisplayPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return DisplayPolicy.lookup(_asset.getWlpInformation().getDisplayPolicy());
    }

    /**
     * The {@link DisplayPolicy} to use
     */
    @Override
    public void setWebDisplayPolicy(DisplayPolicy policy) {
        _asset.getWlpInformation().setWebDisplayPolicy(policy == null ? null : policy.getWlpDisplayPolicy());
    }

    /**
     * Get the {@link DisplayPolicy}
     *
     * @return {@link DisplayPolicy} in use
     */
    /* package */DisplayPolicy getWebDisplayPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return DisplayPolicy.lookup(_asset.getWlpInformation().getWebDisplayPolicy());
    }

    public Date getDate() {
        return _asset.getWlpInformation().getDate();
    }

    public void setDate(Date date) {
        _asset.getWlpInformation().setDate(date);
    }

    @Override
    public void copyFieldsFrom(MassiveResource fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        IfixResource iFixRes = (IfixResource) fromResource;
        setDisplayPolicy(iFixRes.getDisplayPolicy());
        setWebDisplayPolicy(iFixRes.getWebDisplayPolicy());
        setAppliesTo(iFixRes.getAppliesTo());
        setProvideFix(iFixRes.getProvideFix());
        setDate(iFixRes.getDate());
    }

}
