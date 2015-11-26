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

package com.ibm.ws.repository.resources.internal;

import java.util.Collection;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.resources.writeable.SampleResourceWritable;
import com.ibm.ws.repository.transport.model.Asset;

public class SampleResourceImpl extends RepositoryResourceImpl implements SampleResourceWritable {

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public SampleResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    public SampleResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        super(repoConnection, ass);
    }

    @Override
    public void setAppliesTo(String appliesTo) {
        _asset.getWlpInformation().setAppliesTo(appliesTo);
    }

    /** {@inheritDoc} */
    @Override
    public String getAppliesTo() {
        return _asset.getWlpInformation().getAppliesTo();
    }

    /** {@inheritDoc} */
    @Override
    public void setRequireFeature(Collection<String> requireFeature) {
        _asset.getWlpInformation().setRequireFeature(requireFeature);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getRequireFeature() {
        return _asset.getWlpInformation().getRequireFeature();
    }

    // Make setType public as this represents two different types in Massive
    @Override
    public void setType(ResourceType type) {
        super.setType(type);
    }

    /** {@inheritDoc} */
    @Override
    public void setShortName(String shortName) {
        _asset.getWlpInformation().setShortName(shortName);
    }

    /** {@inheritDoc} */
    @Override
    public String getShortName() {
        return _asset.getWlpInformation().getShortName();
    }

    /**
     * Gets a lower case version of the {@link #getIbmShortName()}.
     * 
     * @return
     */
    public String getLowerCaseShortName() {
        return _asset.getWlpInformation().getLowerCaseShortName();
    }

    @Override
    protected void copyFieldsFrom(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        SampleResourceImpl sampleRes = (SampleResourceImpl) fromResource;
        setAppliesTo(sampleRes.getAppliesTo());
        setRequireFeature(sampleRes.getRequireFeature());
        setShortName(sampleRes.getShortName());
    }

}
