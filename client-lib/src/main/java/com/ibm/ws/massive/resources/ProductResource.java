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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.RepositoryException;
import com.ibm.ws.massive.sa.client.model.AppliesToFilterInfo;
import com.ibm.ws.massive.sa.client.model.FilterVersion;
import com.ibm.ws.massive.sa.client.model.WlpInformation;

/**
 * This class represents INSTALL and ADDON resource types.
 */
public class ProductResource extends ProductRelatedResource {

    /*
     * ----------------------------------------------------------------------------------------------------
     * STATIC HELPER METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    /**
     * This will obtain the addons from the repository that match the specified product definition
     *
     * @param loginInfo The connection information where the resources should be obtained from.
     * @param definition The product definition to match against
     * @return Returns a Collection of {@link ProductResource} objects that represent the addons that match the product
     *         definition supplied
     * @throws RepositoryBackendException If there was a problem connecting to the repository
     */
    public static Collection<ProductResource> getMatchingAddons(LoginInfo loginInfo, ProductDefinition definition) throws RepositoryBackendException {
        Collection<ProductResource> addonList = MassiveResource.getAllResources(Type.ADDON, loginInfo);
        Collection<ProductResource> matchedAddons = new ArrayList<ProductResource>();
        for (ProductResource addon : addonList) {
            if (addon.matches(definition) == MatchResult.MATCHED) {
                matchedAddons.add(addon);
            }
        }
        return matchedAddons;
    }

    /**
     *
     * @param userId
     * @param password
     * @param apiKey
     * @throws RepositoryBackendException
     */
    public static Collection<ProductResource> getAllProducts(LoginInfo loginInfo) throws RepositoryBackendException {
        Collection<ProductResource> product = MassiveResource.getAllResources(Type.INSTALL, loginInfo);
        Collection<ProductResource> addons = MassiveResource.getAllResources(Type.ADDON, loginInfo);
        product.addAll(addons);
        return product;
    }

    /**
     * Get all products with a given license type
     *
     * @param License type
     * @param userId
     * @param password
     * @param apiKey
     * @throws RepositoryException
     */
    public static Collection<ProductResource> getAllProducts(LicenseType licenseType, LoginInfo loginInfo) throws RepositoryBackendException {
        Collection<ProductResource> product = MassiveResource.getAllResources(licenseType, Type.INSTALL, loginInfo);
        Collection<ProductResource> addons = MassiveResource.getAllResources(licenseType, Type.ADDON, loginInfo);
        product.addAll(addons);
        return product;
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
    public static ProductResource getProduct(LoginInfoEntry loginInfoResource, String id) throws RepositoryBackendException, RepositoryBadDataException {
        return getResource(loginInfoResource, id);
    }

    /*
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public ProductResource(LoginInfoEntry loginInfoResource) {
        super(loginInfoResource);
    }

    @Override
    protected String getVersionForVanityUrl() {
        Collection<AppliesToFilterInfo> filters = null;
        String version = "";

        if (getType().equals(Type.ADDON)) {
            WlpInformation wlp = _asset.getWlpInformation();
            if (wlp != null) {
                filters = wlp.getAppliesToFilterInfo();
            }
        } else {
            filters = AppliesToProcessor.parseAppliesToHeader(getProductId() + "; productEdition="
                                                              + getProductEdition() +
                                                              "; productVersion=" + getProductVersion(), null);
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

    /*
     * This section is a temporary hack to put the product edition into the applies to Filter
     */
    @Override
    protected void copyFieldsFrom(MassiveResource fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        generateAppliesToFilterInfo();
    }

    public void generateAppliesToFilterInfo() {
        List<AppliesToFilterInfo> filter = AppliesToProcessor.parseAppliesToHeader(getProductId() + "; productEdition="
                                                                                   + getProductEdition() +
                                                                                   "; productVersion=" + getProductVersion(), null);
        _asset.getWlpInformation().setAppliesToFilterInfo(filter);
    }

    /*
     * End of Edition hack
     */
}
