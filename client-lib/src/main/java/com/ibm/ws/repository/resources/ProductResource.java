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

import com.ibm.ws.repository.common.enums.ResourceType;

/**
 * Represents a Product Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to products.
 * <p>
 * Products represented by this interface can either be of type {@link ResourceType#INSTALL} or {@link ResourceType#ADDON}.
 */
public interface ProductResource extends ProductRelatedResource {

    /**
     * Gets the appliesTo field associated with the resource
     * 
     * @return The appliesTo field associated with the resource, or null if it has not been set
     */
    public String getAppliesTo();

}
