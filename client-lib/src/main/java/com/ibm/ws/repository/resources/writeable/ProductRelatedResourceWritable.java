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
package com.ibm.ws.repository.resources.writeable;

import java.util.Collection;

import com.ibm.ws.repository.resources.ProductRelatedResource;

/**
 * Represents a product-like (either a product or a tool) resource in the repository.
 * <p>
 * This interface allows write access to fields which are specific to product-like things.
 */
public interface ProductRelatedResourceWritable extends ProductRelatedResource, RepositoryResourceWritable, WebDisplayable {

    /**
     * Sets the productId for the resource
     *
     * @param productId the product id
     */
    public void setProductId(String productId);

    /**
     * Sets the edition of the product
     *
     * @param edition the edition of the product
     */
    public void setProductEdition(String edition);

    /**
     * Sets the install type if the product (e.g. "Archive")
     *
     * @param productInstallType the install type
     */
    public void setProductInstallType(String productInstallType);

    /**
     * Sets the version of the product
     *
     * @param version the product version
     */
    public void setProductVersion(String version);

    /**
     * Sets the features included in this product
     *
     * @param provideFeature the symbolic names of features provided by this product
     */
    public void setProvideFeature(Collection<String> provideFeature);

    /**
     * Sets the features that this product depends on
     *
     * @param requireFeature the symbolic names of features required by this product
     */
    public void setRequireFeature(Collection<String> requireFeature);

    /**
     * Sets the collection of OSGi requirements this product has
     *
     * @param genericRequirements The OSGi requirements for this product
     */
    public void setGenericRequirements(String genericRequirements);

    /**
     * Sets the version information for the Java packaged with this product
     *
     * @param packagedJava The version information for the packaged Java
     */
    public void setPackagedJava(String packagedJava);

}