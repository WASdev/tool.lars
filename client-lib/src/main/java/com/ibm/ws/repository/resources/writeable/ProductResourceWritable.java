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

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.resources.ProductResource;

/**
 * Represents a Product Resource which can be uploaded to a repository.
 * <p>
 * This interface allows write access to fields which are specific to products.
 * <p>
 * Products represented by this interface can either be of type {@link ResourceType#INSTALL} or {@link ResourceType#ADDON}.
 */
public interface ProductResourceWritable extends ProductResource, ProductRelatedResourceWritable, ApplicableToProductWritable {

    /**
     * Sets the type of the product resource
     * <p>
     * Products can either be of type {@link ResourceType#INSTALL} or {@link ResourceType#ADDON}.
     * 
     * @param type the type of the product
     */
    public void setType(ResourceType type);

}
