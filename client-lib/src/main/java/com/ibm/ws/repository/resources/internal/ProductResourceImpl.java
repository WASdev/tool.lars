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
import java.util.List;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.AttachmentResource;
import com.ibm.ws.repository.resources.writeable.ProductResourceWritable;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.FilterVersion;
import com.ibm.ws.repository.transport.model.WlpInformation;

/**
 * This class represents INSTALL and ADDON resource types.
 */
public class ProductResourceImpl extends ProductRelatedResourceImpl implements ProductResourceWritable {

    /*
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public ProductResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    public ProductResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        super(repoConnection, ass);
    }

    @Override
    public RepositoryResourceMatchingData createMatchingData() {
        ExtendedMatchingData matchingData = new ExtendedMatchingData();
        matchingData.setType(getType());
        matchingData.setName(getName());
        matchingData.setProviderName(getProviderName());

        if ((getType().equals(ResourceType.INSTALL))) {
            matchingData.setVersion(getProductVersion());
        } else {
            // Regen the appliesToFilterInfo as the level of code that generated each resource may
            // be different and give us different results so regen it now.
            try {
                List<AppliesToFilterInfo> atfi = generateAppliesToFilterInfoList(false);
                matchingData.setAtfi(atfi);
            } catch (RepositoryResourceCreationException e) {
                // This should only be thrown if validate editions is set to true, for us its set to false
            }
        }
        return matchingData;
    }

    @Override
    protected String getVersionForVanityUrl() {
        Collection<AppliesToFilterInfo> filters = null;
        String version = "";

        if (getType().equals(ResourceType.ADDON)) {
            WlpInformation wlp = _asset.getWlpInformation();
            if (wlp != null) {
                filters = wlp.getAppliesToFilterInfo();
            }
        } else {
            filters = AppliesToProcessor.parseAppliesToHeader(getProductId() + "; productEdition="
                                                              + getProductEdition() +
                                                              "; productVersion=" + getProductVersion());
        }

        if (filters != null && !filters.isEmpty()) {
            AppliesToFilterInfo atfi = filters.iterator().next();
            if (atfi != null) {
                FilterVersion ver = atfi.getMinVersion();
                if (ver != null) {
                    version = ver.getLabel();
                }
            }
        }

        return version;
    }

    @Override
    protected String getNameForVanityUrl() {
        String name = getName();
        try {
            AttachmentResource attach = getMainAttachment();
            if (attach != null) {
                name = attach.getName();
                if (name != null && !name.isEmpty()) {
                    int index = name.lastIndexOf("-");
                    if (index != -1) {
                        name = name.substring(0, index);
                    }
                }
            }
        } catch (RepositoryBackendException e) {
            return getName();
        } catch (RepositoryResourceException e) {
            return getName();
        }

        return name;
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

    @Override
    protected void copyFieldsFrom(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        ProductResourceImpl prodRes = (ProductResourceImpl) fromResource;
        setAppliesTo(prodRes.getAppliesTo());
    }

    @Override
    public void setType(ResourceType type) {
        super.setType(type);
    }

    /*
     * This section is a temporary hack to put the product edition into the applies to Filter
     */
    @Override
    public void updateGeneratedFields(boolean performEditionChecking) throws RepositoryResourceCreationException {
        super.updateGeneratedFields(performEditionChecking);
        if (getType() == ResourceType.INSTALL) {
            generateAppliesToFilterInfo();
        }
    }

    public void generateAppliesToFilterInfo() {
        List<AppliesToFilterInfo> filter = AppliesToProcessor.parseAppliesToHeader(getProductId() + "; productEdition="
                                                                                   + getProductEdition() +
                                                                                   "; productVersion=" + getProductVersion());
        _asset.getWlpInformation().setAppliesToFilterInfo(filter);
    }

    /*
     * End of Edition hack
     */
}
