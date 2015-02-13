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

import com.ibm.ws.massive.LoginInfoEntry;

public abstract class ProductRelatedResource extends MassiveResource implements WebDisplayable {

    public ProductRelatedResource(LoginInfoEntry loginInfoResource) {
        super(loginInfoResource);
    }

    public void setProductId(String productId) {
        _asset.getWlpInformation().setProductId(productId);
    }

    public String getProductId() {
        return _asset.getWlpInformation().getProductId();
    }

    public void setProductEdition(String edition) {
        _asset.getWlpInformation().setProductEdition(edition);
    }

    public String getProductEdition() {
        return _asset.getWlpInformation().getProductEdition();
    }

    public void setProductInstallType(String productInstallType) {
        _asset.getWlpInformation().setProductInstallType(productInstallType);
    }

    public String getProductInstallType() {
        return _asset.getWlpInformation().getProductInstallType();
    }

    public void setProductVersion(String version) {
        _asset.getWlpInformation().setProductVersion(version);
    }

    public String getProductVersion() {
        return _asset.getWlpInformation().getProductVersion();
    }

    /**
     * The {@link DisplayPolicy} to use
     */
    public void setDisplayPolicy(DisplayPolicy policy) {
        if (policy != null) {
            _asset.getWlpInformation().setDisplayPolicy(policy.getWlpDisplayPolicy());
        } else {
            _asset.getWlpInformation().setDisplayPolicy(null);
        }
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
        if (policy != null) {
            _asset.getWlpInformation().setWebDisplayPolicy(policy.getWlpDisplayPolicy());
        } else {
            _asset.getWlpInformation().setWebDisplayPolicy(null);
        }
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

    public void setProvideFeature(Collection<String> provideFeature) {
        _asset.getWlpInformation().setProvideFeature(provideFeature);
    }

    public Collection<String> getProvideFeature() {
        return _asset.getWlpInformation().getProvideFeature();
    }

    public void setRequireFeature(Collection<String> requireFeature) {
        _asset.getWlpInformation().setRequireFeature(requireFeature);
    }

    public Collection<String> getRequireFeature() {
        return _asset.getWlpInformation().getRequireFeature();
    }

    public void setAppliesTo(String appliesTo) {
        _asset.getWlpInformation().setAppliesTo(appliesTo);
    }

    public String getAppliesTo() {
        return _asset.getWlpInformation().getAppliesTo();
    }

    // Make setType public as this represents two different types in Massive
    @Override
    public void setType(Type type) {
        super.setType(type);
    }

    @Override
    protected void copyFieldsFrom(MassiveResource fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        ProductRelatedResource prodRes = (ProductRelatedResource) fromResource;
        setProductId(prodRes.getProductId());
        setProductEdition(prodRes.getProductEdition());
        setProductInstallType(prodRes.getProductInstallType());
        setProductVersion(prodRes.getProductVersion());
        setDisplayPolicy(prodRes.getDisplayPolicy());
        setWebDisplayPolicy(prodRes.getWebDisplayPolicy());
        setProvideFeature(prodRes.getProvideFeature());
        setRequireFeature(prodRes.getRequireFeature());
        setAppliesTo(prodRes.getAppliesTo());
        updateGeneratedFields();
    }
}
