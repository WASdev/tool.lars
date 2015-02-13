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

public class SampleResource extends MassiveResource {

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
    public static Collection<SampleResource> getAllSamples(LoginInfo loginInfo) throws RepositoryBackendException {
        Collection<SampleResource> samples = MassiveResource.getAllResources(Type.OPENSOURCE, loginInfo);
        Collection<SampleResource> productSamples = MassiveResource.getAllResources(Type.PRODUCTSAMPLE, loginInfo);
        samples.addAll(productSamples);
        return samples;
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
    public static SampleResource getSample(LoginInfoEntry loginInfoResource, String id) throws RepositoryBackendException, RepositoryBadDataException {
        return getResource(loginInfoResource, id);
    }

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public SampleResource(LoginInfoEntry loginInfoResource) {
        super(loginInfoResource);
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

    // Make setType public as this represents two different types in Massive
    @Override
    public void setType(Type type) {
        super.setType(type);
    }

    @Override
    protected void copyFieldsFrom(MassiveResource fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        SampleResource sampleRes = (SampleResource) fromResource;
        setAppliesTo(sampleRes.getAppliesTo());
        setRequireFeature(sampleRes.getRequireFeature());
    }

}
