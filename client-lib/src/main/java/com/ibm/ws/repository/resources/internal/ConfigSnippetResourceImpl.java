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
import com.ibm.ws.repository.resources.writeable.ConfigSnippetResourceWritable;
import com.ibm.ws.repository.transport.model.Asset;

public class ConfigSnippetResourceImpl extends RepositoryResourceImpl implements ConfigSnippetResourceWritable {

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public ConfigSnippetResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    public ConfigSnippetResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        super(repoConnection, ass);
        if (ass == null) {
            setType(ResourceType.CONFIGSNIPPET);
        }
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

    @Override
    protected void copyFieldsFrom(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        ConfigSnippetResourceImpl configSnipRes = (ConfigSnippetResourceImpl) fromResource;
        setAppliesTo(configSnipRes.getAppliesTo());
        setRequireFeature(configSnipRes.getRequireFeature());
    }

}
