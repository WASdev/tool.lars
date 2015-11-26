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
package com.ibm.ws.repository.resources;

import java.util.Collection;

/**
 * Represents a product-like (either a product or a tool) resource in the repository.
 * <p>
 * This interface allows read access to fields which are specific to product-like things.
 */
public interface ProductRelatedResource extends RepositoryResource {

    /**
     * Gets the productId for the resource.
     * 
     * @return the product id, or null if it has not been set
     */
    public String getProductId();

    /**
     * Gets the edition of the product, if applicable.
     * 
     * @return the edition of the product, or null if it is not set
     */
    public String getProductEdition();

    /**
     * Gets the install type of the product (e.g. "Archive")
     * 
     * @return the install type, or null if it is not set
     */
    public String getProductInstallType();

    /**
     * Gets the version of the product
     * 
     * @return the product version, or null if it is not set
     */
    public String getProductVersion();

    /**
     * Gets the features included in this product
     * 
     * @return the features provided by this product, or null if not set
     */
    public Collection<String> getProvideFeature();

    /**
     * Gets the features that this product depends on
     * 
     * @return the features required by this product, or null if not set
     */
    public Collection<String> getRequireFeature();

}
