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
package com.ibm.ws.repository.transport.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;

/**
 *
 */
public abstract class AbstractRepositoryClient implements RepositoryReadableClient {

    /**
     * This method will return all of the assets matching specific filters in Massive.
     * It will just return a summary of each asset and not include any {@link Attachment}s.
     * 
     * @param types
     *            The types to look for or <code>null</code> will return all types
     * @param productIds The product IDs to look for. Should not be <code>null</code> although supplying this will return assets for any product ID
     * @param visibility The visibility to look for or <code>null</code> will return all visibility values (or none)
     * @param productVersions The values of the minimum version in the appliesToFilterInfo to look for
     * @return A collection of the assets of that type
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public Collection<Asset> getAssets(final Collection<ResourceType> types, final Collection<String> productIds, final Visibility visibility, final Collection<String> productVersions) throws IOException, RequestFailureException {
        return getFilteredAssets(types, productIds, visibility, productVersions, false);
    }

    /**
     * This method will return all of the assets matching specific filters in Massive that do not have a maximum version in their applies to filter info.
     * It will just return a summary of each asset and not include any {@link Attachment}s.
     * 
     * @param types
     *            The types to look for or <code>null</code> will return all types
     * @param productIds The product IDs to look for. Should not be <code>null</code> although supplying this will return assets for any product ID
     * @param visibility The visibility to look for or <code>null</code> will return all visibility values (or none)
     * @return A collection of the assets of that type
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public Collection<Asset> getAssetsWithUnboundedMaxVersion(final Collection<ResourceType> types, final Collection<String> rightProductIds, final Visibility visibility) throws IOException, RequestFailureException {
        return getFilteredAssets(types, rightProductIds, visibility, null, true);
    }

    /**
     * This method will return all of the assets of a specific type in Massive.
     * It will just return a summary of each asset and not include any {@link Attachment}s.
     * 
     * @param type
     *            The type to look for, if <code>null</code> this method behaves
     *            in the same way as {@link #getAllAssets()}
     * @return A collection of the assets of that type
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public Collection<Asset> getAssets(final ResourceType type) throws IOException, RequestFailureException {
        Collection<ResourceType> types;
        if (type == null) {
            types = null;
        } else {
            types = Collections.singleton(type);
        }
        return getFilteredAssets(types, null, null, null, false);
    }

    /**
     * Implementation for the filtered get methods {@link #getAssets(Collection, String, Visibility, String)} and
     * {@link #getAssetsWithUnboundedMaxVersion(Collection, String, Visibility)}.
     * 
     * @param types
     *            The types to look for or <code>null</code> will return all types
     * @param productIds The product IDs to look for or <code>null</code> will return assets for any product ID
     * @param visibility The visibility to look for or <code>null</code> will return all visibility values (or none)
     * @param productVersions The value of the minimum version in the appliesToFilterInfo to look for
     * @param unboundedMaxVersion <code>true</code> if
     * @return
     * @throws IOException
     * @throws RequestFailureException
     */
    protected Collection<Asset> getFilteredAssets(Collection<ResourceType> types, Collection<String> productIds, Visibility visibility, Collection<String> productVersions,
                                                  boolean unboundedMaxVersion) throws IOException, RequestFailureException {
        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();
        if (types != null && !types.isEmpty()) {
            Collection<String> typeValues = new HashSet<String>();
            for (ResourceType type : types) {
                typeValues.add(type.getValue());
            }
            filters.put(FilterableAttribute.TYPE, typeValues);
        }
        filters.put(FilterableAttribute.PRODUCT_ID, productIds);
        if (visibility != null) {
            filters.put(FilterableAttribute.VISIBILITY, Collections.singleton(visibility.toString()));
        }
        filters.put(FilterableAttribute.PRODUCT_MIN_VERSION, productVersions);
    
        if (unboundedMaxVersion) {
            filters.put(FilterableAttribute.PRODUCT_HAS_MAX_VERSION, Collections.singleton(Boolean.FALSE.toString()));
        }
        return getFilteredAssets(filters);
    }

    /**
     * Returns <code>true</code> if all the filters are empty.
     * 
     * @param filters
     * @return
     */
    protected boolean allFiltersAreEmpty(Map<FilterableAttribute, Collection<String>> filters) {
        for (Map.Entry<FilterableAttribute, Collection<String>> filter : filters.entrySet()) {
            Collection<String> values = filter.getValue();
            if (values != null && !values.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private interface AppliesToFilterGetter {
        public String getValue(final AppliesToFilterInfo atfi);
    }

    protected Collection<String> getValues(final FilterableAttribute attrib, final Asset asset) {
        String ret;
        switch (attrib) {
            case LOWER_CASE_SHORT_NAME:
                ret = asset.getWlpInformation().getLowerCaseShortName();
                return (ret == null ? Collections.<String> emptyList() : Collections.singleton(ret));
            case PRODUCT_HAS_MAX_VERSION:
                return getFromAppliesTo(asset, new AppliesToFilterGetter() {
                    @Override
                    public String getValue(AppliesToFilterInfo atfi) {
                        return atfi.getHasMaxVersion();
                    }
                });
            case PRODUCT_ID:
                return getFromAppliesTo(asset, new AppliesToFilterGetter() {
                    @Override
                    public String getValue(AppliesToFilterInfo atfi) {
                        return atfi.getProductId() != null ? atfi.getProductId() : null;
                    }
                });
            case PRODUCT_MIN_VERSION:
                return getFromAppliesTo(asset, new AppliesToFilterGetter() {
                    @Override
                    public String getValue(AppliesToFilterInfo atfi) {
                        return atfi.getMinVersion() != null ? atfi.getMinVersion().getValue() : null;
                    }
                });
            case SHORT_NAME:
                ret = asset.getWlpInformation().getShortName();
                return (ret == null ? Collections.<String> emptyList() : Collections.singleton(ret));
            case SYMBOLIC_NAME:
                return asset.getWlpInformation().getProvideFeature() == null ? Collections.<String> emptyList() : asset.getWlpInformation().getProvideFeature();
            case TYPE:
                ret = asset.getType() == null ? null : asset.getType().getValue();
                return (ret == null ? Collections.<String> emptyList() : Collections.singleton(ret));
            case VISIBILITY:
                ret = asset.getWlpInformation().getVisibility() == null ? null : asset.getWlpInformation().getVisibility().toString();
                return (ret == null ? Collections.<String> emptyList() : Collections.singleton(ret));
            default:
                return null;

        }
    }

    /**
     * Utility method to cycle through the applies to filters info and collate the values found
     * 
     * @param asset
     * @param getter
     * @return
     */
    private static Collection<String> getFromAppliesTo(final Asset asset, final AppliesToFilterGetter getter) {
        Collection<AppliesToFilterInfo> atfis = asset.getWlpInformation().getAppliesToFilterInfo();
        Collection<String> ret = new ArrayList<String>();
        if (atfis != null) {
            for (AppliesToFilterInfo at : atfis) {
                if (at != null) {
                    String val = getter.getValue(at);
                    if (val != null) {
                        ret.add(val);
                    }
                }
            }
        }
        return ret;
    }
}
